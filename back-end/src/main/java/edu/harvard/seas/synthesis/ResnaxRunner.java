package edu.harvard.seas.synthesis;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;

public class ResnaxRunner {
	public static String resnax_path = "lib";
	public static int timeout = 60; // 60 seconds
	
	private String java_class_path;
	private String z3_lib_path;

	private String example_file_path = "input";
	private String program_file_path = "program";
	private String log_dir_path = "resnax_log" + File.separator;
	private String temp_dir_path = "resnax_temp" + File.separator;

	private static ResnaxRunner single_instance = null;
	
	public HashMap<String, String> dsl_to_automaton_regex = new HashMap<String, String>();
	public Process process = null;
	public int counter = 0;
	
	private ResnaxRunner() {
		if(!log_dir_path.endsWith(File.separator)) {
			log_dir_path += File.separator;
		}
		
		if(!temp_dir_path.endsWith(File.separator)) {
			temp_dir_path += File.separator;
		}
		
		File log_dir = new File(log_dir_path);
		if(log_dir.exists()) {
			log_dir.delete();
		}
		log_dir.mkdir();
		
		File temp_dir = new File(temp_dir_path);
		if(temp_dir.exists()) {
			temp_dir.delete();
		}
		temp_dir.mkdir();
		
		// By default
		z3_lib_path = resnax_path;

		String os = System.getProperty("os.name").toLowerCase();
		String jvmBitVersion = System.getProperty("sun.arch.data.model");
		if(os.indexOf("win") >= 0) {
			if(jvmBitVersion.equals("32")) {
				z3_lib_path = resnax_path + File.separator + "win32";
			}
			else if(jvmBitVersion.equals("64")) {
				z3_lib_path = resnax_path + File.separator + "win64";
			}
		}
		
		// enumerate all jar files in the classpath
		java_class_path = resnax_path + File.separator + "resnax.jar"
		 + File.pathSeparator + resnax_path + File.separator + "antlr-4.7.1-complete.jar"
		 + File.pathSeparator + resnax_path + File.separator + "automaton.jar"
		 + File.pathSeparator + resnax_path + File.separator + "com.microsoft.z3.jar"
		 + File.pathSeparator + resnax_path + File.separator + "javatuples-1.2.jar"
		 + File.pathSeparator + resnax_path + File.separator + "libz3java.dylib"
		 + File.pathSeparator + resnax_path + File.separator + "libz3.dylib"
		 + File.pathSeparator + resnax_path + File.separator + "libz3java.so";
	}
	
	public static ResnaxRunner getInstance() {
		if(single_instance == null) {
			single_instance = new ResnaxRunner();
		} 
		
		return single_instance;
	}
	
	public static void reset() {
		if(single_instance == null) {
			return;
		}
		
		// kill the current synthesis process
		if(single_instance.process != null && single_instance.process.isAlive()) {
			single_instance.process.destroy();
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(single_instance.process.isAlive()) {
				single_instance.process.destroyForcibly();
			}
		}
		// remove the two files
		File f1 = new File(single_instance.example_file_path);
		f1.delete();
		File f2 = new File(single_instance.program_file_path);
		f2.delete();
		
		// remove log files if any
		File fError = new File("resnax-error");
		if(fError.exists()) {
			fError.delete();
		}
        File fOutput = new File("resnax-output");
		if(fOutput.exists()) {
			fOutput.delete();
		}
		
		single_instance = null;
	}
	
	private String prev_sketch = "";
	private String prev_excludes = "";
	private HashSet<String> prev_examples = new HashSet<String>();
	public List<String> run(Example[] examples, Regex[] regexes) {
		// reset the previous mapping between DSL regexes and automaton regexes
		dsl_to_automaton_regex.clear();
		
		// write the input examples to the example file
		File f = new File(example_file_path);
		String s = "";
		HashSet<String> example_set = new HashSet<String>();
		for(Example example : examples) {
			s += "\"" + example.input + "\"," + (example.output ? "+" : "-") + System.lineSeparator();
			example_set.add(example.input + "," + example.output);
		}
		s+= System.lineSeparator();
		
		// use a random ground truth, it does not matter, just a requirement of the synthesizer
		s += "repeatatleast(or(<A>,or(<B>,<C>)),1)";
		s += System.lineSeparator();
		
		// parse the annotations to sketches
		String sketch = parseAnnotationToSketch(examples, regexes);
		
		HashSet<String> exclude_set = new HashSet<String>();
		for(Regex regex : regexes) {
			if(regex.exclude.length == 0) continue;
			
			for(String exclude : regex.exclude) {
				exclude_set.add(exclude);
			}
		}
		
		String excludes = "";
		for(String e : exclude_set) {
			excludes += e + "&&";
		}
		
		if(!excludes.isEmpty()) {
			excludes = excludes.substring(0, excludes.length() - 2);
		} else {
			excludes = ",";
		}
		
		boolean restart;
		HashSet<String> copy = new HashSet<String>(prev_examples);
		copy.removeAll(example_set);
		if(process == null) {
			// this is the first iteration
			counter = 0;
			restart = true;
		} else if(!sketch.equals(prev_sketch) || !excludes.equals(prev_excludes) || !copy.isEmpty()) {
			// user intent may have changed, redo the synthesis from scratch
			counter = 0;
			restart = true;
		} else if(process != null && !process.isAlive()) {
			// the synthesis process has crashed due to error such as out of memory
			// have to restart
			counter = 0;
			restart = true;
		} else {
			restart = false;
		}
		
		// set the signal 
		s += "READY-" + counter;
		
		try {
			// write the examples to the example file
			FileUtils.write(f, s, Charset.defaultCharset(), false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			invokeResnax(sketch, excludes, restart);
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		// get the new synthesized programs
		ArrayList<String> new_regexes = new ArrayList<String>();
		try {
			File log_file = new File(program_file_path);
			if(log_file.exists()) {
				List<String> lines = FileUtils.readLines(log_file, Charset.defaultCharset());
				for(int i = 0; i < lines.size()-1; i+=2) {
					String curr_dsl_regex = lines.get(i).trim();
					String curr_automaton_regex = lines.get(i+1).trim();
					if(!new_regexes.contains(curr_dsl_regex)) {
						// avoid duplication
						new_regexes.add(curr_dsl_regex);
						dsl_to_automaton_regex.put(curr_dsl_regex, curr_automaton_regex);
					}
				}
				
				if(new_regexes.isEmpty()) {
					System.err.println("Synthesis timeout. No program is generated.");
				}
				
				log_file.delete();
			} else {
				System.err.println("No resnax log file exists. The synthesizer crashed.");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
    	prev_sketch = sketch;
    	prev_excludes = excludes;
    	prev_examples = example_set;
    	counter++;
		
		return new_regexes;
	}
	
	public void invokeResnax(String sketch, String excludes, boolean restart) throws IOException, InterruptedException {
        File fError = new File("resnax-error");
        File fOutput = new File("resnax-output");
		if(restart) {
			// check if the process is still alive, if it is , then kill it.
			if(process != null && process.isAlive()) {
				process.destroy();
				Thread.sleep(2000);
				if(process.isAlive()) {
					process.destroyForcibly();
				}
			}
			
			int maxMem;
			try {
				// This is specific to Oracle JVM
				long memorySize = ((com.sun.management.OperatingSystemMXBean) ManagementFactory
				        .getOperatingSystemMXBean()).getTotalPhysicalMemorySize();
				maxMem = (int) (memorySize / (1024 * 1024 * 1024));
			} catch(Exception e) {
				// catch any exceptions that arise in other JVMs
				// if any exception occurs, make a conversative choice of only allocating a max of 8G memory
				maxMem = 8;
			}
			
			String jvmBitVersion = System.getProperty("sun.arch.data.model");
			if(jvmBitVersion.equals("32")) {
				// If the JVM is 32-bit, we can only allocate a max of 4G memory.
				maxMem = 4;
				
				// If it is a Windows system, be more conservative and only allocate 1G memory
				String os = System.getProperty("os.name").toLowerCase();
				if(os.indexOf("win") >= 0) {
					maxMem = 1;
				}
			}

			// invoke resnax in a separate thread and set a timeout
			String[] cmd = {"java", "-Xmx" + maxMem + "G", "-Djava.library.path=" + z3_lib_path,
					"-cp", java_class_path, "-ea", "MRGA.Main", 
					"0", // dataset : 0 - so, 1 - deepregex, 2 - kb13
					example_file_path, // file path to the input-output examples
					log_dir_path, // path to the log directory
					sketch,
					"1", 
					"2", // mode : 1 - normal, 2 - prune, 3 - forgot, 4 - pure-enumeration, 5 - example-only
					"0", // extended mode, what is this?
					temp_dir_path,
					"5", 
					excludes,
					example_file_path,
					program_file_path,
					timeout * 1000 + "",
					",",
					",",
					","};
	        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
	        processBuilder.redirectError(fError);
	        processBuilder.redirectOutput(fOutput);
	        process = processBuilder.start();
		} 
		
        while(true) {
        	// wait till the signal is there
        	File f = new File(program_file_path);
        	if(f.exists()) {
        		String data = FileUtils.readFileToString(f, Charset.defaultCharset());
	            if (data.contains("READY-" + counter)) {
	            	break;
	            }
        	} else if (fError.exists()) {
        		String errorMessage = FileUtils.readFileToString(fError, Charset.defaultCharset());
        		if(!errorMessage.trim().isEmpty()) {
        			// an error occurs 
        			if(fOutput.exists()) {
        	        	System.out.println(FileUtils.readFileToString(fOutput, Charset.defaultCharset()));
        	            fOutput.delete();            
        	        }
        			System.out.println("Error occurs during program synthesis");
        			System.out.println(errorMessage);
        			fError.delete();
            		return;
        		}
        	}
        	
        	
        	// sleep 5 seconds
          	Thread.sleep(5000);
        }
        
        if(fError.exists()) {
        	System.out.println(FileUtils.readFileToString(fError, Charset.defaultCharset()));            
        	fError.delete();
        }
        if(fOutput.exists()) {
        	System.out.println(FileUtils.readFileToString(fOutput, Charset.defaultCharset()));
            fOutput.delete();            
        }
	}
	
	public String parseAnnotationToSketch(Example[] examples, Regex[] regexes) {
		HashSet<String> exact_matches = new HashSet<String>();
		HashSet<String> not_matches = new HashSet<String>();
		HashSet<String> char_families = new HashSet<String>();
		HashSet<String> includes = new HashSet<String>();
		
		for(Example example : examples) {
			if(example.output) {
				// only consider exact match in positive examples
				for(String s : example.exact) {
					exact_matches.add(s);
				}
			}
			
			if(!example.output) {
				// only consider unmatch in negative examples
				for(String s : example.unmatch) {
					not_matches.add(s);
				}
			}
			
			for(String s : example.generalize) {
				String char_family = s.substring(s.lastIndexOf("@@@") + 3);
				if(char_family.equals("any")) {
					// handle it outside
					continue;
				}
				
				char_families.add('<' + char_family + '>');
				
				// comment out this heuristic, seems not so helpful				
//				if(!example.output) {
//					char_families.add("not(contain(<" + char_family + ">))");
//				}
			}
		}
		
		for(Regex regex : regexes) {
			for(String s : regex.include) {
				includes.add(s);
			}
		}
		
		for(Regex regex : regexes) {
			for(String s : regex.include) {
				includes.add(s);
			}
		}
		
		String sketch = "?";
		String sketch_includes = "";
		HashSet<String> single_chars = new HashSet<String>();
		HashSet<String> sequences = new HashSet<String>();
		
		for(String match : exact_matches) {
			if(match.length() == 1) {
				// a single character
				single_chars.add("<" + match + ">");
			} else {
				if(match.equals("--")) {
					// this is a trick
					// handle -- as a sequence
					sequences.add("concat(<->,<->)");
					continue;
				}
				char[] chars = match.toCharArray();
				// Option 1: treat multiple characters as a sequence
				String s = "";
				for(int i = 0; i < chars.length - 1; i++) {
					s += "concat(<" + chars[i] + ">,";
				}
				s+= "<" + chars[chars.length - 1] + ">";
				for(int i = 0; i < chars.length - 1; i++) {
					s+= ")";
				}
				sequences.add(s);
				// Option 2: treat multiple characters separately, not as a sequence
				for(char c : chars) {
					single_chars.add("<" + c + ">");
				}
			}
		}
		
		for(String unmatch : not_matches) {
			if(unmatch.length() == 1) {
				// a single character
				single_chars.add("<" + unmatch + ">");
			} else {
				char[] chars = unmatch.toCharArray();
				// Option 1:  treat multiple characters as a sequence
				String s = "";
				for(int i = 0; i < chars.length - 1; i++) {
					s += "concat(<" + chars[i] + ">,";
				}
				s+= "<" + chars[chars.length - 1] + ">";
				for(int i = 0; i < chars.length - 1; i++) {
					s+= ")";
				}
				sequences.add(s);
				
				// Option 2: treat multiple characters separately, not as a sequence
				for(char c : chars) {
					single_chars.add("<" + c + ">");
				}
			}
		}
		
		for(String char_family : char_families) {
			sequences.add(char_family);
		}
		
		for(String include : includes) {
			if(include.equals("repeatatleast")) {
				include = "repeatatleast(?,-1)";
			} else if (include.equals("contain")) {
				include = "contain(?)";
			} else if (include.equals("or")) {
				include = "or(?,?)";
			} else if (include.equals("startwith")) {
				include = "startwith(?)";
			} else if (include.equals("endwith")) {
				include = "endwith(?)";
			} else if (include.equals("optional")) {
				include = "optional(?)";
			} else if (include.equals("star")) {
				include = "star(?)";
			} else if (include.equals("kleenestar")) {
				include = "kleenestar(?)";
			} else if (include.equals("repeat")) {
				include = "repeat(?,-1)";
				continue;
			} else if (include.equals("repeatrange")) {
				include = "repeatrange(?,-1,-1)";
			} else if (include.equals("concat")) {
				include = "concat(?,?)";
			} else if (include.equals("not")) {
				include = "not(?)";
			} else if (include.equals("notcc")) {
				include = "notcc(?)";
			} else if (include.equals("and")) {
				include = "and(?,?)";
			} else if (include.equals("or")) {
				include = "or(?,?)";
			} else if (include.equals("sep")) {
				include = "sep(?,?)";
			}
			
			sketch_includes += include + ",";
		}
		
		ArrayList<String> l = new ArrayList<String>(single_chars);
		if(l.size() > 1) {
			sketch += "{";
			// add a disjunction of all single chars
			for(int i = 0; i < l.size() - 1; i++) {
				sketch += "or(" + l.get(i)+ ",";
			}
			sketch += l.get(l.size() - 1) ;
			for(int i = 0; i < l.size() - 1; i++) {
				sketch += ")";
			}
			
			// then add individual chars
			for(int i = 0; i < l.size(); i++) {
				sketch += "," + l.get(i);
			}
		} else if (l.size() == 1) {
			sketch += "{" + l.get(0);
		}
		
		if(sequences.size() > 0) {
			if(sketch.contains("{")) {
				sketch += ",";
			} else {
				sketch += "{";
			}
			
			for(String sequence : sequences) {
				sketch += sequence + ",";
			}
			
			if(sketch.endsWith(",")) {
				sketch = sketch.substring(0, sketch.length() - 1);
			}
		}
		
		if(!sketch_includes.isEmpty()) {
			if(sketch.contains("{")) {
				sketch += "," + sketch_includes;
			} else {
				sketch += "{" + sketch_includes;
			}
		}
		
		if(sketch.contains("{")) {
			if(sketch.endsWith(",")) {
				sketch = sketch.substring(0, sketch.length() - 1);
			}
			sketch += "}";
		}
		
		return sketch;
	}
}

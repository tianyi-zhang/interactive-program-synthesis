package edu.harvard.seas.synthesis.sample;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

public abstract class RegexSampler {
	HashMap<String, String> regex_map;
	HashMap<String, ArrayList<String>> match_map;
	HashMap<String, ArrayList<String>> unmatch_map;
	HashMap<String, String> example_map;
	
	File log_file;
	
	public RegexSampler(String logFile) {
		regex_map = new HashMap<String, String>();
		match_map = new HashMap<String, ArrayList<String>>();
		unmatch_map = new HashMap<String, ArrayList<String>>();
		example_map = new HashMap<String, String>();
		log_file = new File(logFile);
	}
	
	public void processLogFile() {
		LineIterator it = null;
		try {
			it = FileUtils.lineIterator(log_file, "UTF-8");
			String cur_id = "";
			while(it.hasNext()) {
				String line = it.nextLine();
				if(!line.startsWith(" ") && line.contains(":")) {
					String id = line.split(":")[0];
					String regex = line.split(":")[1].trim();
					regex_map.put(id, regex);
					
					cur_id = id;
				} else if (line.startsWith(" matching indices:")) {
					String line2 = it.next();
					String indices = line.split(":")[1].trim();
					String examples = line2.split(":")[1].trim();
					String[] arr1 = indices.split(" ");
					String[] arr2 = examples.split(";");
					// print the number of satisfied examples for each regex
					System.out.println(arr1.length);
					
					for(int i = 0; i < arr1.length; i++) {
						String index = arr1[i];
						String example = arr2[i];
						if(!example_map.containsKey(index)) {
							example_map.put(index, example);
						}
					}
					
					ArrayList<String> matches = new ArrayList<String>();
					for(String match : arr1) {
						matches.add(match);
					}
					
					match_map.put(cur_id, matches);
				} else if (line.startsWith(" not matching indices:")) {
					String line2 = it.next();
					String indices = line.split(":")[1].trim();
					String examples = line2.split(":")[1].trim();
					String[] arr1 = indices.split(" ");
					String[] arr2 = examples.split(";");
					for(int i = 0; i < arr1.length; i++) {
						String index = arr1[i];
						String example = arr2[i];
						if(!example_map.containsKey(index)) {
							example_map.put(index, example);
						}
					}
					
					ArrayList<String> unmatches = new ArrayList<String>();
					for(String unmatch : arr1) {
						unmatches.add(unmatch);
					}
					
					unmatch_map.put(cur_id, unmatches);
				}
			}
			
			// print the indices of input-output examples
//			for(String eid : example_map.keySet()) {
//				System.out.println(eid + " - " + example_map.get(eid));
//			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if(it != null) {
				LineIterator.closeQuietly(it);
			}
		}
	}
	
	public abstract void sample();
	
	public Set<String> solve(String matrix_file, String cost_file) throws IOException, InterruptedException {
		HashSet<String> sample = new HashSet<String>(); // a sample of columns in the matrix
		
		File f1 = new File(matrix_file);
		File f2 = new File(cost_file);
		
		// run the python set cover solver
		File fError = new File("set-cover-error");
        File fOutput = new File("set-cover-output");
        
		String[] cmd = {"/usr/local/bin/python", "setcover/setcover.py", f1.getAbsolutePath(), f2.getAbsolutePath()};
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        processBuilder.redirectError(fError);
        processBuilder.redirectOutput(fOutput);
        Process process = processBuilder.start();
        process.waitFor();
        
        String error_output = FileUtils.readFileToString(fError, Charset.defaultCharset());
        if(!error_output.isEmpty()) {
        	System.err.println("Set cover solver throws an error.");
        	System.err.println(error_output);
        }
        List<String> lines = FileUtils.readLines(fOutput, Charset.defaultCharset());
        if(!lines.isEmpty()) {
        	for(String line : lines) {
        		if(line.startsWith("Selected Program:")) {
        			String matrix_col = line.substring(line.indexOf(':')+1).trim();
        			// add the column index to the sample set
        			sample.add(matrix_col);
        		}
        	}
        }
        
        fError.delete();
        fOutput.delete();
        
        return sample;
	}
}

package edu.harvard.seas.synthesis;

import java.io.File;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class ServerCommandLineParser {
	private final static String APPLICATION_NAME = "java -jar ips-backend.jar";
	
	// config for program synthesis
	public int timeout; // Optional. 60 seconds by default.
//	public String z3_lib_path; // Required. 
	public String resnax_path; // Required.
		
		
	// config for input example geneartion
//	public String python3_path; // Optional.
//	public String input_generator_path; // Required.
	public int num_of_examples_per_cluster; // Optional. 5 by default.
	
	public boolean parse(String[] args) {
		CommandLineParser parser = new DefaultParser();
		CommandLine commandLine = null;
		try{
			commandLine = parser.parse(ServerCommandLineParser.getOptions(), args);
		} catch (ParseException e){
			if(!(args.length == 1 && (args[0].equals("-h") || args[0].equals("--helper")))){
				System.out.println("Could not parse arguments" + System.lineSeparator()
					+ "Exception information:" + System.lineSeparator() + e.getLocalizedMessage());
			}
			
			printHelp(commandLine);
			return false;
		}
		
		assert(commandLine != null);
		
//		this.z3_lib_path = commandLine.getOptionValue('z');
		this.resnax_path = commandLine.getOptionValue('s').trim();
		File f = new File(resnax_path);
		if(f.exists()) {
			this.resnax_path = f.getAbsolutePath();
		} else {
			System.out.println("The path to the program synthesizer's libraries, " + this.resnax_path + 
					", is not valid. The directory does not exist.");
			return false;
		}
		
//		this.input_generator_path = commandLine.getOptionValue('g').trim();
//		f = new File(input_generator_path);
//		if(f.exists()) {
//			this.input_generator_path = f.getAbsolutePath();
//		} else {
//			System.out.println("The path to the input generator's script, " + this.input_generator_path + 
//					", is not valid. The script does not exist.");
//			return false;
//		}
		
		if(commandLine.hasOption('t')) {
			try {
				this.timeout = Integer.parseInt(commandLine.getOptionValue('t').trim());
			} catch(NumberFormatException e) {
				System.out.println("Please make sure the value of the timeout option is a valid integer.");
				return false;
			}
			
			if(this.timeout <= 0) {
				System.out.println("Please make sure the value of the timeout option is a positive number.");
				return false;
			}
		} else {
			this.timeout = 60;
		}
		
//		if(commandLine.hasOption('p')) {
//			this.python3_path = commandLine.getOptionValue('p').trim();
//			f = new File(python3_path);
//			if(f.exists()) {
//				this.python3_path = f.getAbsolutePath();
//			} else {
//				System.out.println("The path to the python command, " + this.python3_path + ", is not valid. "
//						+ "The python file does not exist.");
//				return false;
//			}
//		} else {
//			this.python3_path = "python";
//		}
		
		// check python version
//		String[] cmd = {python3_path, "--version"};
//		ProcessBuilder processBuilder = new ProcessBuilder(cmd);
//		File fError = new File("tmp-error");
//        processBuilder.redirectError(fError);
//        File fOutput = new File("tmp-output");
//        processBuilder.redirectOutput(fOutput);
//        try {
//	        Process process = processBuilder.start();
//	        process.waitFor();
//	        
//	        String output = FileUtils.readFileToString(fOutput, Charset.defaultCharset());
//	        String error = FileUtils.readFileToString(fError, Charset.defaultCharset());
//	        
//	        if(!error.isEmpty() || !output.contains("Python 3.")) {
//	        	System.out.println("Please make sure you have installed Python 3 "
//	        			+ "or have provided the correct path to the Python 3 command using the p option.");
//	        	return false;
//	        }
//        } catch (IOException e) {
//			e.printStackTrace();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		} finally {
//			fError.delete();
//			fOutput.delete();
//		}
        
        if(commandLine.hasOption('n')) {
        	try {
				this.num_of_examples_per_cluster = Integer.parseInt(commandLine.getOptionValue('n'));
			} catch(NumberFormatException e) {
				System.out.println("Please make sure the value of the example number option is a valid integer.");
				return false;
			}
			
			if(this.timeout <= 0) {
				System.out.println("Please make sure the value of the example number option is a positive number.");
				return false;
			}
        } else {
        	this.num_of_examples_per_cluster = 5;
        }
        
		return true;
	}
	
	private static void printHelp(CommandLine commandLine){
		HelpFormatter helpFormatter = new HelpFormatter();
		String header = "";
		String footer = "";

		helpFormatter.printHelp(APPLICATION_NAME, header, getOptions(),footer, true);
		System.out.println();
	}
	
	private static Options getOptions(){
//		Option z3PathOption = Option.builder("z")
//			.desc("Specify the path for Z3")
//			.longOpt("z3")
//			.hasArg()
//			.required(true)
//			.optionalArg(false)
//			.build();
		
		Option helper = Option.builder("h")
				.desc("Print the help information.")
				.longOpt("help")
				.hasArg(false)
				.required(false)
				.build();
		
		// I have reimplemented London's python code in Java.
		// We do not need to run python any more.
//		Option pythonPathOption = Option.builder("p")
//				.desc("Specify which python to run. This option should "
//						+ "be set if you have multiple python versions in your OS. "
//						+ "Current we only support python 3. If not specified, we will "
//						+ "use the default python in your OS.")
//				.longOpt("python")
//				.hasArg()
//				.required(false)
//				.build();

//		Option inputGeneratorOption = Option.builder("g")
//			.desc("Specify the path to the input example generator.")
//			.longOpt("input-generator")
//			.hasArg()
//			.required(true)
//			.optionalArg(false)
//			.build();

		Option numPerClusterOption = Option.builder("n")
			.desc("Specify the number of input examples generated per cluster per example seed. The default value is 5.")
			.longOpt("example-num")
			.hasArg()
			.required(false)
			.build();
		
		Option resnaxLibPathOption = Option.builder("s")
				.desc("Specify the path for the program synthesis libraries")
				.longOpt("synthesizer")
				.hasArg()
				.required(true)
				.optionalArg(false)
				.build();

		Option timeoutOption = Option.builder("t")
			.desc("Specify the timeout for the synthesis. 60 seconds if not specified.")
			.longOpt("timeout")
			.hasArg()
			.required(false)
			.build();
		
		Options toReturn = new Options();
		toReturn.addOption(resnaxLibPathOption);
		toReturn.addOption(timeoutOption);
//		toReturn.addOption(z3PathOption);
//		toReturn.addOption(pythonPathOption);
//		toReturn.addOption(inputGeneratorOption);
		toReturn.addOption(numPerClusterOption);
		toReturn.addOption(helper);

		return toReturn;
	}
}

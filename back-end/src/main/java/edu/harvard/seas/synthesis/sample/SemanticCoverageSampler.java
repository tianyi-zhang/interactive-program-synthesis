package edu.harvard.seas.synthesis.sample;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;

public class SemanticCoverageSampler extends RegexSampler {
	
	public SemanticCoverageSampler(String logFile) {
		super(logFile);
	}

	@Override
	public void sample() {
		// construct the matrix
		// row -- the input-output examples
		// column -- the regexes
		// matrix[i][j] = true if the j-th regex matches the i-th example, false otherwise
		boolean[][] matrix = new boolean[example_map.size()][regex_map.size()];
		int n = 0;
		ArrayList<String> regex_list = new ArrayList<String>();
		for(String id : regex_map.keySet()) {
			// store the regexes in order for ease of future look-up
			regex_list.add(id);
			
			ArrayList<String> matches = match_map.get(id);
			for(int i = 0; i < example_map.size(); i++) {
				if(matches.contains((i+1) + "")) {
					matrix[i][n] = true;
				} else {
					matrix[i][n] = false;
				}
			}
			n++;
		}
		
		// check if there is an example that is never satisfied
		HashSet<String> unsatisfied = new HashSet<String>();
		for(int i = 0; i < matrix.length; i++) {
			boolean b = false;
			for(int j = 0; j < matrix[i].length; j++) {
				b = b | matrix[i][j];
			}
			
			if(!b) {
				System.out.println("The " + (i+1) + "-th example is never satisfied.");
				unsatisfied.add((i+1) + "");
			}
		}
		
		// print the matrix to a csv file
		String path1 = log_file.getParent() + File.separator + log_file.getName() + "-matrix.csv";
		File f1 = new File(path1);
		if(f1.exists()) f1.delete();
		for(int i = 0; i < matrix.length; i++) {
			if(unsatisfied.contains("" + (i+1))) {
				// do not print the match result of an example if it is never satisfied
				// otherwise the set cover solver will never find a solution
				continue;
			}
			String s = "";
			for(int j = 0; j < matrix[i].length; j++) {
				s += matrix[i][j] + ",";
			}
			s = s.substring(0, s.length() - 1);
			if(i != matrix.length - 1) {
				s += System.lineSeparator();
			}
			try {
				FileUtils.write(f1, s, Charset.defaultCharset(), true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// print the cost to a csv file
		// set the cost of all regexes to 1
		String path2 = log_file.getParent() + File.separator + log_file.getName() + "-cost.csv";
		File f2 = new File(path2);
		if(f2.exists()) f2.delete();
		String costs = "";
		for(int i = 0; i < regex_map.size(); i++) {
			costs += "1,";
		}
		costs = costs.substring(0, costs.length() - 1);
		try {
			FileUtils.write(f2, costs, Charset.defaultCharset(), false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// run the set cover solver
		try {
			Set<String> sample = solve(path1, path2);
        	System.out.println("Selected Programs:");
			for(String matrix_col : sample) {
				String regex_id = regex_list.get(Integer.parseInt(matrix_col));
				String regex = regex_map.get(regex_id);
				System.out.println(regex);
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}		
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
		// switch between tasks
		int k = 1;
		
		String log = "/Users/tz/Research/Whitebox Synthesis/logs/exemples" + k + ".txt";
		SemanticCoverageSampler sampler = new SemanticCoverageSampler(log);
		sampler.processLogFile();
		sampler.sample();
	}
}

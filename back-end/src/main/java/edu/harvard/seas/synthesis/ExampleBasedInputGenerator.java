package edu.harvard.seas.synthesis;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;

public class ExampleBasedInputGenerator {
	public static String python3_path = "/usr/local/bin/python"; 
	public static int num_of_examples_per_cluster = 5;
	public static String input_generator_path = "example-generation/main.py";

	private String dfa_file_path = "dfa.txt";
	
	public Map<String, Map<String, Boolean>> generate(String example, String automaton_regex, String dsl_regex) {
		Map<String, Map<String, Boolean>> clusters = new LinkedHashMap<String, Map<String, Boolean>>();
		
		if(example.isEmpty()) {
			return clusters;
		}
		
		// parse the regex to an automaton
		RegExp exp = new RegExp(automaton_regex);
	    Automaton a = exp.toAutomaton();
		if(!a.isDeterministic()) {
			a.determinize();
			a.minimize();
		}
		
		// combining overlapping and adjacent edge intervals with same destination. 
		a.reduce();
		
		State p = a.getInitialState();
		for (int i = 0; i < example.length(); i++) {
			State q = p.step(example.charAt(i));
			
			// mutate this i-th character to generate positive example
			HashSet<String> positives = new HashSet<String>();
			HashSet<String> negatives = new HashSet<String>();
			ArrayList<Character> chars = new ArrayList<Character>();
			// find out what char ranges are not accepted by this state
			char c = '\u0020';
			while(c <= '\u007E') {
				chars.add(c);
				c = (char) (c + 1);
			}
			for(Transition t : p.getTransitions()) {
				if(t.getDest().equals(q)) {
					for(int j = 0; j < num_of_examples_per_cluster; j++) {
						String pos = mutateChar(example, i, t, chars);
						positives.add(pos);
					}
				} else {
					// another branch, may or may not be accepted after mutation
					for(int j = 0; j < num_of_examples_per_cluster; j++) {
						String s = mutateChar(example, i, t, chars);
						if(a.run(s)) {
							positives.add(s);
						} else {
							negatives.add(s);
						}
					}
				}
			}
			
			// mutate to generate negative examples
			if(!chars.isEmpty()) {
				for(int j = 0; j < num_of_examples_per_cluster; j++) {
					int randomNum = ThreadLocalRandom.current().nextInt(0, chars.size());
					char mut = chars.get(randomNum);
					String s;
					if(i == example.length() - 1) {
						s = example.substring(0, i) + mut;
					} else {
						s = example.substring(0, i) + mut + example.substring(i+1);
					}
					negatives.add(s);
				}
			}
			
			
			Map<String, Boolean> cluster = new HashMap<String, Boolean>();
        	for(String positive : positives) {
        		cluster.put(positive, true);
        	}
        	
        	if(negatives.isEmpty() && !positives.isEmpty()) {
        		// we didn't find any negative examples
        		String s = "Positive examples only";
        		clusters.put(s, cluster);
        	} else {
        		for(String negative: negatives) {
        			// If this is a negative example
					// get the index of the failure-inducing character 
					// and append it to the end
					int index = getIndexOfFailure(a, negative);
            		cluster.put(negative + "," + index, false);
            		String explanation = 
            				ExplanationGenerator.generateExplanation(negative, a, dsl_regex);
            		if(clusters.containsKey(explanation)) {
            			Map<String, Boolean> existing_cluster = clusters.get(explanation);
            			existing_cluster.putAll(cluster);
            			clusters.put(explanation, existing_cluster);
            		} else {
            			Map<String, Boolean> new_cluster = new HashMap<String, Boolean>();
            			new_cluster.putAll(cluster);
            			clusters.put(explanation, new_cluster);
            		}
            	}
        	}
		}
		
		if(clusters.size() > 1 && clusters.containsKey("Positive examples only")) {
			clusters.remove("Positive examples only");
		}
		
		return clusters;
	}
	
	private int getIndexOfFailure(Automaton a, String s) {
		State p = a.getInitialState();
		int index = 0;
		for (int i = 0; i < s.length(); i++) {
			State q = p.step(s.charAt(i));
			if (q == null) {
				break;
			} else if (i == s.length() - 1 && !q.isAccept()) {
				break;
			}
			
			index = i + 1;
			p = q;
		}
		
		return index;
	}
	
	private String getRandomItem(HashSet<String> set){
	    Random random = new Random();
	    int randomNumber = random.nextInt(set.size());
	    
	    int currentIndex = 0;
	    String randomElement = null;
	    
	    for(String element : set){
	        randomElement = element;
	        
	        if(currentIndex == randomNumber)
	            return randomElement;
	        
	        currentIndex++;
	    }
	    
	    return randomElement;
	}
		
	private String mutateChar(String example, int index, Transition t, ArrayList<Character> chars) {
		char min = t.getMin();
		if(min < '\u0020') {
			min = '\u0020';
		}
		char max = t.getMax();
		if(max > '\u007E') {
			max = '\u007E';
		}
		
		// add this range to chars
		for(int i = 0; i < max - min + 1; i++) {
			char c = (char) (min + i);
			chars.remove((Character) c);
		}
		
		int randomNum = ThreadLocalRandom.current().nextInt(min, max + 1);
		char c = (char) randomNum;
		String s;
		if(index == example.length() - 1) {
			s = example.substring(0, index) + c;
		} else {
			s = example.substring(0, index) + c + example.substring(index+1);
		}
		return s;
	}
	
	@Deprecated
	public Map<String, Map<String, Boolean>> generateFromPythonScript(String example, String regex) {
		Map<String, Map<String, Boolean>> clusters = new LinkedHashMap<String, Map<String, Boolean>>();
		
		// parse the regex to an automaton
		RegExp exp = new RegExp(regex);
	    Automaton automaton = exp.toAutomaton();
		if(!automaton.isDeterministic()) {
			automaton.determinize();
			automaton.minimize();
		}
		
		// combining overlapping and adjacent edge intervals with same destination. 
		automaton.reduce();
		String s = writeAutomatonToFile(automaton);
		File dfa_file = new File(dfa_file_path);
		if(dfa_file.exists()) {
			dfa_file.delete();
		}
		try {
			FileUtils.write(dfa_file, s, Charset.defaultCharset());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// invoke the python script for input generation
		String[] cmd = {python3_path, input_generator_path, example,
				dfa_file_path, "-a", "" + num_of_examples_per_cluster};
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
		try {
			File fError = new File("example-generator-error");
	        processBuilder.redirectError(fError);
	        File fOutput = new File("example-generator-output");
	        processBuilder.redirectOutput(fOutput);
	        Process process = processBuilder.start();
	        process.waitFor();
	        int exitCode = process.exitValue();
	        
	        if(exitCode != 0) {
	        	// there is an error in the python script execution, abort
	        	return clusters;
	        }
	        
	        // read results from the python script output
	        String output = FileUtils.readFileToString(fOutput, Charset.defaultCharset());
	        ObjectMapper mapper = new ObjectMapper();
	        Map<String, Map<String, ArrayList<String>>> map = mapper.readValue(output, Map.class);
	        for(String cluster_id : map.keySet()) {
	        	Map<String, ArrayList<String>> examples = map.get(cluster_id);
	        	ArrayList<String> positives = examples.get("positive");
	        	ArrayList<String> negatives = examples.get("negative");
	        	Map<String, Boolean> cluster = new HashMap<String, Boolean>();
	        	for(String positive : positives) {
	        		cluster.put(positive, true);
	        	}
	        	
	        	for(String negative: negatives) {
	        		cluster.put(negative + "," + cluster_id, false);
	        		String explanation = 
	        				ExplanationGenerator.generateExplanation(negative, automaton, regex);
	        		if(clusters.containsKey(explanation)) {
	        			Map<String, Boolean> existing_cluster = clusters.get(explanation);
	        			existing_cluster.putAll(cluster);
	        			clusters.put(explanation, existing_cluster);
	        		} else {
	        			Map<String, Boolean> new_cluster = new HashMap<String, Boolean>();
	        			new_cluster.putAll(cluster);
	        			clusters.put(explanation, new_cluster);
	        		}
	        	}
	        	
//	        	int index = Integer.parseInt(cluster_id);
//	        	clusters.put("Examples generated by changing the " + 
//	        			ExplanationGenerator.getOrdinalNum(index+1) + " character", cluster);
	        }
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return clusters;
	}
	
	@Deprecated
	public String writeAutomatonToFile(Automaton automaton) {
		// convert the automaton to the compatible format in a file that can be accepted by the python script
		Set<State> states = automaton.getStates();
		HashMap<State, Integer> map = new HashMap<State, Integer>();
		State initialState = automaton.getInitialState();
		map.put(initialState, 0);
		String s = "0" + System.lineSeparator();
		Set<State> finalStates = new HashSet<State>();
		int count = 1;
		for(State state : states) {
			int id;
			if(map.containsKey(state)) {
				id = map.get(state);
			} else {
				id = count;
				count++;
				map.put(state, id);
			}
			
			if(state.isAccept()) {
				finalStates.add(state);
			}
			
			List<Transition> edges = state.getSortedTransitions(false);
			for(Transition edge : edges) {
				State destination = edge.getDest();
				int d_id;
				if(map.containsKey(destination)) {
					d_id = map.get(destination);
				} else {
					d_id = count;
					count++;
					map.put(destination, d_id);
				}
				
				s += id + "," + d_id + ",";
				String minChar = toCharString(edge.getMin());
				String maxChar = toCharString(edge.getMax());
				s += minChar + "-" + maxChar + System.lineSeparator();
			}
		}
		// append the final states
		for(State state : finalStates) {
			s += map.get(state) + ",";
		}
		s = s.substring(0, s.length() - 1);
		return s;
	}
	
	@Deprecated
	String toCharString(char c) {
		if (c >= 0x21 && c <= 0x7e && c != '\\' && c != '"')
			return "" + c;
		else {
			String charString = "\\u";
			String s = Integer.toHexString(c);
			if (c < 0x10)
				charString += "000" + s;
			else if (c < 0x100)
				charString += "00" + s;
			else if (c < 0x1000)
				charString += "0" + s;
			else
				charString += s;
			
			return charString;
		}
	}
}

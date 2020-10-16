package edu.harvard.seas.synthesis;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

import edu.harvard.seas.synthesis.logging.SynthesisLogger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.BasicOperations;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;

@WebSocket
public class SynthesisServerHandler {
	Session session = null;
	private String ip = null;
	
	@OnWebSocketClose
	public void onClose(int statusCode, String reason) {
		this.session = null;
		System.out.println("Close: statusCode=" + statusCode + ", reason="
				+ reason);
	}

	@OnWebSocketError
	public void onError(Throwable t) {
		System.out.println("Error: " + t.getMessage());
	}
	
	@OnWebSocketConnect
	public void onConnect(Session session) {
		this.session = session;
		this.ip = session.getRemoteAddress().getAddress().getHostAddress();
		
		System.out.println("Connect: "
				+ session.getRemoteAddress().getAddress());
	}
	
	private static HashMap<String, String> dsl_to_automaton = null;

	@OnWebSocketMessage
	public void onMessage(String message) {
		System.out.println("Message: ");
		System.out.println(message);
		SynthesisLogger.getSynthesisLogger().logString(message);

		if(message.equals("Reset") || message.equals("Window closed")) {
			// the synthesis task has been changed
			dsl_to_automaton = null;
			ResnaxRunner.reset();
		} else if (message.startsWith("Synthesize Regexes:")) {
			String runID = UUID.randomUUID().toString();
			message = message.substring("Synthesize Regexes: ".length());
			String s1 = message.split("\n")[0];
			String s2 = message.split("\n")[1];
			// Parse the json message
			ObjectMapper mapper = new ObjectMapper();
			try {
				Example[] examples = mapper.readValue(s1, Example[].class);
				Regex[] regexes = mapper.readValue(s2, Regex[].class);

				SynthesisLogger.getSynthesisLogger().logString(MessageFormat.format("[Run:{0}] Number of examples: {1}", runID, examples.length));
				logOperatorAnnotation(runID, regexes);
				
				// invoke the resnax runner
				ResnaxRunner runner = ResnaxRunner.getInstance();
				List<String> new_regexes = runner.run(examples, regexes);
				if(new_regexes.size() > 20) {
					// only display the first 20 to avoid an error "Frame is too large"
					new_regexes = new_regexes.subList(0, 20);
				}
				sendJSONMessage(new_regexes, "regexes");
				dsl_to_automaton = runner.dsl_to_automaton_regex;

				if(new_regexes.size() == 0){
					SynthesisLogger.getSynthesisLogger().logString(MessageFormat.format("[Run:{0}] Synthesis timed out", runID));
				} else {
					SynthesisLogger.getSynthesisLogger().logObjectWithMessage(MessageFormat.format("[Run:{0}] Synthesis successful. Generated {1} regexes.", runID, new_regexes.size()), new_regexes);
				}

			} catch (JsonParseException e) {
				e.printStackTrace();
			} catch (JsonMappingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (message.startsWith("Generate Examples:")) {
			message = message.substring("Generate Examples: ".length());
			String s1 = message.split("\n")[0];
			String s2 = message.split("\n")[1];
			// Parse the json message
			ObjectMapper mapper = new ObjectMapper();
			try {
				String[] examples = mapper.readValue(s1, String[].class);
				String[] regexes = mapper.readValue(s2, String[].class);
				
				if(dsl_to_automaton == null) {
					return;
				}
				
				// generate the synthetic examples
				if(regexes.length == 1) {
					Map<String, Map<String, Boolean>> similarExamples = generateSimilarExamples(regexes[0], examples);
					Map<String, Map<String, Boolean>> wildExamples = generateWildExamples(regexes[0], examples);
					
					// reorder wild examples to show unseen corner cases first
					Map<String, Map<String, Boolean>> reorderedWildExamples = 
							reorderWildExamples(similarExamples, wildExamples);
					sendJSONMessage(similarExamples, reorderedWildExamples, "examples");

					SynthesisLogger.getSynthesisLogger().logObjectWithMessage("similar examples: ", similarExamples);
					SynthesisLogger.getSynthesisLogger().logObjectWithMessage("wild examples: ", reorderedWildExamples);
				} else {
					// the user wants to generate examples for multiple regexes
					// focus on generating input examples that exhibit different behaviors among those regexes
					Map<String, Map<String, Boolean[]>> similarExamples = 
							generateSimilarDistinguishingExamples(regexes, examples);
					Map<String, Map<String, Boolean[]>> wildExamples = 
							generateWildDistinguishingExamples(regexes);
					
					sendJSONMessage(similarExamples, wildExamples, "examples");

					SynthesisLogger.getSynthesisLogger().logObjectWithMessage("similar examples: ", similarExamples);
					SynthesisLogger.getSynthesisLogger().logObjectWithMessage("wild examples: ", wildExamples);
				}
			} catch (JsonParseException e) {
				e.printStackTrace();
			} catch (JsonMappingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}				
		}
	}

	private void logOperatorAnnotation(String runID, Regex[] regexes){
		try {
			if (regexes.length <= 1) {
				return;
			}
			List<Regex> regexList = Arrays.asList(regexes);
			regexList = regexList.subList(1, regexList.size());

			StringBuilder stringBuilder = new StringBuilder();
			int count = 0;
			List<String> annotations = new ArrayList<>();

			for(Regex regex : regexList) {
				stringBuilder.delete(0, stringBuilder.length());
				if (regex.include.length == 0 && regex.exclude.length == 0) {
					continue;
				}
				stringBuilder.append("[" + regex.regex + "] ");
				if (regex.include.length > 0) {
					count += regex.include.length;
					stringBuilder.append("include: ");
					stringBuilder.append(String.join(",", regex.include));
					stringBuilder.append(";");
				}
				if (regex.exclude.length > 0) {
					count += regex.exclude.length;
					stringBuilder.append("exclude: ");
					stringBuilder.append(String.join(",", regex.exclude));
					stringBuilder.append(";");
				}

				annotations.add(stringBuilder.toString());
			}

			SynthesisLogger.getSynthesisLogger().logString(MessageFormat.format("[Run:{0}] Number of Operator annotations: {1} \n{2}", runID, count, String.join("\n", annotations)));
		}
		catch (Exception e) {
			SynthesisLogger.getSynthesisLogger().logException(e);
		}

	}

	private Map<String, Map<String, Boolean>> reorderWildExamples(Map<String, Map<String, Boolean>> familiarExamples,
			Map<String, Map<String, Boolean>> wildExamples) {
		LinkedHashMap<String, Map<String, Boolean>> map = new LinkedHashMap<String, Map<String, Boolean>>();
		
		// 1. add empty string cluster first
		for(String s : wildExamples.keySet()) {
			if(s.contains("non-empty string")) {
				map.put(s, wildExamples.get(s));
				break;
			}
		}
		
		// 2. add clusters about char length
		for(String s : wildExamples.keySet()) {
			if(s.contains("more than")) {
				map.put(s, wildExamples.get(s));
				break;
			}
		}
		
		// 3. add clusters that have not been seen in the familiar examples
		for(String s : wildExamples.keySet()) {
			if(!familiarExamples.containsKey(s)) {
				map.put(s, wildExamples.get(s));
			}
		}
		
		// 4. add the remaining clusters
		for(String s : wildExamples.keySet()) {
			if(!map.containsKey(s)) {
				map.put(s, wildExamples.get(s));
			}
		}
		
		return map;
	}
	
	private Map<String, Map<String, Boolean>> generateSimilarExamples(String regex, String[] examples) {
		String automaton_regex = dsl_to_automaton.get(regex);
		Map<String, Map<String, Boolean>> clusters = new LinkedHashMap<String, Map<String, Boolean>>();
		ExampleBasedInputGenerator exGen = new ExampleBasedInputGenerator();
		for(String example : examples) {
			Map<String, Map<String, Boolean>> map = exGen.generate(example, automaton_regex, regex);
			for(String explanation : map.keySet()) {
				Map<String, Boolean> cluster = map.get(explanation);
				if(clusters.containsKey(explanation)) {
					Map<String, Boolean> existingCluster = clusters.get(explanation);
					existingCluster.putAll(cluster);
					clusters.put(explanation, existingCluster);
				} else {
					Map<String, Boolean> new_cluster = new HashMap<String, Boolean>();
					new_cluster.putAll(cluster);
					clusters.put(explanation, new_cluster);
				}
			}
		}
		
		if(clusters.size() == 1 
				&& clusters.containsKey("Positive examples only")) {
			String s = "We didn't find any negative examples. It seems this regex can accept any string. " + 
    				"Do you want to double check some corner cases instead?";
			Map<String, Boolean> cluster = clusters.get("Positive examples only");
			clusters.clear();
			clusters.put(s, cluster);
		} else {
			if(clusters.containsKey("Positive examples only")) {
				clusters.remove("Positive examples only");
			}
		}
		
		return clusters;
	}
	
	private Map<String, Map<String, Boolean>> generateWildExamples(String regex, String[] examples) {
		String automaton_regex = dsl_to_automaton.get(regex);
		CoverageDrivenInputGenerator gen = new CoverageDrivenInputGenerator(regex);
		Map<String, Map<String, Boolean>> clusters = gen.generate(automaton_regex, examples);	
		return clusters;
	}
	
	private Map<String, Map<String, Boolean[]>> generateSimilarDistinguishingExamples(String[] regexes, String[] examples) {
		Map<String, Map<String, Boolean[]>> distinguishing_examples = new LinkedHashMap<String, Map<String, Boolean[]>>();
		
		// convert regexes to automata
		Automaton[] automata = new Automaton[regexes.length];					
		for(int i = 0; i < regexes.length; i++) {
			String automaton_regex = dsl_to_automaton.get(regexes[i]);
			RegExp exp = new RegExp(automaton_regex);
			Automaton a = exp.toAutomaton();
			automata[i] = a;
		}
		
		HashSet<String> tested = new HashSet<String>();
		for(int i = 0; i < regexes.length; i++) {
			String regex = regexes[i];
			Map<String, Map<String, Boolean>> clusters = generateSimilarExamples(regex, examples);
			for(String explanation : clusters.keySet()) {
				Map<String, Boolean> cluster = clusters.get(explanation);
				for(String example : cluster.keySet()) {
					boolean result = cluster.get(example);
					if(!result) {
						// remove the failure-inducing character index appended at the end of the neagtive example
						example = example.substring(0, example.lastIndexOf(','));
					}
					
					if(tested.contains(example)) {
						continue; 
					}
					
					Boolean[] results = new Boolean[regexes.length];
					results[i]  = result;
					for(int j = 0; j < regexes.length; j++) {
						if(j != i) {
							boolean result2 = automata[j].run(example);
							results[j] = result2;
						}
					}
					
					for(Boolean b : results) {
						if(b != result) {
							// this is a distinguishing example
							// generate explanation for all failed regexes
							String explanation2 = 
									ExplanationGenerator.generateExplanation(example, automata, regexes, results);
							
							Map<String, Boolean[]> map;
							if(distinguishing_examples.containsKey(explanation2)) {
								map = distinguishing_examples.get(explanation2);
							} else {
								map = new HashMap<String, Boolean[]>();
							}
							
							map.put(example, results);
							distinguishing_examples.put(explanation2, map);
							break;
						}
					}
					
					tested.add(example);
				}
			}
		}
		
		return distinguishing_examples;
	}
	
	private Map<String, Map<String, Boolean[]>> generateWildDistinguishingExamples(String[] regexes) {
		Map<String, Map<String, Boolean[]>> distinguishing_examples = new HashMap<String, Map<String, Boolean[]>>();
		
		Automaton[] automata = new Automaton[regexes.length];					
		for(int i = 0; i < regexes.length; i++) {
			String automaton_regex = dsl_to_automaton.get(regexes[i]);
			RegExp exp = new RegExp(automaton_regex);
			Automaton a = exp.toAutomaton();
			automata[i] = a;
		}
		
		CoverageDrivenInputGenerator gen = new CoverageDrivenInputGenerator();
		for(int i = 0; i < automata.length; i++) {
			Automaton a1 = automata[i];
			for(int j = i + 1; j < automata.length; j++) {
				Automaton a2 = automata[j];
				Automaton diff1 = BasicOperations.minus(a1, a2);
				ArrayList<Map<String, Boolean>> clusters = 
						gen.generateInputStrings(diff1, new HashSet<List<State>>(), true);
				for(Map<String, Boolean> cluster : clusters) {
					for(String example : cluster.keySet()) {
						Boolean[] results = new Boolean[automata.length];
						results[i] = true;
						results[j] = false;
						for(int k = 0; k < automata.length; k++) {
							if(k!=i && k!=j) {
								results[k] = automata[k].run(example);
							}
						}
						
						String explanation = 
								ExplanationGenerator.generateExplanation(example, automata, regexes, results);
						
						Map<String, Boolean[]> map;
						if(distinguishing_examples.containsKey(explanation)) {
							map = distinguishing_examples.get(explanation);
						} else {
							map = new HashMap<String, Boolean[]>();
						}
						
						map.put(example, results);
						distinguishing_examples.put(explanation, map);
					}
				}
				
				Automaton diff2 = BasicOperations.minus(a2, a1);
				clusters = gen.generateInputStrings(diff2, new HashSet<List<State>>(), true);
				for(Map<String, Boolean> cluster : clusters) {
					for(String example : cluster.keySet()) {
						Boolean[] results = new Boolean[automata.length];
						results[i] = false;
						results[j] = true;
						for(int k = 0; k < automata.length; k++) {
							if(k!=i && k!=j) {
								results[k] = automata[k].run(example);
							}
						}
						
						String explanation = 
								ExplanationGenerator.generateExplanation(example, automata, regexes, results);
						
						Map<String, Boolean[]> map;
						if(distinguishing_examples.containsKey(explanation)) {
							map = distinguishing_examples.get(explanation);
						} else {
							map = new HashMap<String, Boolean[]>();
						}
						
						map.put(example, results);
						distinguishing_examples.put(explanation, map);
					}
				}
			}
		}
		
		return distinguishing_examples;
	}
	
	private void sendJSONMessage(Object sentObject, String header) {
		// send the JSON message to the front end
		ObjectMapper mapper = new ObjectMapper();
        try {
        	String jsonMessage = mapper.writeValueAsString(sentObject);
            System.out.println(jsonMessage);
            session.getRemote().sendString(header + ": " + jsonMessage);
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	private void sendJSONMessage(Object o1, Object o2, String header) {
		// send the JSON message to the front end
		ObjectMapper mapper = new ObjectMapper();
        try {
        	String jsonMessage1 = mapper.writeValueAsString(o1);
            System.out.println(jsonMessage1);
            String jsonMessage2 = mapper.writeValueAsString(o2);
            System.out.println(jsonMessage2);
            session.getRemote().sendString(header + ": " + jsonMessage1 + "\n" + jsonMessage2);
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
}

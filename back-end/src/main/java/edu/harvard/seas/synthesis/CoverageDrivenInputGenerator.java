package edu.harvard.seas.synthesis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.javatuples.Pair;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.BasicOperations;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;

public class CoverageDrivenInputGenerator {
	int num = 5; // num of strings per cluster
	int cap = 20;
	
	String dsl_regex = "";
	
	public CoverageDrivenInputGenerator() {
		
	}
	
	public CoverageDrivenInputGenerator(String dsl_regex) {
		this.dsl_regex = dsl_regex;
	}
	
	public Map<String, Map<String, Boolean>> generate(String regex, String[] examples) {
		RegExp exp = new RegExp(regex);
		Automaton a = exp.toAutomaton();
		if(!a.isDeterministic()) {
			a.determinize();
			a.minimize();
		}
		
		// combining overlapping and adjacent edge intervals with same destination. 
		a.reduce();
		
		// identify which paths have been covered
		// note that here we assume all examples are positive examples
//		Set<List<State>> coveredPaths = new HashSet<List<State>>();
//		for(String s : examples) {
//			List<State> path = new ArrayList<State>();
//			State p = a.getInitialState();
//			path.add(p);
//			for (int i = 0; i < s.length(); i++) {
//				State q = p.step(s.charAt(i));
//				path.add(q);
//				p = q;
//			}
//			coveredPaths.add(path);
//		}
		
		// generate a pool of positive examples
		// try to generate all positive examples first
//		ArrayList<Map<String, Boolean>> pos = generateInputStrings(a, new HashSet<List<State>>(), true);
		
		ArrayList<Map<String, Boolean>> neg = generateInputStrings(a, new HashSet<List<State>>(), false);
		
		if(neg.isEmpty()) {
			// this regex accepts any string
			ArrayList<Map<String, Boolean>> pos = generateInputStrings(a, new HashSet<List<State>>(), true);
			// aggregate all positive strings
			Map<String, Boolean> m = new HashMap<String, Boolean>();
			for(Map<String, Boolean> cluster : pos) {
				m.putAll(cluster);
			}
			Map<String, Map<String, Boolean>> clusters = new LinkedHashMap<String, Map<String, Boolean>>();
			if(m.isEmpty()) {
				return clusters;
			} else {
				String s = "We didn't find any negative examples. This regex can accept any string.";
	    		clusters.put(s, m);
				return clusters;
			}
		}
		
		// re-cluster negative examples based on their failure reasons
		Map<String, Map<String, Boolean>> negWithExplanation = new LinkedHashMap<String, Map<String, Boolean>>();
		for(Map<String, Boolean> cluster :  neg) {
			if(cluster.isEmpty()) {
				negWithExplanation.put("", cluster);
			} else {
				// pick one example
				String s = cluster.keySet().iterator().next();
				// remove the failure-inducing character index
				s = s.substring(0, s.lastIndexOf(','));
				String explanation = ExplanationGenerator.generateExplanation(s, a, dsl_regex);
//				System.out.println(explanation);
				if(negWithExplanation.containsKey(explanation)) {
					Map<String, Boolean> map = negWithExplanation.get(explanation);
					map.putAll(cluster);
					negWithExplanation.put(explanation, map);
				} else {
					Map<String, Boolean> map = new HashMap<String, Boolean>();
					map.putAll(cluster);
					negWithExplanation.put(explanation, map);
				}
			}
		}
		
		Map<String, Map<String, Boolean>> map = new LinkedHashMap<String, Map<String, Boolean>>();
		// enumerate paths to all positive examples
		Set<List<State>> paths = enumeratePaths(a);
		// TODO: try to pair each negative example is paired with a positive one
		for(String explanation : negWithExplanation.keySet()) {
			Map<String, Boolean> cluster = negWithExplanation.get(explanation);

			Map<String, Boolean> m;
			if(map.containsKey(explanation)) {
				m = map.get(explanation);
			} else {
				m = new LinkedHashMap<String, Boolean>();
			}
			
			for(String example : cluster.keySet()) {
				// put example into map
				m.put(example, false);
				
				example = example.substring(0, example.lastIndexOf(','));
				
				if(example.isEmpty()) {
					continue;
				}
				
				State p = a.getInitialState();
				List<State> l = new ArrayList<State>();
				for(int i = 0; i < example.length(); i++) {
					l.add(p);
					State q = p.step(example.charAt(i));
					if (q == null) {
						break;
					} else if (i == example.length() - 1 && !q.isAccept()) {
						l.add(q);
						break;
					}

					p = q;
				}
				
				String sub = example.substring(0, l.size()-1);
				for(List<State> path : paths) {
					if(path.containsAll(l)) {
						// generate a corresponding positive example
						String pos = continueToGenerateExample(sub, path);
						
						m.put(pos, true);
						break;
					}
				}
			}
			
			map.put(explanation, m);
		}
		
		return map;
	}
	
	
	public String continueToGenerateExample(String s, List<State> path) {
		String example = s;
		for(int i = s.length(); i < path.size()-1; i++) {
			State src = path.get(i);
			State dst = path.get(i+1);
			Set<Transition> ts = src.getTransitions();
			if(ts.size() > 0) {
				Transition t = ts.iterator().next();
				if(!t.getDest().equals(dst)) {
					// not the correct edge to the next state
					continue;
				}
								
				char min = t.getMin();
				if(min < '\u0020') {
					min = '\u0020';
				}
				char max = t.getMax();
				if(max > '\u007E') {
					max = '\u007E';
				}
				
				int randomNum = ThreadLocalRandom.current().nextInt(min, max + 1);
				char c = (char) randomNum;
				example += c;
			}
		}
		
		return example;
	}
	
	public ArrayList<Map<String, Boolean>> generateInputStrings(Automaton a, Set<List<State>> coveredPaths, boolean accepted) {
		Automaton curr = a;
		if(!accepted) {
			// generate negative examples
			// create a complement (or you can call it negation) of the automaton
			curr = BasicOperations.complement(a);
		}
		
		Set<List<State>> paths = enumeratePaths(curr);
		// explore paths that have not been covered by user-given examples
		paths.removeAll(coveredPaths);
		List<List<String>> clusters = generateBasedOnPath(paths, accepted);
//		if (a.getSingleton() != null) {
//			if (accepted)
//				return a.getSingleton();
//			else if (a.getSingleton().length() > 0)
//				return "";
//			else
//				return "\u0000";
//
//		}
		
		ArrayList<Map<String, Boolean>> l = new ArrayList<Map<String, Boolean>>();
		for(List<String> cluster : clusters) {
			HashMap<String, Boolean> m = new HashMap<String, Boolean>();
			for(String s : cluster) {
				if(!accepted) {
					// If this is a negative example
					// get the index of the failure-inducing character 
					// and append it to the end
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
					
					s += "," + index;
				}
				
				m.put(s, accepted);
				
				// apply the cap of examples in each cluster				
				if(m.size() == cap) break;
			}
			l.add(m);
		}
		
		return l;
	}
	
	public Set<List<State>> enumeratePaths(Automaton a) {
		Set<List<State>> acceptedPaths = new HashSet<List<State>>();
		
		// init 
		List<State> l = new ArrayList<State>();
		State s0 = a.getInitialState();
		l.add(s0);
		if(s0.isAccept()) {
			List<State> p0 = new ArrayList<State>();
			p0.add(s0);
			acceptedPaths.add(p0);
		}
		
		Set<List<State>> paths = new HashSet<List<State>>(); // the set of paths to explore
		paths.add(l);
		
		// use edge coverage to drive the traversal
		// state coverage is too weak while path coverage is unrealistic when there is a loop
		Set<Pair<State, State>> edges = new HashSet<Pair<State, State>>();
		for(State s : a.getStates()) {
			for(Transition t : s.getTransitions()) {
				edges.add(new Pair<State, State>(s, t.getDest()));
			}
		}
		
		while(!paths.isEmpty() && !hasFullCoverage(acceptedPaths, edges)) {
			Set<List<State>> newPaths = new HashSet<List<State>>();
			for(List<State> path : paths) {
				State q = path.get(path.size() - 1);
				
				// BFS 
				for (Transition t : q.getTransitions()) {
					// append to existing paths ending with q
					// duplicate these paths if there are multiple outcoming edges from q
					ArrayList<State> copy = new ArrayList<State>();
					copy.addAll(path);
					State dst = t.getDest();
					copy.add(dst);
					if(dst.isAccept()) {
						acceptedPaths.add(copy);
					}
					
					if(dst.getTransitions().size() > 0) {
						// add the current path to continue exploring
						newPaths.add(copy);
					}
				}
			}
			paths = newPaths;
		}
		
		return acceptedPaths;
	}
	
	private boolean hasFullCoverage(Set<List<State>> paths, Set<Pair<State, State>> edges) {
		Set<Pair<State, State>> coverSet = new HashSet<Pair<State, State>>();
		for(List<State> p : paths) {
			for(int i = 0; i < p.size() - 1; i++) {
				State src = p.get(i);
				State dst = p.get(i+1);
				coverSet.add(new Pair<State, State>(src, dst));
			}
		}
		
		if(coverSet.equals(edges)) {
			return true;
		} else {
			return false;
		}
	}
	
	public List<List<String>> generateBasedOnPath(Set<List<State>> paths, boolean accepted) {
		List<List<String>> clusters = new ArrayList<List<String>>();
		// sort by length
		List<List<State>> sortedPaths = new ArrayList<List<State>>();
		sortedPaths.addAll(paths);
		Comparator<List<State>> pathLengthComparator = new Comparator<List<State>>()
	    {
	        @Override
	        public int compare(List<State> p1, List<State> p2)
	        {
	            return Integer.compare(p1.size(), p2.size());
	        }
	    };
	    
	    Collections.sort(sortedPaths, pathLengthComparator);
	    
		for(List<State> p : sortedPaths) {
			List<String> cluster = new ArrayList<String>();
			List<String> temp = generateBasedOnPath(p);
			cluster.addAll(temp);
			
//			System.out.println(cluster);
			clusters.add(cluster);
		}
		
		return clusters;
	}
	
	public List<String> generateBasedOnPath(List<State> path) {
		ArrayList<String> examples = new ArrayList<String>();
		examples.add("");
		
		if(path.size() == 1) {
			// the initial state is an accept state
			// meaning it accepts an empty string
			return examples;
		}
		
		for(int i = 0; i < path.size() - 1; i++) {
			State src = path.get(i);
			State dst = path.get(i+1);
			ArrayList<String> newExamples = new ArrayList<String>();
			for(Transition t : src.getTransitions()) {
				if(!t.getDest().equals(dst)) {
					// not the correct edge to the next state
					continue;
				}
				
				ArrayList<String> copy = new ArrayList<String>();
				
				char min = t.getMin();
				if(min < '\u0020') {
					min = '\u0020';
				}
				char max = t.getMax();
				if(max > '\u007E') {
					max = '\u007E';
				}
				
				// append this random char to the end of each path
				for(int j = 0; j < examples.size(); j++) {
					// pick a random char between min and max
					HashSet<String> set = new HashSet<String>();
					for(int k = 0; k < num; k++) {
						int randomNum = ThreadLocalRandom.current().nextInt(min, max + 1);
						char c = (char) randomNum;
						set.add(c + "");
					}
					
					for(String c : set) {
						String p = examples.get(j);
						String np = p + c;
						copy.add(np);
					}
				}
				
				newExamples.addAll(copy);
			}
			
			if(newExamples.size() > 100) {
				// truncate to avoid out of memory error
				examples = new ArrayList<String>(newExamples.subList(0, 100));
			} else {
				examples = newExamples;
			}
		}
		
		return examples;
	}
	
	static String getShortestExample(State s, boolean accepted) {
		Map<State,String> path = new HashMap<State,String>();
		LinkedList<State> queue = new LinkedList<State>();
		path.put(s, "");
		queue.add(s);
		String best = null;
		while (!queue.isEmpty()) {
			State q = queue.removeFirst();
			String p = path.get(q);
			if (q.isAccept() == accepted) {
				if (best == null || p.length() < best.length() || (p.length() == best.length() && p.compareTo(best) < 0))
					best = p;
			} else {
				for (Transition t : q.getTransitions()) {
					String tp = path.get(t.getDest());
					char min = t.getMin();
					if(min < '\u0020') {
						min = '\u0020';
					}
					char max = t.getMax();
					if(max > '\u007E') {
						max = '\u007E';
					}
					
					// pick a random number between min and max
					int randomNum = ThreadLocalRandom.current().nextInt(min, max + 1);
					char c = (char) randomNum;
					String np = p + c;
					if (tp == null || (tp.length() == np.length() && np.compareTo(tp) < 0)) {
						if (tp == null)
							queue.addLast(t.getDest());
						path.put(t.getDest(), np);
					}
				}
			}
		}
		return best;
	}
}

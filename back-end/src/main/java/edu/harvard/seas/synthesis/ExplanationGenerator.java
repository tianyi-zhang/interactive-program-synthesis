package edu.harvard.seas.synthesis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.text.StringEscapeUtils;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;

public class ExplanationGenerator {
	public static String getOrdinalNum(int i) {
	    if(i == 1) {
	        return "1st";
	    } else if (i == 2) {
	        return "2nd";
	    } else if (i == 3) {
	        return "3rd";
	    } else {
	        return i + "th";
	    }
	}
	
	// Currently we only support generate explanations for negative examples
	public static String generateExplanation(String s, Automaton a, String dsl_regex) {
		String explanation = "";
		
		if(s.isEmpty()) {
			explanation = "Examples rejected because the selected regex expects non-empty string.";
			return explanation;
		}
		
		State p = a.getInitialState();
		for (int i = 0; i < s.length(); i++) {
			State q = p.step(s.charAt(i));
			if (q == null) {
				if(p.isAccept() && p.getTransitions().size() == 0) {
					explanation = "Examples rejected because they have "
							+ "more than " + i + " characters.";
				} else {
					String range = getAcceptedCharsFromState(p);
					if(range.startsWith("not ")) {
						String chars = range.substring(4);
						if(chars.length() == 1) {
							// single character
							String l = "concat(<" + chars + ">,<" + chars + ">)";
							if(dsl_regex.contains(l)) {
								// special case
								explanation = "Examples rejected because they contain <span class=\"code\">" + StringEscapeUtils.escapeHtml4(chars + chars) + "</span>.";
								break;
							}
						} 
						
						explanation = "Examples rejected because the " + ExplanationGenerator.getOrdinalNum(i+1) 
							+ " character is <span class=\"code\">" + StringEscapeUtils.escapeHtml4(chars) + "</span>.";
					} else {
						explanation = "Examples rejected because the " + ExplanationGenerator.getOrdinalNum(i+1) 
							+ " character is not <span class=\"code\">" + StringEscapeUtils.escapeHtml4(range) + "</span>.";
					}
				}
				
				break;
			} else if (i == s.length() - 1 && !q.isAccept()) {
				// the regex expects more characters
				if(dsl_regex.contains("contain")) {
					List<Transition> tToAccept = new ArrayList<Transition>();
					for(Transition t : q.getTransitions()) {
						if(t.getDest().isAccept()) {
							tToAccept.add(t);
						}
					}
					
					if(tToAccept.isEmpty()) {
						// none of the following states are accept state
						// the regex is likely to expect a sequence of chars
						// pick the transition that only accepts a single char
						String thatChar = null;
						for(Transition t : q.getTransitions()) {
							if(t.getMax() == t.getMin()) {
								thatChar = t.getMin() + "";
								break;
							} else if (t.getMax() == '9') {
								thatChar = t.getMin() + " to 9";
								break;
							} else if (t.getMax() == 'a' && t.getMin() == 'z') {
								thatChar = "a to z";
								break;
							} else if (t.getMax() == 'A' && t.getMin() == 'Z') {
								thatChar = "A to Z";
								break;
							}
						}
						
						if(thatChar == null) {
							explanation = "Failed to generate a cluster header";
						} else {
							explanation = "Examples rejected because the selected regex "
									+ "expects more characters. The next character can be <span class=\"code\">" 
									+ StringEscapeUtils.escapeHtml4(thatChar) + "</span>.";
						}
					} else {
						explanation = "Examples rejected because the selected regex "
								+ "expects the example to contain <span class=\"code\">" + 
								StringEscapeUtils.escapeHtml4(getAcceptedCharsInTransitions(tToAccept)) + "</span>.";
					}								
				} else if (dsl_regex.contains("endwith")) {
					List<Transition> tToAccept = new ArrayList<Transition>();
					for(Transition t : q.getTransitions()) {
						if(t.getDest().isAccept()) {
							tToAccept.add(t);
						}
					}
					
					explanation = "Examples rejected because the selected regex "
							+ "expects the example to end with <span class=\"code\">" 
							+ StringEscapeUtils.escapeHtml4(getAcceptedCharsInTransitions(tToAccept)) + "</span>.";
				} else {
					String range  = getAcceptedCharsFromState(p);
					if (range.startsWith("not " )){
						explanation = "Examples rejected because the selected regex "
								+ "expects more characters. The next character cannot be <span class=\"code\">" 
								+ StringEscapeUtils.escapeHtml4(range.substring(4)) + "</span>.";
					} else {
						explanation = "Examples rejected because the selected regex "
								+ "expects more characters. The next character can be <span class=\"code\">" 
								+ StringEscapeUtils.escapeHtml4(range) + "</span>.";
					}
				}
				
				break;
			}
			
			p = q;
		}
		
		return explanation;
	}
	
	public static String generateExplanation(String example, Automaton[] automata, String[] regexes, Boolean[] results) {
		String explanation = "";
		Map<String, String> m = new HashMap<String, String>(); // explanation -> an concatenation of regexes with the same explanation
		for (int j = 0; j < results.length; j++) {
			if(!results[j]) {
				String s = ExplanationGenerator.generateExplanation(example, automata[j], regexes[j]);
				if(m.containsKey(s)) {
					String existing_regex = m.get(s);
					existing_regex += " and " + regexes[j];
					m.put(s, existing_regex);
				} else {
					m.put(s, regexes[j]);
				}
			}
		}
		
		for(String s : m.keySet()) {
			String regex = m.get(s);
			if(s.contains("the selected regex") && regex.contains(" and ")) {
				// multiple regexes fail for the same reason
				s = s.replace("the selected regex", "these regexes");
			} else if (s.contains("the selected regex")) {
				// single regex fails
				s = s.replace("the selected regex", "this regex");
			}
			if(explanation.endsWith("and also rejected by ")) {
				explanation += regex + s.substring("Examples rejected".length());
			} else {
				s  = s.replace("Examples rejected", "Examples rejected by <span class=\"code\">" 
														+ StringEscapeUtils.escapeHtml4(regex) + "</span>");
				explanation += s;
			}
			
			explanation += ", and also rejected by ";
		}
		
		explanation = explanation.substring(0, explanation.lastIndexOf(','));
		
		return explanation;
	}
	
	private static String getAcceptedCharsFromState(State s) {
		if(s.getTransitions().isEmpty()) {
			return "";
		}
		
		List<Transition> trans = s.getSortedTransitions(true);
		String range = getAcceptedCharsInTransitions(trans);
		return range;
	}
	
	private static String getAcceptedCharsInTransitions(List<Transition> trans) {
		char min = trans.get(0).getMin();
		char max = trans.get(trans.size() - 1).getMax();
		String range = "";
		if(min == '\u0000' && max == '\uffff') {
			// this represents a character from the very first char to the end char
			// maybe without certain characters
			range = "";
			if(trans.size() == 1) {
				range = "any characters";
			} else {
				range = "not ";
				for(int i = 0; i < trans.size() - 1; i++) {
					Transition t1 = trans.get(i);
					Transition t2 = trans.get(i+1);
					char start = (char) (t1.getMax() + 1);
					char end = (char) (t2.getMin() - 1);
					
					if(start < '\u0020') {
						start = '\u0020';
					}
					if(end > '\u007E') {
						end = '\u007E';
					}
					
					if(start == end) {
						range += (start == '\u0020' ? "empty space" : start) + " or ";
					} else if (end - start == 1) {
						range += (start == '\u0020' ? "empty space" : start) + " or " + end + " or "; 
					} else if (end - start == 2) {
						range += (start == '\u0020' ? "empty space" : start) + " or " + (char) (start + 1) + " or " + end + " or ";
					} else {
						range += (start == '\u0020' ? "empty space" : start) + " to " + end + " or ";
					}
				}
				range = range.substring(0, range.length() - 4);
			}
		} else {
			for (Transition t : trans) {
				char start = t.getMin();
				if(start < '\u0020') {
					start = '\u0020';
				}
				char end = t.getMax();
				if(end > '\u007E') {
					end = '\u007E';
				}
				
				if(start == end) {
					range += (start == '\u0020' ? "empty space" : start) + " or ";
				} else if (end - start == 1) {
					range += (start == '\u0020' ? "empty space" : start) + " or " + end + " or "; 
				} else if (end - start == 2) {
					range += (start == '\u0020' ? "empty space" : start) + " or " + (char) (start + 1) + " or " + end + " or ";
				} else {
					range += (start == '\u0020' ? "empty space" : start) + " to " + end + " or ";
				}
			}
			
			range = range.substring(0, range.length() - 4);
		}
		
		return range;
	}
}

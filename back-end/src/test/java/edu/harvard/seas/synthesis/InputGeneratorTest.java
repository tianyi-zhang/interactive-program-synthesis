package edu.harvard.seas.synthesis;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.Ignore;
import org.junit.Test;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.BasicOperations;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;

public class InputGeneratorTest {

	@Test
	@Ignore
	public void testAutomatonToFile1() {
		String regex = "([^D]){1,}";
		RegExp exp = new RegExp(regex);
	    Automaton a = exp.toAutomaton();
		ExampleBasedInputGenerator gen = new ExampleBasedInputGenerator();
		String s = gen.writeAutomatonToFile(a);
		System.out.println(s);
	}
	
	@Test
	@Ignore
	public void testAutomatonToFile2() {
		String regex = ".*((([C])|([A])))";
		RegExp exp = new RegExp(regex);
	    Automaton a = exp.toAutomaton();
		ExampleBasedInputGenerator gen = new ExampleBasedInputGenerator();
		String s = gen.writeAutomatonToFile(a);
		System.out.println(s);
	}
	
	@Test
	@Ignore
	public void testAutomatonToFile3() {
		String regex = "([^D]){1,10}";
		RegExp exp = new RegExp(regex);
	    Automaton a = exp.toAutomaton();
		ExampleBasedInputGenerator gen = new ExampleBasedInputGenerator();
		String s = gen.writeAutomatonToFile(a);
		System.out.println(s);
	}
	
	@Test
	@Ignore
	public void testAutomatonToFile4() {
		String regex = "((([A])|((([B])|([C]))))){1,}";
		RegExp exp = new RegExp(regex);
	    Automaton a = exp.toAutomaton();
		ExampleBasedInputGenerator gen = new ExampleBasedInputGenerator();
		String s = gen.writeAutomatonToFile(a);
		System.out.println(s);
	}
	
	@Test
	@Ignore
	public void testAutomatonToFile5() {
		String regex = "(([+])?)(([9])([0-9]))";
		RegExp exp = new RegExp(regex);
	    Automaton a = exp.toAutomaton();
		ExampleBasedInputGenerator gen = new ExampleBasedInputGenerator();
		String s = gen.writeAutomatonToFile(a);
		System.out.println(s);
	}
	
	
	@Test
	@Ignore
	public void testAutomatonToFile6() {
		String regex = "(([+])?)(([1-9])([1-9]))";
		RegExp exp = new RegExp(regex);
	    Automaton a = exp.toAutomaton();
		ExampleBasedInputGenerator gen = new ExampleBasedInputGenerator();
		String s = gen.writeAutomatonToFile(a);
		System.out.println(s);
	}
		
	@Test
	public void testInputGenerator1() {
		String regex = "(([+])?)(([1-9])([1-9]))"; // concat(optional(<+>),concat(<num1-9>,<num1-9>)
		String example = "91";
		ExampleBasedInputGenerator gen = new ExampleBasedInputGenerator();
		Map<String, Map<String, Boolean>> clusters = gen.generate(example, regex, "concat(optional(<+>),concat(<num1-9>,<num1-9>)");
		assertEquals(3, clusters.size());
	}
	
	@Test
	@Ignore
	public void testInputGenerator2() {
		String regex = "(([+])?)(([9])([0-9]))"; // concat(optional(<+>),concat(<9>,<num>)
		String example = "+91";
		ExampleBasedInputGenerator gen = new ExampleBasedInputGenerator();
		Map<String, Map<String, Boolean>> clusters = gen.generate(example, regex, "concat(optional(<+>),concat(<9>,<num>)");
		assertEquals(4, clusters.size()); // this could be flaky depends on the sampled examples.
	}
	
	@Test
	public void testInputGenerator3() {
		String regex = "((([A])|((([B])|([C]))))){1,}";
		String example = "ABC";
		ExampleBasedInputGenerator gen = new ExampleBasedInputGenerator();
		Map<String, Map<String, Boolean>> clusters = gen.generate(example, regex, "repeatatleast(or(<A>,or(<B>,<C>),1))");
		assertEquals(3, clusters.size());
	}
	
	@Test
	public void testInputGenerator4() {
		String regex = "([^D]){1,10}"; 
		String example = "ABC";
		ExampleBasedInputGenerator gen = new ExampleBasedInputGenerator();
		Map<String, Map<String, Boolean>> clusters = gen.generate(example, regex, "repeatatleast(not(<D>),1,10))");
		assertEquals(3, clusters.size());
	}
	
	@Test
	public void testInputGenerator5() {
		String regex = "~(.*(([-])([-])).*)"; 
		String example = "a-b";
		ExampleBasedInputGenerator gen = new ExampleBasedInputGenerator();
		Map<String, Map<String, Boolean>> clusters = gen.generate(example, regex, "not(contain(concat(<->,<->)))");
		assertEquals(1, clusters.size());
	}
	
	@Test
	public void testInputGenerator6() {
		String regex = "(([+])?).*";
		String example = "+abc";
		ExampleBasedInputGenerator gen = new ExampleBasedInputGenerator();
		Map<String, Map<String, Boolean>> clusters = gen.generate(example, regex, "startwith(optional(<+>))");
		// it can be matched with anything!!!!
		assertEquals(1, clusters.size());
		String header = "Positive examples only";
		assertEquals(header, clusters.keySet().iterator().next());
	}
	
	@Test
	public void testInputGenerator7() {
		String regex = ".*(([1])([0-9])).*";
		String example = "123";
		ExampleBasedInputGenerator gen = new ExampleBasedInputGenerator();
		Map<String, Map<String, Boolean>> clusters = gen.generate(example, regex, "contain(concat(<1>,<num>))");
		assertEquals(1, clusters.size());
	}
	
	@Test
	public void testInputGenerator8() {
		String regex = ".*(([0-9])([2])).*";
		String example = "12+";
		ExampleBasedInputGenerator gen = new ExampleBasedInputGenerator();
		Map<String, Map<String, Boolean>> clusters = gen.generate(example, regex, "	contain(concat(<num>,<2>))");
		assertEquals(1, clusters.size());
	}
		
	@Test
	public void testAutomatonMinus() {
		String regex1 = "([^D]){1,}";
		String regex2 = "([^D]){1,10}";
		RegExp exp1 = new RegExp(regex1);
		RegExp exp2 = new RegExp(regex2);
		Automaton automaton1 = exp1.toAutomaton();
		Automaton automaton2 = exp2.toAutomaton();
		Automaton diff1 = BasicOperations.minus(automaton1, automaton2);
		String accept1 = getShortestExample(diff1, true);
		System.out.println(diff1.toDot());
		System.out.println(accept1);
		
		Automaton diff2 = BasicOperations.minus(automaton2, automaton1);
		String accept2 = getShortestExample(diff2, true);
		System.out.println(diff2.toDot());
		System.out.println(accept2);
	}
	
	public static String getShortestExample(Automaton a, boolean accepted) {
		if (a.getSingleton() != null) {
			if (accepted)
				return a.getSingleton();
			else if (a.getSingleton().length() > 0)
				return "";
			else
				return "\u0000";

		}
		return getShortestExample(a.getInitialState(), accepted);
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
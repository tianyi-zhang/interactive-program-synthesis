package edu.harvard.seas.synthesis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.BasicOperations;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;

public class CoverageDrivenInputGenerationTest {
	@Test
	public void testGeneratePositiveExample1() {
		String regex = "([^D]){1,}";
		RegExp exp = new RegExp(regex);
		Automaton a = exp.toAutomaton(); // two states, one state has a loop
		System.out.println(a.toDot());
		CoverageDrivenInputGenerator gen = new CoverageDrivenInputGenerator();
		Set<List<State>> pathsToAccept = gen.enumeratePaths(a);
		assertEquals(2, pathsToAccept.size());
		
		List<List<String>> clusters = gen.generateBasedOnPath(pathsToAccept, true);
		assertEquals(2, clusters.size());
	}
	
	@Test
	public void testGenerateCornerCase1() {
		String regex = "([^D]){1,}"; // repeatatleast(notcc(<D>),1)
		
		String[] examples = new String[2];
		examples[0] = "A";
		examples[1] = "B";
		
		CoverageDrivenInputGenerator gen = new CoverageDrivenInputGenerator();
		Map<String, Map<String, Boolean>> clusters = gen.generate(regex, examples);
		
		assertEquals(4, clusters.size()); // 8 clusters if not clustering by explanation
	}
	
	@Test
	public void testGeneratePositiveExample2() {
		String regex = "([^D]){1,10}";
		RegExp exp = new RegExp(regex);
		Automaton a = exp.toAutomaton(); // 11 states, sequential, 10 of them are accept states
//		System.out.println(a.toDot());
		CoverageDrivenInputGenerator gen = new CoverageDrivenInputGenerator();
		Set<List<State>> pathsToAccept = gen.enumeratePaths(a);
		assertEquals(10, pathsToAccept.size());
		
		List<List<String>> clusters = gen.generateBasedOnPath(pathsToAccept, true);
		assertEquals(10, clusters.size());
	}
	
	@Test
	public void testGenerateCornerCase2() {
		String regex = "([^D]){1,10}"; // repeatatleast(notcc(<D>),1,10)
		
		String[] examples = new String[2];
		examples[0] = "A";
		examples[1] = "B";
		
		CoverageDrivenInputGenerator gen = new CoverageDrivenInputGenerator();
		Map<String, Map<String, Boolean>> clusters = gen.generate(regex, examples);
		
		assertEquals(12, clusters.size()); // 76 clusters if not clustering by explanation
	}
	
	@Test
	public void testGeneratePositiveExample3() {
		String regex = ".*((([C])|([A])))";
		RegExp exp = new RegExp(regex);
		Automaton a = exp.toAutomaton(); // two states, each state has a loop, and there is a loop between two states
//		System.out.println(a.toDot());
		CoverageDrivenInputGenerator gen = new CoverageDrivenInputGenerator();
		Set<List<State>> pathsToAccept = gen.enumeratePaths(a);
		assertEquals(7, pathsToAccept.size());
		
		List<List<String>> clusters = gen.generateBasedOnPath(pathsToAccept, true);
		assertEquals(7, clusters.size());
	}
	
	@Test
	public void testGenerateCornerCase3() {
		String regex = ".*((([C])|([A])))"; // endwith(or(<A>,<C>))
		
		String[] examples = new String[2];
		examples[0] = "A";
		examples[1] = "C";
		
		CoverageDrivenInputGenerator gen = new CoverageDrivenInputGenerator("endwith(or(<A>,<C>)");
		Map<String, Map<String, Boolean>> clusters = gen.generate(regex, examples);
		
		assertEquals(2, clusters.size()); // 14 clusters if not clustered by explanation
	}
	
	@Test
	public void testGeneratePositiveExample4() {
		String regex = "((([A])|((([B])|([C]))))){1,}";
		RegExp exp = new RegExp(regex);
		Automaton a = exp.toAutomaton(); // 2 states, one with a loop
//		System.out.println(a.toDot());
		CoverageDrivenInputGenerator gen = new CoverageDrivenInputGenerator();
		Set<List<State>> pathsToAccept = gen.enumeratePaths(a);
		assertEquals(2, pathsToAccept.size());
		
		List<List<String>> clusters = gen.generateBasedOnPath(pathsToAccept, true);
		assertEquals(2, clusters.size());
	}
	
	@Test
	public void testGenerateCornerCase4() {
		String regex = "((([A])|((([B])|([C]))))){1,}"; // repeatatleast(or(<A>,or(<B>,<C>),1))
		
		String[] examples = new String[2];
		examples[0] = "A";
		examples[1] = "C";
		
		CoverageDrivenInputGenerator gen = new CoverageDrivenInputGenerator("repeatatleast(or(<A>,or(<B>,<C>),1))");
		Map<String, Map<String, Boolean>> clusters = gen.generate(regex, examples);
		
		assertEquals(4, clusters.size()); // 8 clusters if not clustered by explanation
	}
	
	@Test
	public void testGeneratePositiveExample5() {
		String regex = "(([+])?)(([9])([0-9]))";
		RegExp exp = new RegExp(regex);
		Automaton a = exp.toAutomaton(); // 4 states, there is a branch, one accept state
//		System.out.println(a.toDot());
		CoverageDrivenInputGenerator gen = new CoverageDrivenInputGenerator();
		Set<List<State>> pathsToAccept = gen.enumeratePaths(a);
		assertEquals(2, pathsToAccept.size());
		
		List<List<String>> clusters = gen.generateBasedOnPath(pathsToAccept, true);
		assertEquals(2, clusters.size());
	}
	
	@Test
	public void testGenerateCornerCase5() {
		String regex = "(([+])?)(([9])([0-9]))";
		
		String[] examples = new String[2];
		examples[0] = "A";
		examples[1] = "C";
		
		CoverageDrivenInputGenerator gen = new CoverageDrivenInputGenerator("concat(optional(<+>),concat(<9>,<num>))");
		Map<String, Map<String, Boolean>> clusters = gen.generate(regex, examples);
		
		assertEquals(8, clusters.size()); // 8 clusters if not clustered by explanation
	}
	
	@Test
	public void testGenerateCornerCase6() {
		String regex = "((.*([B]).*)|([A]))";
		
		String[] examples = new String[2];
		examples[0] = "A";
		examples[1] = "C";
		
		CoverageDrivenInputGenerator gen = new CoverageDrivenInputGenerator("or(contain(<B>),<A>)");
		Map<String, Map<String, Boolean>> clusters = gen.generate(regex, examples);
		
		assertEquals(2, clusters.size()); // 2 clusters if not clustered by explanation
	}
	
	@Test
	public void testGenerateCornerCase7() {
		String regex = "(([+])?).*";
		
		String[] examples = new String[2];
		examples[0] = "A";
		examples[1] = "C";
		
		CoverageDrivenInputGenerator gen = new CoverageDrivenInputGenerator("startwith(optional(<+>))");
		Map<String, Map<String, Boolean>> clusters = gen.generate(regex, examples);
		
		assertEquals(1, clusters.size()); 
		String header = "We didn't find any negative examples. This regex can accept any string.";
		assertEquals(header, clusters.keySet().iterator().next());
	}
	
	@Test
	public void testGenerateNegativeExample1() {
		String regex = "([^D]){1,}";
		RegExp exp = new RegExp(regex);
		Automaton a = exp.toAutomaton(); // two states, one state has a loop
//		Automaton a2 = BasicOperations.complement(a);
//		System.out.println(a2.toDot());
		
		CoverageDrivenInputGenerator gen = new CoverageDrivenInputGenerator();
		ArrayList<Map<String, Boolean>> negatives = gen.generateInputStrings(a, new HashSet<List<State>>(), false);
		
		assertEquals(7, negatives.size());
	}
	
	@Test
	public void testGenerateNegativeExample2() {
		String regex = "([^D]){1,10}";
		RegExp exp = new RegExp(regex);
		Automaton a = exp.toAutomaton(); // 11 states, sequential, 10 of them are accept states
//		Automaton a2 = BasicOperations.complement(a);
//		System.out.println(a2.toDot());
		
		CoverageDrivenInputGenerator gen = new CoverageDrivenInputGenerator();
		ArrayList<Map<String, Boolean>> negatives = gen.generateInputStrings(a, new HashSet<List<State>>(), false);
		
		assertEquals(67, negatives.size());
	}
	
	@Test
	public void testGenerateNegativeExample3() {
		String regex = "(.*((([C])|([A]))).*)";
		RegExp exp = new RegExp(regex);
		Automaton a = exp.toAutomaton();
//		System.out.println(a.toDot());
//		Automaton a2 = BasicOperations.complement(a);
//		System.out.println(a2.toDot());
		
		CoverageDrivenInputGenerator gen = new CoverageDrivenInputGenerator("contain(or(<C>,<A>))");
		ArrayList<Map<String, Boolean>> negatives = gen.generateInputStrings(a, new HashSet<List<State>>(), false);
		assertEquals(2, negatives.size());
		
		assertTrue(negatives.get(1).keySet().iterator().next().contains(",0"));
	}
}

package edu.harvard.seas.synthesis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Test;

public class ResnaxRunnerTest {
	
	@After
    public void cleanup() {
        ResnaxRunner.reset();
    }
	
	@Test
	public void test() {
		ResnaxRunner runner = ResnaxRunner.getInstance();
		List<String> l = null;
		try {
			Example[] examples = new Example[2];
			Example e1 = new Example();
			e1.input = "a";
			e1.output = true;
			e1.exact = new String[0];
			e1.unmatch = new String[0];
			e1.generalize = new String[0];
			examples[0] = e1;
			Example e2 = new Example();
			e2.input = "A";
			e2.output = false;
			e2.exact = new String[0];
			e2.unmatch = new String[0];
			e2.generalize = new String[0];
			examples[1] = e2;
			Regex[] regexes = new Regex[0];
			l = runner.run(examples, regexes);
		} finally {
			ResnaxRunner.reset();
		}
		
		assertEquals(5, l.size());
		assertTrue(l.contains("<low>"));
	}
	
	@Test
	public void testIncremental() {
		ResnaxRunner runner = ResnaxRunner.getInstance();
		List<String> l = null;
		try {
			Example[] examples = new Example[2];
			Example e1 = new Example();
			e1.input = "a";
			e1.output = true;
			e1.exact = new String[0];
			e1.unmatch = new String[0];
			e1.generalize = new String[0];
			examples[0] = e1;
			Example e2 = new Example();
			e2.input = "A";
			e2.output = false;
			e2.exact = new String[0];
			e2.unmatch = new String[0];
			e2.generalize = new String[0];
			examples[1] = e2;
			Regex[] regexes = new Regex[0];
			l = runner.run(examples, regexes);
			
			assertEquals(5, l.size());
			assertTrue(l.contains("<low>"));
			assertTrue(l.contains("<a>"));
			
			examples = new Example[3];
			examples[0] = e1;
			examples[1] = e2;
			Example e3 = new Example();
			e3.input = "b";
			e3.output = true;
			e3.exact = new String[0];
			e3.unmatch = new String[0];
			e3.generalize = new String[0];
			examples[2] = e3;
			l = runner.run(examples, regexes);
			
			assertEquals(5, l.size());
			assertTrue(l.contains("<low>"));
			assertFalse(l.contains("<a>"));
		} finally {
			ResnaxRunner.reset();
		}
	}
	
	@Test
	public void testTimeout() {
		ResnaxRunner runner = ResnaxRunner.getInstance();
		List<String> l = null;
		try {
			Example[] examples = new Example[2];
			Example e1 = new Example();
			e1.input = "a";
			e1.output = true;
			e1.exact = new String[0];
			e1.unmatch = new String[0];
			e1.generalize = new String[0];
			examples[0] = e1;
			Example e2 = new Example();
			e2.input = "A";
			e2.output = false;
			e2.exact = new String[0];
			e2.unmatch = new String[0];
			e2.generalize = new String[0];
			examples[1] = e2;
			Regex[] regexes = new Regex[0];
			l = runner.run(examples, regexes);
			
			assertEquals(5, l.size());
			assertTrue(l.contains("<low>"));
			assertTrue(l.contains("<a>"));
			
			// add a conflicting example
			examples = new Example[3];
			examples[0] = e1;
			examples[1] = e2;
			Example e3 = new Example();
			e3.input = "a";
			e3.output = false;
			e3.exact = new String[0];
			e3.unmatch = new String[0];
			e3.generalize = new String[0];
			examples[2] = e3;
			l = runner.run(examples, regexes);
			
			assertEquals(0, l.size());
			
			// remove the conflicting example
			examples = new Example[2];
			examples[0] = e1;
			examples[1] = e2;
			l = runner.run(examples, regexes);
			
			assertEquals(5, l.size());
			assertTrue(l.contains("<low>"));
			assertTrue(l.contains("<a>"));
		} finally {
			ResnaxRunner.reset();
		}
	}
}

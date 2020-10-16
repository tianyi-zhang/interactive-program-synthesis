package edu.harvard.seas.synthesis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

public class ServerCommandParserTest {
	
	@Test
	public void testPrintHelper() {
		String[] cmd = new String[1];
		cmd[0] = "-h";
		ServerCommandLineParser cmdParser = new ServerCommandLineParser();
		cmdParser.parse(cmd);
	}
	
	@Test
	public void testShortRequiredOptions() {
		String path = "/Users/tz/Research/Interactive Program Synthesis/interactive-program-synthesizer/back-end/lib";
		String[] cmd = new String[4];
		cmd[0] = "-s";
		cmd[1] = path;
		cmd[2] = "-n";
		cmd[3] = "10";
		ServerCommandLineParser cmdParser = new ServerCommandLineParser();
		assertTrue(cmdParser.parse(cmd));
		assertEquals(path, cmdParser.resnax_path);
		assertEquals(10, cmdParser.num_of_examples_per_cluster);
	}
	
	@Test
	@Ignore
	public void testWrongPythonVersion() {
		String path = "/Users/tz/Research/Interactive Program Synthesis/interactive-program-synthesizer/back-end/lib";
		String[] cmd = new String[2];
		cmd[0] = "-s";
		cmd[1] = path;
		ServerCommandLineParser cmdParser = new ServerCommandLineParser();
		assertFalse(cmdParser.parse(cmd));
	}
}

package edu.lu.uni.serval.config;

/**
 * Configuration of PAR.
 * 
 * @author anonymous
 *
 */
public class Configuration {

	public static String knownBugPositions = "BugPositions.txt";  

	// apr
	public static String linesFilePath = "";
	public static String proj = "";
	public static String id = "";
	
	public static String suspPositionsFilePath = "SuspiciousCodePositions/";
	public static String failedTestCasesFilePath = "FailedTestCases/";
	public static String faultLocalizationMetric = "Ochiai";
	public static String outputPath = "OUTPUT/";

	public static final String TEMP_FILES_PATH = ".temp/";
	public static final long SHELL_RUN_TIMEOUT = 10800L;

}

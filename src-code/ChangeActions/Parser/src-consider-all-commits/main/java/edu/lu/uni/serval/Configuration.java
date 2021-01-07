package edu.lu.uni.serval;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.lu.uni.serval.BugCommit.parser.MessageFile;

public class Configuration {
	
	public static List<MessageFile> msgFiles = new ArrayList<>();
	
	// apr : parameters.
	public static String D4J_REPO = "../d4j-repo/";
	public static String commitDB = "../d4j-info/projects/";
	public static String PROJ_BUG = "Chart";//"Chart";
	public static String PROJECT = "jfreechart";//"jfreechart";
	public static String ID = "19";
	public static boolean DELETE_PatchCommitsDir = true;
	public static boolean PRINT_ALLCOMMIT = true;
	public static String CCIPath;
	public static String CCIPurePath;
//	public static String USER_NAME = "apr";
	public static boolean singleBug = false;
	public static Map<String, Integer> numOfBugs = new HashMap<>();
	static{
		numOfBugs.put("Chart", 26);   //jfreechart
		numOfBugs.put("Closure", 133); //176  //TODO // closure-compiler
		numOfBugs.put("Lang", 65); // commons-lang
		numOfBugs.put("Math", 106); // commons-math
		numOfBugs.put("Time", 27); // joda-time
		numOfBugs.put("Mockito", 38); // mockito
	}
	public static int commitIdLength = 8;
	public static Map<String, Integer> commitNoMap = new HashMap<>();  // commitId number
	public static Map<String, Integer> commitExecutedNoMap = new HashMap<>();  // commitId number
	public static Date commitTime = new Date(0); // bug fix.   add 0 as a parameter
		
		
	public static final String BUG_REPORT_URL = "https://issues.apache.org/jira/browse/";
	public static String SUBJECTS_PATH = "../subjects/";  //apr
	private static final String OUTPUT_PATH = "../data/";
	public static final String BUG_REPORTS_PATH = OUTPUT_PATH + "BugReports/";
	public static final String PATCH_COMMITS_PATH = OUTPUT_PATH + "PatchCommits/";
	public static final String DIFFENTRY_SIZE_PATH = OUTPUT_PATH + "DiffentrySizes/";
	public static final String PARSE_RESULTS_PATH = OUTPUT_PATH + "ParseResults/";
	
	//apr
	public static final String AllCCI = OUTPUT_PATH + "AllCCI/";
	
	public static final long TIMEOUT_THRESHOLD = 108000L; //1800L; apr debug
	
	// apr: get current dir path
	// /home/apr/ALL_APR_TOOLS/Pre-PatchParse/PatchParser-D4J/Parser
	public static final String HOME = System.getProperty("user.dir") + "/";
	public static final String BUGS = OUTPUT_PATH + "bugs/";
	
	public static Map<String, Integer> numOfWorkers = new HashMap<>();
	public static Map<String, Integer> sizeThreshold = new HashMap<>();

	public static int msgFilesDelSize;
	
	static {
		numOfWorkers.put("commons-io", 1);
		numOfWorkers.put("commons-lang", 1);
		numOfWorkers.put("commons-math", 10);
		numOfWorkers.put("derby", 20);
		numOfWorkers.put("lucene-solr", 50);
		numOfWorkers.put("mahout", 10);
		
		sizeThreshold.put("buggy hunk", 8);
		sizeThreshold.put("fixed hunk", 10);
	}
}

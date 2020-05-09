package edu.lu.uni.serval;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import edu.lu.uni.serval.BugCommit.Distribution;
import edu.lu.uni.serval.BugCommit.PatchRelatedCommits;
import edu.lu.uni.serval.BugCommit.filter.MultipleThreadsPatchCommitsFilter;

// modified by apr
public class Main {

	public static void main(String[] args) throws IOException {
		// parse parameter.
		setParameters(args);
			
		// obtain project loc info
		System.out.println("======================================================================================");
		System.out.println("Statistics of project LOC.");
		System.out.println("======================================================================================");
		new Distribution().countLOC(Configuration.SUBJECTS_PATH);
		
		// obtain all fix commits
		System.out.println("\n\n\n======================================================================================");
		System.out.println("Collect bug-fix-related commits via bug-related keywords matching,\n"
				+ "and Fileter out test Java code changes.");
		System.out.println("======================================================================================");
		PatchRelatedCommits prc = new PatchRelatedCommits();
		prc.collectCommits(Configuration.SUBJECTS_PATH, Configuration.PATCH_COMMITS_PATH, Configuration.BUG_REPORTS_PATH);
		
		System.out.println("\n\n\n======================================================================================");
		System.out.println("Filter out non-Java code changes (e.g., Javadoc).");
		System.out.println("======================================================================================");
		new MultipleThreadsPatchCommitsFilter().filter(Configuration.SUBJECTS_PATH, Configuration.PATCH_COMMITS_PATH);
		
	}
	
	private static void setParameters(String[] args) {
//		public static String D4J_REPO = "/home/apr/ALL_APR_TOOLS/d4j-repo/";
//		public static String PROJ_BUG = "Chart";
//		public static String PROJECT = "jfreechart";
//		public static String ID = "2";
//		public static boolean DELETE_COMMITS = false;
        Option opt1 = new Option("d4j","D4J_REPO",true,"e.g., ../d4j-repo/");
        opt1.setRequired(false);
        Option opt2 = new Option("bugProj","PROJ_BUG",true,"e.g., Chart");
        opt2.setRequired(false);   
        Option opt3 = new Option("oriProj","PROJECT",true,"e.g., jfreechart");
        opt3.setRequired(false);
        Option opt4 = new Option("id","ID",true,"e.g., 2");
        opt4.setRequired(false);
        Option opt5 = new Option("singleBug","SINGLE",true,"e.g., false");
        opt4.setRequired(false);

        Options options = new Options();
        options.addOption(opt1);
        options.addOption(opt2);
        options.addOption(opt3);
        options.addOption(opt4);
        options.addOption(opt5);

        CommandLine cli = null;
        CommandLineParser cliParser = new DefaultParser();
        HelpFormatter helpFormatter = new HelpFormatter();

        try {
            cli = cliParser.parse(options, args);
        } catch (org.apache.commons.cli.ParseException e) {
            helpFormatter.printHelp(">>>>>> test cli options", options);
            e.printStackTrace();
        } 

        if (cli.hasOption("d4j")){
        	Configuration.D4J_REPO = cli.getOptionValue("d4j");
        }
        if(cli.hasOption("bugProj")){
        	Configuration.PROJ_BUG = cli.getOptionValue("bugProj");
        }
        if(cli.hasOption("oriProj")){
        	Configuration.PROJECT = cli.getOptionValue("oriProj");
        }
        if(cli.hasOption("id")){
        	Configuration.ID = cli.getOptionValue("id");
        }
        if(cli.hasOption("singleBug")){
        	String single = cli.getOptionValue("singleBug");
        	if (single.equalsIgnoreCase("false")){
        		Configuration.singleBug = false;
        	}else if(single.equalsIgnoreCase("true")){
        		Configuration.singleBug = true;
        	}else{
        		System.out.println("Wrong format of SINGLE parameter.");
        	}
        }
        
        System.out.format("oriProj: %s, bugProj: %s, id: %s", Configuration.PROJECT, Configuration.PROJ_BUG, Configuration.ID);
        
        // set project dir
    }

}

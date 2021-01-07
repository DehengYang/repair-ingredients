package edu.lu.uni.serval;

import java.io.IOException;
import java.text.ParseException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import edu.lu.uni.serval.BugCommit.Distribution;
import edu.lu.uni.serval.BugCommit.parser.MultipleThreadsPatchesParser1;
import edu.lu.uni.serval.utils.FileHelper;

// modified by apr
public class Main2 {

	public static void main(String[] args) throws IOException, ParseException  {
		// parse parameters
		setParameters(args);
		
		/*for(int id = 1; id <= 3; id++){
			Configuration.ID = String.format("%s", id);*/
			
			// reinit
//			if (Configuration.msgFiles.size()>0){
//				for (int i = 0; i < Configuration.msgFilesDelSize; i++){
//					Configuration.msgFiles.remove(0);
//				}
//				System.out.format("4 Configuration.msgFiles size: %s\n", Configuration.msgFiles.size());
//			}
		
		// ===================
		System.out.format("\n\n\n=================== Current proj_id: %s_%s ================\n\n\n", Configuration.PROJ_BUG, Configuration.ID);
//		System.out.format("0 Configuration.msgFiles size: %s\n", Configuration.msgFiles.size());
		Configuration.commitNoMap.clear();
		Configuration.commitExecutedNoMap.clear();

		// delete first, For example: ../data/ParseResults/CCI/Chart/20
		Configuration.CCIPath = Configuration.PARSE_RESULTS_PATH + "CCI/" + Configuration.PROJ_BUG + "/" + Configuration.ID + "/";
		Configuration.CCIPurePath = Configuration.PARSE_RESULTS_PATH + "CCI-pure/" + Configuration.PROJ_BUG + "/" + Configuration.ID + "/";
		FileHelper.deleteDirectory(Configuration.CCIPath);
		FileHelper.deleteDirectory(Configuration.CCIPurePath);

		//			System.out.println("\n\n\n======================================================================================");
		//			System.out.println("Statistics of diff hunk sizes of code changes.");
		//			System.out.println("======================================================================================");
		// set the threshold of patch hunks.
		//			new Distribution().statistics(Configuration.PATCH_COMMITS_PATH, Configuration.DIFFENTRY_SIZE_PATH);

		//			System.out.println("\n\n\n======================================================================================");
		//			System.out.println("Parse code changes of patches.");
		//			System.out.println("======================================================================================");
		new MultipleThreadsPatchesParser1().parse(Configuration.PATCH_COMMITS_PATH, Configuration.PARSE_RESULTS_PATH);
		//			new MultipleThreadsPatchesParser2().parse(Configuration.PATCH_COMMITS_PATH, Configuration.PARSE_RESULTS_PATH);
//		}
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

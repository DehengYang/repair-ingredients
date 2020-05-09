package edu.lu.uni.serval.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import edu.lu.uni.serval.bug.fixer.AbstractFixer;
import edu.lu.uni.serval.bug.fixer.ParFixer;
import edu.lu.uni.serval.bug.fixer.ParFixer2;
import edu.lu.uni.serval.config.Configuration;
import edu.lu.uni.serval.utils.SuspiciousPosition;

/**
 * Fix bugs with Fault Localization results.
 * 
 * @author anonymous
 *
 */
public class Main {
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		// parse parameter.
		Map<String, String> parameters = setParameters(args);
		String proj = parameters.get("proj");
		String id = parameters.get("id");
		String projDir = parameters.get("projDir");
		String searchDir = parameters.get("searchDir");
		
		String buggyProjectsPath = projDir;// "./Defecst4JBugs/Defects4JData/"
		String projectName = proj + "_" + id; // "Lang_24"
		Configuration.linesFilePath = searchDir; // /home/apr/eclipse-projs/codesearch/search-log/chart/3
		Configuration.proj = proj;
		Configuration.id = id;
		System.out.println(projectName);

		// not used now.
		String defects4jPath = ""; // "~/environment/defects4j"  or "~/environment/defects4j/framework/bin/"
		Configuration.faultLocalizationMetric = "Ochiai"; // Ochiai
		Configuration.outputPath += "FL/";
		
		// start match
		fixBug(buggyProjectsPath, defects4jPath, projectName);
	}

	public static void fixBug(String buggyProjectsPath, String defects4jPath, String buggyProjectName) throws FileNotFoundException, IOException {
		String suspiciousFileStr = Configuration.suspPositionsFilePath;
		
		String dataType = "PAR";
		String[] elements = buggyProjectName.split("_");
		String projectName = elements[0];
		int bugId;
		try {
			bugId = Integer.valueOf(elements[1]);
		} catch (NumberFormatException e) {
			System.err.println("Please input correct buggy project ID, such as \"Chart_1\".");
			return;
		}
		
		AbstractFixer fixer = new ParFixer2(buggyProjectsPath, projectName, bugId, defects4jPath);
		fixer.metric = Configuration.faultLocalizationMetric;
		fixer.dataType = dataType;
		fixer.suspCodePosFile = new File(suspiciousFileStr);
		// FIXME
//		if (Integer.MAX_VALUE == fixer.minErrorTest) {
//			System.out.println("Failed to defects4j compile bug " + buggyProjectName);
//			return;
//		}
		
		//FIXME
//		fixer.fixProcess();
		fixer.matchLines();
		
		int fixedStatus = fixer.fixedStatus;
		switch (fixedStatus) {
		case 0:
//			System.out.println("Failed to fix bug " + buggyProjectName);
			System.out.println("Finish code matching for bug : " + buggyProjectName);
			break;
		case 1:
//			System.out.println("Succeeded to fix bug " + buggyProjectName);
			break;
		case 2:
//			System.out.println("Partial succeeded to fix bug " + buggyProjectName);
			break;
		}
	}

	/*
	 * receive parameters
	 */
	private static Map<String, String> setParameters(String[] args) {
//		repoFixed + proj + "/", 
//		projId,
//		"../CodeSearch/search-log/" + proj.toLowerCase() + "/" + id};
		
		Map<String, String> parameters = new HashMap<>();
		
        Option opt1 = new Option("proj","project_name",true,"e.g., Chart");
        opt1.setRequired(true);
        Option opt2 = new Option("id","id",true,"e.g., 3");
        opt2.setRequired(true);   
        Option opt3 = new Option("projDir","target_path",true,"e.g., ../CodeSearch/search-log/chart/3");
        opt3.setRequired(true);
        Option opt4 = new Option("searchDir","search_result_dir",true,"e.g., ./CodeSearch/d4j-repo/fixed_bugs_dir/Chart/");
        opt3.setRequired(true);

        Options options = new Options();
        options.addOption(opt1);
        options.addOption(opt2);
        options.addOption(opt3);
        options.addOption(opt4);

        CommandLine cli = null;
        CommandLineParser cliParser = new DefaultParser();
        HelpFormatter helpFormatter = new HelpFormatter();

        try {
            cli = cliParser.parse(options, args);
        } catch (org.apache.commons.cli.ParseException e) {
            helpFormatter.printHelp(">>>>>> test cli options", options);
            e.printStackTrace();
        } 

        if (cli.hasOption("proj")){
        	parameters.put("proj", cli.getOptionValue("proj"));
        }
        if(cli.hasOption("id")){
        	parameters.put("id", cli.getOptionValue("id"));
        }
        if(cli.hasOption("projDir")){
        	parameters.put("projDir", cli.getOptionValue("projDir"));
        }
        if(cli.hasOption("searchDir")){
        	parameters.put("searchDir", cli.getOptionValue("searchDir"));
        }
		return parameters;
    }
}

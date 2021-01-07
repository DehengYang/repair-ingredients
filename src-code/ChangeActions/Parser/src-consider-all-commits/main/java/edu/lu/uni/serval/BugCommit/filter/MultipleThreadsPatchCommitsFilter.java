package edu.lu.uni.serval.BugCommit.filter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import edu.lu.uni.serval.Configuration;
import edu.lu.uni.serval.BugCommit.parser.MessageFile;
import edu.lu.uni.serval.BugCommit.parser.WorkMessage;

/**
 * Filter out the commits with non-Java code changes.
 * 
 * @author anonymous
 *
 */
public class MultipleThreadsPatchCommitsFilter {
	
//	private static Logger log = LoggerFactory.getLogger(MultipleThreadsPatchCommitsFilter.class);

	/*
	 * subjectsPath -> Configuration.SUBJECTS_PATH -> ../subjects/
	 * outputPath -> Configuration.PATCH_COMMITS_PATH -> ../data/PatchCommits/
	 */
	@SuppressWarnings("deprecation")
	public void filter(String subjectsPath, String outputPath) {
		File[] projects = new File(subjectsPath).listFiles();
		for (File project : projects) {
			if (project.isDirectory()  
					&& project.getName().equals(Configuration.PROJECT)) {  // fix here
				String projectName = project.getName();
				final List<MessageFile> msgFiles = readMessageFiles(projectName, outputPath);
				ActorSystem system = null;
				ActorRef parsingActor = null;
				// it's okay to set this as 50, or 16, or 20 ...
				int numberOfWorkers = 50; 
				
				// not used
//				if( ! Configuration.numOfWorkers.keySet().contains(projectName)){
//					numberOfWorkers = 1;
//				}else{
//					numberOfWorkers = Configuration.numOfWorkers.get(projectName);
//				}
				
				final WorkMessage msg = new WorkMessage(0, msgFiles);
				try {
//					log.info("Parsing begins...");
					System.out.println("Parsing begins...");
					system = ActorSystem.create("Parsing-Patch-System-" + projectName);
					parsingActor = system.actorOf(ParsePatchActor.props(numberOfWorkers, outputPath, projectName), "patch-parse-actor-" + projectName);
					parsingActor.tell(msg, ActorRef.noSender());
				} catch (Exception e) {
					e.printStackTrace();
					system.shutdown();
				}
			}
		}
	}

	/*
	 * read all commits from ../data/PatchCommits/Keywords/ directory
	 */
	private List<MessageFile> readMessageFiles(String projectName, String path) {
		List<MessageFile> msgFiles = new ArrayList<>();
		String keywordPatchsFile = path + "Keywords/" + projectName;
		// bug report-linked bug not used
//		String linkedPatchsFile = path + "Linked/" + projectName;

		List<String> commitIds = new ArrayList<>();
		
		msgFiles.addAll(getMessageFiles(keywordPatchsFile, "Keywords", commitIds));
		// apr comment
//		msgFiles.addAll(getMessageFiles(linkedPatchsFile, "Linked", commitIds));

//		System.out.println("Identified patch-related commits: " + commitIds.size());
		return msgFiles;
	}

	private List<MessageFile> getMessageFiles(String filePath, String type, List<String> commitIds) {
		List<MessageFile> msgFiles = new ArrayList<>();
		File revFilesPath = new File(filePath + "/revFiles/");
		File[] revFiles = revFilesPath.listFiles();   // project folders
		
		for (File revFile : revFiles) {
			String fileName = revFile.getName();
			if (fileName.endsWith(".java")) {
				File prevFile = new File(filePath + "/prevFiles/prev_" + fileName);// previous file
				fileName = fileName.replace(".java", ".txt");
				File diffentryFile = new File(filePath + "/DiffEntries/" + fileName); // DiffEntry file
				MessageFile msgFile = new MessageFile(revFile, prevFile, diffentryFile);
				msgFiles.add(msgFile);
				
				String commitId = fileName.substring(0, Configuration.commitIdLength);
				if (!commitIds.contains(commitId)) commitIds.add(commitId);
			}
		}
		
		return msgFiles;
	}
	
}

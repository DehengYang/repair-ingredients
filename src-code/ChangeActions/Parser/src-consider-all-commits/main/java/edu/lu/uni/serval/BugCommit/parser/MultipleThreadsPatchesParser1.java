package edu.lu.uni.serval.BugCommit.parser;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import edu.lu.uni.serval.Configuration;
import edu.lu.uni.serval.BugCommit.BugDiff;
import edu.lu.uni.serval.utils.FileHelper;

/**
 * Parse all patches together.
 * 
 * @author anonymous
 *
 */
public class MultipleThreadsPatchesParser1 {
	
	private static Logger log = LoggerFactory.getLogger(MultipleThreadsPatchesParser1.class);

	@SuppressWarnings("deprecation")
	public void parse(String patchPath, String outputPath) throws IOException, ParseException {
		int bugHunkSize = Configuration.sizeThreshold.get("buggy hunk");
		int fixHunkSize = Configuration.sizeThreshold.get("fixed hunk");
		// add e.g., Chart-1 buggy file & fixed file
		List<MessageFile> msgFiles1 = readMessageFiles2();
		
		// comment: "Linked" not used.
//		List<MessageFile> msgFiles = readMessageFiles(patchPath, "Linked");
//		msgFiles.addAll(readMessageFiles(patchPath, "Keywords"));
		List<MessageFile> msgFiles2 = readMessageFiles(patchPath, "Keywords");
		
		System.out.format("1 Configuration.msgFiles size: %s\n", Configuration.msgFiles.size());
		Configuration.msgFiles.addAll(msgFiles2);
		System.out.format("2 Configuration.msgFiles size: %s\n", Configuration.msgFiles.size());
		Configuration.msgFiles.addAll(0, msgFiles1);
		Configuration.msgFilesDelSize = msgFiles1.size();
		System.out.format("3 Configuration.msgFiles size: %s\n", Configuration.msgFiles.size());
		
		ActorSystem system = null;
		ActorRef parsingActor = null;
		
		int numberOfWorkers = 50;
		setTime();
		
		final WorkMessage msg = new WorkMessage(0, Configuration.msgFiles);
		try {
//			log.info("Parsing begins...");
			system = ActorSystem.create("Parsing-Patches-System");
			parsingActor = system.actorOf(ParsePatchActor.props(numberOfWorkers, outputPath, bugHunkSize, fixHunkSize), "patch-parser-actor");
			parsingActor.tell(msg, ActorRef.noSender());
			
//			System.out.format("======================================TEST HERE======================================\n\n\n");
//			if (Configuration.msgFiles.size()>0){
//				for (int i = 0; i < Configuration.msgFilesDelSize; i++){
//					Configuration.msgFiles.remove(0);
//				}
//				System.out.format("4 Configuration.msgFiles size: %s\n", Configuration.msgFiles.size());
//			}
		} catch (Exception e) {
			system.shutdown();
			e.printStackTrace();
		}
	}

	// apr : read Chart buggy and fixed files
	// ../data/PatchCommits/Keywords/jfreechart   Chart/1/
	private List<MessageFile> readMessageFiles2() {
		List<MessageFile> msgFiles = new ArrayList<>();
		File[] projects = new File(Configuration.BUGS).listFiles();
		for (File project : projects) {
			if (project.isDirectory()) { // Chart
				String projDir = Configuration.BUGS + project.getName() + "/";
				
				// find all ids
				File[] idsDir = new File(projDir).listFiles();
				for(File idDir : idsDir){
					if(idDir.isDirectory() && idDir.getName().equals(Configuration.ID)){ // fix this: only one at a time
						String projIdDir = projDir + idDir.getName() + "/";
						File revFilesPath = new File(projIdDir);
						File[] revFiles = revFilesPath.listFiles();   // project folders
						for (File revFile : revFiles) {
							if (revFile.getName().startsWith("fixed-")) {
								String fileName = revFile.getName();
								File prevFile = new File(projIdDir + fileName.replace("fixed-", "buggy-"));// previous file
//								File diffentryFile = new File(projIdDir + "bugDiff.txt"); // DiffEntry file
								MessageFile msgFile = new MessageFile(revFile, prevFile);
								msgFile.setProj(project.getName());
								msgFile.setId(idDir.getName());
								msgFiles.add(msgFile);
							}
						}
					}
				}
				
			}
		}
		return msgFiles;
	}
	
	/*
	 * read time of bug (e.g., Chart-1) from commitTimePath
	 */
	private void setTime() throws ParseException {
		// clear CCI file.
		//FileHelper.deleteFile(Configuration.BUGS + Configuration.PROJ_BUG + "/" + Configuration.ID +"/changeAction-info");
		FileHelper.deleteFile(Configuration.BUGS + Configuration.PROJ_BUG + "/" + Configuration.ID +"/changeAction.txt");
		
		// get Proj_id time
		String commitTimePath = Configuration.BUGS + Configuration.PROJ_BUG + "/"
				+ Configuration.ID + "/"; //CommitId-
		File commitTimePathFile = new File(commitTimePath);
		File[] files =  commitTimePathFile.listFiles();
		List<String> timeList =  new ArrayList<>();
		for(File file : files){
			if(file.getName().startsWith("CommitId-")){ // records time.
				String[] timeLines = FileHelper.readFile(file).trim().split("\n");
				for(String timeLine : timeLines){
					if(!timeList.contains(timeLine)){
						timeList.add(timeLine);
						timeLine = timeLine.split("\\+")[0].trim();
						SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						Configuration.commitTime = format.parse(timeLine);
					}
				}
				
				if(timeList.size() > 1){
					FileHelper.outputToFile(commitTimePath + "timeError" + file.getName(), "", false);
				}
			}
		}
		
	}

	private List<MessageFile> readMessageFiles(String path, String dataType) throws IOException, ParseException {
		List<MessageFile> msgFiles = new ArrayList<>();
		File[] projects = new File(path + dataType).listFiles();
		int cnt = 0;
		for (File project : projects) {
			// apr change.
			if (project.isDirectory() && ! project.getName().endsWith("_allCommits")) {
				String projectPath = project.getPath();
				File revFilesPath = new File(projectPath + "/revFiles/");
				File[] revFiles = revFilesPath.listFiles();   // project folders
				
				for (File revFile : revFiles) {
					if (revFile.getName().endsWith(".java")) {
						String fileName = revFile.getName();
						File prevFile = new File(projectPath + "/prevFiles/prev_" + fileName);// previous file
						fileName = fileName.replace(".java", ".txt");
						File diffentryFile = new File(projectPath + "/DiffEntries/" + fileName); // DiffEntry file
						MessageFile msgFile = new MessageFile(revFile, prevFile, diffentryFile);
						
						//apr
						String commitId = revFile.getName().substring(0, Configuration.commitIdLength);
						Date commitTime = BugDiff.getCommitTime(commitId);
						msgFile.setCommitTime(commitTime);
						
//						if(Configuration.commitNoMap.containsKey(commitId)){
//							int number = Configuration.commitNoMap.get(commitId);
//							Configuration.commitNoMap.put(commitId, number + 1);
//						}else{
//							Configuration.commitNoMap.put(commitId, 1);
////							Configuration.commitExecutedNoMap.put(commitId, 0);
//						}
						
						msgFiles.add(msgFile);
						
//						cnt ++;
//						if (cnt>1000){
//							break; //debug
//						}
					}
				}
			}
		}
		return msgFiles;
	}
}

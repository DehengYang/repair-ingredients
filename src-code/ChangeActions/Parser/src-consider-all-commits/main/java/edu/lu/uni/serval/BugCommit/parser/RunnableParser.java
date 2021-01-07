package edu.lu.uni.serval.BugCommit.parser;

import java.io.File;

import edu.lu.uni.serval.Configuration;

public class RunnableParser implements Runnable {

	private File prevFile;
	private File revFile;
	private File diffentryFile;
	private PatchParser parser;
	private int bugHunkSize;
	private int fixHunkSize;
	
	private MessageFile msgFile;
	private File cciFile;
	
	// apr: not null -> proj_id commit
	private String idFlag;
	
	public RunnableParser(File cciFile, MessageFile msgFile, File prevFile, File revFile, File diffentryFile, PatchParser parser, int bugHunkSize, int fixHunkSize, String idFlag) {
		this.prevFile = prevFile;
		this.revFile = revFile;
		this.diffentryFile = diffentryFile;
		this.parser = parser;
		this.bugHunkSize = bugHunkSize;
		this.fixHunkSize = fixHunkSize;
		this.idFlag = idFlag;
		
		this.msgFile = msgFile;
		this.cciFile = cciFile;
	}

	@Override
	public void run() {
//		if (msgFile.getPatches().size() == 0){
		
		
		// is history commit
		if (msgFile.getId() == null 
							&& msgFile.getCommitTime().compareTo(Configuration.commitTime) < 0 ){
			if (!cciFile.exists()){ // should do 
				long startT = System.currentTimeMillis();
				parser.parsePatches(prevFile, revFile, diffentryFile, bugHunkSize, fixHunkSize, idFlag);
//				System.out.format("[%ss size: %s|%s] cciFile: diff -Naur %s %s\n", (System.currentTimeMillis() - startT)/1000.0, parser.getPatches().size(), parser.getActionSets().size(), prevFile, revFile);
				
				
				
//				if (parser.getPatches().size()>0){
//					System.out.format("");
//				}
//				msgFile.getPatches().putAll(parser.getPatches());
//				System.out.format("msgFile.getPatches() size:%s (%s) %s \n", msgFile.getPatches().size(), parser.getActionSets().size(), msgFile.getPrevFile());
			}
//			else{
//				System.out.format("[Already exist] cciFile: %s\n", cciFile.getName());
////				System.out.format("[Already exist] msgFile.getPatches() size:%s %s\n", msgFile.getPatches().size(), msgFile.getPrevFile());
//			}
		}
		
		// is bug fix e.g., Closure-1
		if (msgFile.getId() != null){ 
			long startT = System.currentTimeMillis();
			parser.parsePatches(prevFile, revFile, diffentryFile, bugHunkSize, fixHunkSize, idFlag);
			System.out.format("[%ss size: %s|%s] cciFile: diff -Naur %s %s\n", (System.currentTimeMillis() - startT)/1000.0, parser.getPatches().size(), parser.getActionSets().size(), prevFile, revFile);
		}
		
	}
}

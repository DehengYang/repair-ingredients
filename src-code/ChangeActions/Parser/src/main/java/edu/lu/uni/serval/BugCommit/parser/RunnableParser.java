package edu.lu.uni.serval.BugCommit.parser;

import java.io.File;

public class RunnableParser implements Runnable {

	private File prevFile;
	private File revFile;
	private File diffentryFile;
	private PatchParser parser;
	private int bugHunkSize;
	private int fixHunkSize;
	
	// apr: not null -> proj_id commit
	private String idFlag;
	
	public RunnableParser(File prevFile, File revFile, File diffentryFile, PatchParser parser, int bugHunkSize, int fixHunkSize, String idFlag) {
		this.prevFile = prevFile;
		this.revFile = revFile;
		this.diffentryFile = diffentryFile;
		this.parser = parser;
		this.bugHunkSize = bugHunkSize;
		this.fixHunkSize = fixHunkSize;
		this.idFlag = idFlag;
	}

	@Override
	public void run() {
		parser.parsePatches(prevFile, revFile, diffentryFile, bugHunkSize, fixHunkSize, idFlag);
	}
}

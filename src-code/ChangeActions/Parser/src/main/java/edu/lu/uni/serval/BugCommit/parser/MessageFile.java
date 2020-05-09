package edu.lu.uni.serval.BugCommit.parser;

import java.io.File;
import java.util.Date;

public class MessageFile {
	
	private File revFile;
	private File prevFile;
	private File diffEntryFile;
	private File positionFile;
	//apr
	private String proj = null;
	private String id = null;
	private Date commitTime = new Date();
	
	public MessageFile(File revFile, File prevFile) {
		super();
		this.revFile = revFile;
		this.prevFile = prevFile;
	}
	
	public MessageFile(File revFile, File prevFile, File diffEntryFile) {
		super();
		this.revFile = revFile;
		this.prevFile = prevFile;
		this.diffEntryFile = diffEntryFile;
	}

	public File getRevFile() {
		return revFile;
	}
	
	public File getPrevFile() {
		return prevFile;
	}
	
	public File getDiffEntryFile() {
		return diffEntryFile;
	}

	public File getPositionFile() {
		return positionFile;
	}

	public void setPositionFile(File positionFile) {
		this.positionFile = positionFile;
	}

	public String getProj() {
		return proj;
	}

	public void setProj(String proj) {
		this.proj = proj;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Date getCommitTime() {
		return commitTime;
	}

	public void setCommitTime(Date commitTime) {
		this.commitTime = commitTime;
	}
	
}

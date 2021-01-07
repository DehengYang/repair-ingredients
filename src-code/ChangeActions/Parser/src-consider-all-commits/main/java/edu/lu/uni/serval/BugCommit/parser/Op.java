package edu.lu.uni.serval.BugCommit.parser;

import java.util.ArrayList;
import java.util.List;

public class Op {
	private int level; // parent, child
	private String op; // UPD, MOV ...
	private String stmtType = null;
	
	private String opName = null;
	private String parentOpName = null;
	private List<String> childOpNameList = new ArrayList<>();
	public int getLevel() {
		return level;
	}
	public void setLevel(int level) {
		this.level = level;
	}
	public String getOp() {
		return op;
	}
	public void setOp(String op) {
		this.op = op;
	}
	public String getStmtType() {
		return stmtType;
	}
	public void setStmtType(String stmtType) {
		this.stmtType = stmtType;
	}
	public String getOpName() {
		return opName;
	}
	public void setOpName(String opName) {
		this.opName = opName;
	}
	public String getParentOpName() {
		return parentOpName;
	}
	public void setParentOpName(String parentOpName) {
		this.parentOpName = parentOpName;
	}
	
	public String toString(){
		String blank = "";
		int tmpLevel = level;
		while (tmpLevel - 1 > 0){
			blank += "   ";
			tmpLevel --; 
		}
		
		String childOpName = "";
		for (String name : childOpNameList){
			childOpName = childOpName + " " + name;
		}
		childOpName = childOpName.trim();
		
		return opName + ":" + blank +"(" + op + ", " 
				+ stmtType + ", "
				+ parentOpName + ", "
				+ childOpName + ")\n";
	}
	public List<String> getChildOpNameList() {
		return childOpNameList;
	}
	public void setChildOpNameList(List<String> childOpNameList) {
		this.childOpNameList = childOpNameList;
	} 
	
	public boolean equals(Object obj) {
        return (this.toString().equals(obj.toString()));
    }
}

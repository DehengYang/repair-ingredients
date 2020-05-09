package edu.lu.uni.serval;

import java.io.IOException;

import junit.framework.TestCase;

public class MainTest extends TestCase{
	public void testProjId(String bugProj, String oriProj, String id, String singleBug) throws IOException{
		String[] args2 = new String[] {
    			"-d4j", "../d4j-repo/",
    			"-bugProj", bugProj,
    			"-oriProj", oriProj,
    			"-id", id,
    			"-singleBug", singleBug
    			};
    	Main.main(args2);
	}
	
	public void testD4JBug(){
		try {
			testProjId("Chart", "jfreechart", "3", "true");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}

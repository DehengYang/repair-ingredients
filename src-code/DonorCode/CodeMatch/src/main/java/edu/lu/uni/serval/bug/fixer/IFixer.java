package edu.lu.uni.serval.bug.fixer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import edu.lu.uni.serval.bug.fixer.AbstractFixer.SuspCodeNode;
import edu.lu.uni.serval.dataprepare.DataPreparer;
import edu.lu.uni.serval.utils.SuspiciousPosition;

/**
 * Fixer Interface.
 * 
 * @author anonymous
 *
 */
public interface IFixer {

	public List<SuspiciousPosition> readSuspiciousCodeFromFile(String metric, String buggyProject, DataPreparer dp);
	
	public SuspCodeNode parseSuspiciousCode(SuspiciousPosition suspiciousCode);

	public void fixProcess();
	
	//FIXME
	public void matchLines() throws FileNotFoundException, IOException;
	
}

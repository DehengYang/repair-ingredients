package edu.lu.uni.serval.bug.fixer;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.lu.uni.serval.config.Configuration;
import edu.lu.uni.serval.utils.FileHelper;
import edu.lu.uni.serval.utils.SuspiciousPosition;

/**
 * Automated Program Repair Tool: PAR.
 * 
 * Fix bugs with PAR when the bug positions at the file level is provided.
 * 
 * @author anonymous
 *
 */
public class File_ParFixer extends ParFixer {
	
	private static Logger log = LoggerFactory.getLogger(File_ParFixer.class);
	
	public File_ParFixer(String path, String projectName, int bugId, String defects4jPath) {
		super(path, projectName, bugId, defects4jPath);
	}
	
	public File_ParFixer(String path, String metric, String projectName, int bugId, String defects4jPath) {
		super(path, metric, projectName, bugId, defects4jPath);
	}

	@Override
	public void fixProcess() {
		// Read paths of the buggy project.
		if (!dp.validPaths) return;
		
		// Read suspicious positions.
		List<String> buggyFileList = readKnownFileLevelBugPositions();
		List<SuspiciousPosition> suspiciousCodeList = readSuspiciousCodeFromFile(metric, buggyProject, dp);
		if (suspiciousCodeList == null) return;
		
		List<SuspCodeNode> triedSuspNode = new ArrayList<>();
		log.info("=======FILE_PARFIXER: Start to fix suspicious code======");
		for (SuspiciousPosition suspiciousCode : suspiciousCodeList) {
			
			if (!buggyFileList.contains(suspiciousCode.classPath)) continue;
			
			SuspCodeNode scn = parseSuspiciousCode(suspiciousCode);
			if (scn == null) continue;

//			log.debug(scn.suspCodeStr);
			if (triedSuspNode.contains(scn)) continue;
			triedSuspNode.add(scn);
	        // Match fix templates for this suspicious code with its context information.
	        fixWithMatchedFixTemplates(scn);
	        
			if (minErrorTest == 0) break;
        }
		log.info("=======FILE_PARFIXER: Finish off fixing======");
		
		FileHelper.deleteDirectory(Configuration.TEMP_FILES_PATH + this.dataType + "/" + this.buggyProject);
	}

	private List<String> readKnownFileLevelBugPositions() {
		List<String> buggyFileList = new ArrayList<>();
		
		String[] posArray = FileHelper.readFile(Configuration.knownBugPositions).split("\n");
		Boolean isBuggyProject = null;
		for (String pos : posArray) {
			if (isBuggyProject == null || isBuggyProject) {
				if (pos.startsWith(this.buggyProject + "@")) {
					isBuggyProject = true;
					
					String[] elements = pos.split("@");
	            	String classPath = elements[1];
	            	String shortSrcPath = dp.srcPath.substring(dp.srcPath.indexOf(this.buggyProject) + this.buggyProject.length() + 1);
	            	classPath = classPath.substring(shortSrcPath.length(), classPath.length() - 5).replace("/", ".");

	            	if (!buggyFileList.contains(classPath)) {
	            		buggyFileList.add(classPath);
	            	}
				} else if (isBuggyProject!= null && isBuggyProject) isBuggyProject = false;
			} else if (!isBuggyProject) break;
		}
		return buggyFileList;
	}

}

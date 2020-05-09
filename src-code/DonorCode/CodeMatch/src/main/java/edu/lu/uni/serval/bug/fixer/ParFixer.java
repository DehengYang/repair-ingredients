package edu.lu.uni.serval.bug.fixer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.lu.uni.serval.config.Configuration;
import edu.lu.uni.serval.entity.Pair;
import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.par.templates.FixTemplate;
import edu.lu.uni.serval.par.templates.fix.ClassCastChecker;
import edu.lu.uni.serval.par.templates.fix.CollectionSizeChecker;
import edu.lu.uni.serval.par.templates.fix.ExpressionAdder;
import edu.lu.uni.serval.par.templates.fix.ExpressionRemover;
import edu.lu.uni.serval.par.templates.fix.ExpressionReplacer;
import edu.lu.uni.serval.par.templates.fix.MethodReplacer;
import edu.lu.uni.serval.par.templates.fix.NullPointerChecker;
import edu.lu.uni.serval.par.templates.fix.ParameterAdder;
import edu.lu.uni.serval.par.templates.fix.ParameterRemover;
import edu.lu.uni.serval.par.templates.fix.ParameterReplacer;
import edu.lu.uni.serval.par.templates.fix.RangeChecker;
import edu.lu.uni.serval.tbar.context.ContextReader;
import edu.lu.uni.serval.utils.Checker;
import edu.lu.uni.serval.utils.FileHelper;
import edu.lu.uni.serval.utils.ShellUtils;
import edu.lu.uni.serval.utils.SuspiciousPosition;
import edu.lu.uni.serval.utils.SuspiciousCodeParser;

/**
 * Automated Program Repair Tool: PAR.
 * 
 * All fix templates are introduced in paper "Automatic patch generation learned from human-written patches".
 * https://dl.acm.org/citation.cfm?id=2486893
 * 
 * @author anonymous
 *
 */
public class ParFixer extends AbstractFixer {
	
	private static Logger log = LoggerFactory.getLogger(ParFixer.class);
	
	// apr
	private static String logPath = null;
	private int startLineNo = 0;
	private int endLineNo = 0;
	private int matchedStartLineNo = 0;
	private int matchedEndLineNo = 0;
	private String fullPath = null;
	private String dpPath = null;
	private String projId = null;
	private String matchedFlag = null;
	private String matchedLogPath = null;
	private List<String> fixed_code = new ArrayList<>();
	private List<String> similar_code = new ArrayList<>();
	private String classPath = "null";
	
	// code improvement: add HashMap for patch_match
	private Map<Integer, String> patchLinesMap = new HashMap<Integer, String>(); 
	
	private int patchStartLineNo = 0;
	private int patchEndLineNo = 0;
	// bug fix: I forget to set patchClassPath
	private String patchClassPath = "";
	// code improve
	private String patchMethod = null;
	private String matchMethod = null;
	
	// code improve: only save the same fixCode once. (do not repeat)
	private int fixCodeSaveFlag = 0;
	
	// code improve:
	private File bkupFolder;
	private int parsedLinesCnt = 0;
	
	public ParFixer(String path, String projectName, int bugId, String defects4jPath) {
		super(path, projectName, bugId, defects4jPath);
	}
	
	public ParFixer(String path, String metric, String projectName, int bugId, String defects4jPath) {
		super(path, metric, projectName, bugId, defects4jPath);
	}
	
	public void initAll(){
		patchStartLineNo = 0;
		patchEndLineNo = 0;
		
		patchClassPath = "";
		patchMethod = null;
		matchMethod = null;
		
		fixCodeSaveFlag = 0;
		
		// bug fix: clear again!
		patchLinesMap.clear();
		// code improve: output to a matched file for each fix code.
		matchedLogPath = null;
		// bug fix: logPath should not be null. When fixing, try to change matchLinesFromEachFile method
		logPath = null;
	}
	
	public void matchLines() throws IOException{
		// init
		init();
				
		// get file path
		File dir = new File(Configuration.linesFilePath);
		String filePath = null;
		if (dir.exists()){
			File[] files = dir.listFiles(); 
			for (File file:files){
				String fileName = file.getName();
				// bug fix: exclude xxx.~ file. 
				if(fileName.substring(0,6).equals("lines_") 
					&& !fileName.substring(fileName.length()-1, fileName.length()).equals("~")){
					filePath = file.getAbsolutePath();
					
					// get patch lines
					//e.g., lines_org.joda.time.Partial:218-218_fixed.log
					String[] linesTmp = filePath.split(":")[1].split("_")[0].split("-");
					int startLine = Integer.parseInt(linesTmp[0]);
					int endLine = Integer.parseInt(linesTmp[1]);
					
					// init all relevant
					initAll();
					for (int line = startLine; line <= endLine; line ++){
						patchLinesMap.put(line, "");
					}
					
					matchLinesFromEachFile(filePath, startLine, endLine);
				}
			}
		}
		if (filePath == null){
			System.err.println("filePath not found in : " + Configuration.linesFilePath);
			return;
		}
	}
	
	
	public void matchLinesFromEachFile(String filePath, int startLine, int endLine) throws IOException{
		// read lines
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(filePath)));
		SuspiciousPosition sc = new SuspiciousPosition();
		
		for (String line = br.readLine(); line != null; line = br.readLine()) {
			// /home/apr/d4j/fixed_bugs_dir/Chart/Chart_3/source/org/jfree/data/time/TimeSeries
			line = line.trim();
			String line1 = line.split(":")[1];
			String[] lines = line1.split("\\.");
			String fullPath = lines[0];
			
			// set full path. 
			// e.g., /home/apr/d4j/fixed_bugs_dir/Chart/Chart_3/source/org/jfree/data/time/TimeSeries
			this.fullPath = fullPath;
			
			// java-1057-1058
			String lineRanges = line.split(":")[1].split("\\.")[1];
			
			this.startLineNo = Integer.parseInt(lineRanges.split("-")[1]);
			this.endLineNo = Integer.parseInt(lineRanges.split("-")[2]);
			
			// source java src
			int index;
			if (fullPath.contains("/source/")){
				index = fullPath.indexOf("/source/") + 8;
//				classPath = fullPath.substring(index + 8);
			}else if(fullPath.contains("/java/")){
				index = fullPath.indexOf("/java/") + 6;
//				classPath = fullPath.substring(index + 6);
			}else if(fullPath.contains("/src/")){
				index = fullPath.indexOf("/src/") + 5;
//				classPath = fullPath.substring(index + 5);
			}else{
				System.err.println("unexpected classpath: " + fullPath);
				return;
			}
			
			classPath = fullPath.substring(index);
			// /home/apr/d4j/fixed_bugs_dir/Chart/Chart_3/src/xxx
			// get projId
			String id = fullPath.split("_")[ fullPath.split("_").length - 1 ].split("/")[0];
			String[] projTemp = fullPath.split("_")[ fullPath.split("_").length - 2 ].split("/");
			String proj = projTemp[projTemp.length - 1];
			this.projId = proj + "_" + id; // get proj_id
			index = fullPath.indexOf(this.projId);
			this.dpPath = fullPath.substring(0, index);
			
			classPath = classPath.replace("/", ".");
			sc.classPath = classPath;
			if(line.contains("fixed code:")){
				// feature implementation: 
				patchStartLineNo = this.startLineNo;
				patchEndLineNo = this.endLineNo;
				patchClassPath = classPath;
				
				if (logPath == null){
					// bug fix: file name.
					logPath = "./match-log/" + Configuration.proj + '/' + Configuration.id + '/' 
						+ classPath + "_" + startLine + "-" + endLine; 
				}
				if (matchedLogPath == null){
					// bug fix: change logPath to matchedLogPath
					matchedLogPath = "./match-log/" + Configuration.proj + '/' + Configuration.id + '/' 
						+ classPath + "_" + startLine + "-" + endLine + "_matched.log"; 
				}
				
				writeStringToFile(logPath, "fixed code: \n", true);
				runMatchSingleLine(sc, classPath, "fixed_code");;
			}else if(line.contains("similar code:")){
				// code improve: exclude identical similar code.
				if(patchStartLineNo == startLineNo && patchEndLineNo == endLineNo
						&& patchClassPath == classPath){
					continue;
				}
				
				// TODO:This can be actually deleted. 
				if (logPath == null){
					logPath = "./match-log/" + Configuration.proj + '/' + Configuration.id + '/' 
						+ classPath + "_" + this.startLineNo + "-" + this.endLineNo; 
				}
				if (matchedLogPath == null){
					matchedLogPath = "./match-log/" + Configuration.proj + '/' + Configuration.id + '/' 
						+ classPath + "_" + this.startLineNo + "-" + this.endLineNo + "_matched.log"; 
				}
				
				writeStringToFile(logPath, "similar code: \n", true);
				runMatchSingleLine(sc, classPath);;
			}else{
				System.err.println("unexpected line format: " + line);
				return;
			}
		}
		br.close();
	}
	
	public void runMatchSingleLine(SuspiciousPosition sc, String classPath) throws IOException{
		runMatchSingleLine(sc, classPath, "");
	}
	public void runMatchSingleLine(SuspiciousPosition sc, String classPath, String flag) throws IOException{
		List<String> beforeMatches = new ArrayList<>();
		List<String> afterMatches = new ArrayList<>();
		
		// bug fix: re-set matchedStart && end LineNo 
		this.matchedStartLineNo = 0;
		this.matchedEndLineNo = 0;
		this.matchedFlag = "";
		
		for (int lineNo = this.startLineNo; lineNo <= this.endLineNo; lineNo ++){
			sc.lineNumber = lineNo;
			Pair<String, String> pair = matchSingleLine(sc, flag);
			beforeMatches.add(pair.getFirst()+"\n");
			afterMatches.add(pair.getSecond()+"\n");
		}
		
		writeStringToFile(logPath, "---before match--- "
				+ classPath + " <" + this.startLineNo + ", " + this.endLineNo + ">\n", true);
		for (String match : beforeMatches){
			// fix bug: add trim() to wipe out "\n"
			if (match.trim().equals("")){
				continue;
			}
			writeStringToFile(logPath, match, true);
		}
		// code improvement: print matched string only when the code snippet is changed.
		if (!matchedFlag.equals("")){
			writeStringToFile(logPath, "\n---after match--- " + matchedFlag + " \n", true);
			for (String match : afterMatches){
				// fix bug: add trim() to wipe out "\n"
				if (match.trim().equals("")){
					continue;
				}
				writeStringToFile(logPath, match, true);
			}
		}
		
		writeStringToFile(logPath, "\n\n", true);
		
		// get fixed_code
		if (flag.equals("fixed_code")){
			fixed_code.clear();
			if (!matchedFlag.equals("")){
				fixed_code.addAll(afterMatches);
			}else{
				fixed_code.addAll(beforeMatches);
			}
		}else{
			similar_code.clear();
			if (!matchedFlag.equals("")){
				similar_code.addAll(afterMatches);
			}else{
				similar_code.addAll(beforeMatches);
			}
		}
		
		trimCode(fixed_code);
		trimCode(similar_code);
		compareFixedAndSim(fixed_code, similar_code);
	}
	
	private void trimCode(List<String> code){
		// bug fix: should change line <= into line <
		for(int line = 0; line < code.size(); line++){
			String lineCur = code.get(line);
			code.set(line, lineCur.trim());
		}
	}
	
	private void compareFixedAndSim(List<String> fixed_code_ori, List<String> sim_code_ori) {
		// delete all "" string in the two list.
		List<String> fixed_code = filterNullStr(fixed_code_ori);
		List<String> sim_code = filterNullStr(sim_code_ori);
				
		
		if(fixed_code.isEmpty() || sim_code.isEmpty()){
			return;
		}
		
		int equalFlag = 1;
		for (String fixLines : fixed_code){
			// bug fix: (a line may include several lines, so) split into lines
			String[] fix_lines = fixLines.split("\n");
			for (String fixLine : fix_lines){
				fixLine = fixLine.trim();
				
				if (!patchLinesMap.containsValue(fixLine)){
//					writeStringToFile("./error.log",fixLine + "\n", true);
					continue;
				}
				
				// check if in sim_code
				int equalFlag2 = 0;
				for (String simLines:sim_code){
					List<String> sim_lines =  Arrays.asList(simLines.split("\n"));
					trimCode(sim_lines);

					// bug fix: change sim_code into sim_lines
					// bug fix
					// if (!sim_lines.contains(fixLine) && patchLinesMap.containsValue(fixLine)){
					if (sim_lines.contains(fixLine) ){
						equalFlag2 = 1;
						break;
					}
				}
				// fail to find fixed_line in similar code. thus equalFlag = 0
				if (equalFlag2 == 0){
					equalFlag = 0;
					break;
				}
			}
			if(equalFlag == 0){
				break;
			}
		}
		
		if (equalFlag == 1){
			if(fixCodeSaveFlag == 0){
				// bug fix: a copy&paste bug. should change logPath to matchedLogPath
				writeStringToFile(matchedLogPath, "---patch code--- "
						+ patchClassPath + " <" + this.patchStartLineNo + ", " + this.patchEndLineNo + ">\n", true);
				for (String match : fixed_code){
					// fix bug: add trim() to wipe out "\n"
					if (match.trim().equals("")){
						continue;
					}
					writeStringToFile(matchedLogPath, match + "\n", true);
				}
				writeStringToFile(matchedLogPath, "\n", true);	

				fixCodeSaveFlag = 1;
			}
			String str = "";
			if(patchMethod.equals(matchMethod)){
				str = "---fix ingredient---(SameMethod) ";
			}else{
				str = "---fix ingredient--- ";
			}
			writeStringToFile(matchedLogPath, str
					+ classPath + " <" + this.startLineNo + ", " + this.endLineNo + ">\n", true);
			for (String match : sim_code){
				// fix bug: add trim() to wipe out "\n"
				if (match.trim().equals("")){
					continue;
				}
				writeStringToFile(matchedLogPath, match + "\n", true);
			}
			writeStringToFile(matchedLogPath, "\n\n", true);
		}
	}

	private List<String> filterNullStr(List<String> code_ori) {
		List<String> filted_list = new ArrayList<>();
		
		for(String line : code_ori){
			if(!line.equals("")){
				filted_list.add(line);
			}else{
//				print("skip null string in code");
			}
		}
		return filted_list;
	}
	
	public void backup(String flag) throws IOException{
		if (flag.equals("backup")){
			// first backup
			// without "/"
			String srcPath = null;
			if (dp.srcPath.endsWith("/")){
				srcPath = dp.srcPath.substring(0, dp.srcPath.length() - 1);
			}else{
				srcPath = dp.srcPath;
			}
			// this.bkupFolder
			bkupFolder = new File(srcPath + "-ori");
			if (bkupFolder.exists() && bkupFolder.isDirectory()){
				// already have a backup folder
			}else{
				FileUtils.copyDirectory(new File(dp.srcPath), bkupFolder);
			}
		}else{
			// restore
			FileUtils.deleteDirectory(new File(dp.srcPath));
			FileUtils.copyDirectory(bkupFolder, new File(dp.srcPath));
		}
	}

	public Pair<String, String> matchSingleLine(SuspiciousPosition sc, String flag) throws IOException {
		// count 
		parsedLinesCnt ++;
		print("Parsed Lines Count: " + parsedLinesCnt 
				+ " : " + sc.classPath + ":" + sc.lineNumber);
		
		// reset dp for each line.
		this.dp.reset(this.dpPath, this.projId);
		
		//backup
		backup("backup");
		
		// get the AST node for the line
		SuspCodeNode scn1 = parseSuspiciousCode(sc);
		
		if (scn1 == null){
			// (fix bug) if in matched. return "". else return "null_ast" str.
			if (sc.lineNumber <= this.matchedEndLineNo &&
					sc.lineNumber >= this.matchedStartLineNo){
				return new Pair<String, String>("", "");
			}
			String javaFilePath = fullPath + ".java";
			String results = ShellUtils.shellRunForFileRead(Arrays.asList(
				"cat " + javaFilePath + " | sed -n " + sc.lineNumber + "p" ), buggyProject);
			if (results.trim().equals("")){
				results = "empty line";
			}
			return new Pair<String, String>(results, results);		
		}
		
		ITree scan = scn1.suspCodeAstNode; 
		
		// get clazz and super clazz
		Pair<String, String> clazzAndSuper = getClazzSuperMethod(scan, flag);
		String clazz = clazzAndSuper.getFirst();
		String superClazz = clazzAndSuper.getSecond();
		
		// first initialze a FixTemplate
		FixTemplate ft = null;
		ft = new ExpressionReplacer();
		ft.setSuspiciousCodeStr(scn1.suspCodeStr);
		ft.setSuspiciousCodeTree(scan); //scn1.suspCodeAstNode
		if (scn1.javaBackup == null) ft.setSourceCodePath(dp.srcPath);
		else ft.setSourceCodePath(dp.srcPath, scn1.javaBackup);
		
		// get all variables
		List<ITree> suspVars = new ArrayList<>();
		// FIXME: identifySuspiciousVariables method should be modified for my purpose 
		ContextReader.identifySuspiciousVariables(scan, suspVars, new ArrayList<String>());
		ContextReader.readAllVariablesAndFields(scan, ft.allVarNamesMap, ft.varTypesMap, ft.allVarNamesList, ft.sourceCodePath, ft.dic);
		
		// 
		SuspiciousCodeParser scp = new SuspiciousCodeParser();
		String suspiciousJavaFile = sc.classPath.replace(".", "/") + ".java";
		String filePath = this.dp.srcPath + suspiciousJavaFile;
		scp.parseJavaFile(new File(filePath));
		int currentStartLineNo = scp.getUnit().getLineNumber(scan.getPos());
		int currentEndLineNo=  scp.getUnit().getLineNumber(scan.getPos()+ scan.getLength());
		
		// exclude repeated ast match
		if(this.matchedStartLineNo <= currentStartLineNo && 
				this.matchedEndLineNo >= currentStartLineNo){
			return new Pair<String, String>("", "");
		}else{
			// fix bug: sometimes the line number may match a if() ast that includes
			// even the before ast (thus causing repetition). 
			// In this situation, we want to exlude it.
			if (this.matchedStartLineNo != 0 && this.matchedEndLineNo != 0){
				if (currentStartLineNo <= this.matchedStartLineNo &&
						currentEndLineNo >= this.matchedEndLineNo){
					return new Pair<String, String>("", "");
				}
			}
		}
		this.matchedStartLineNo = currentStartLineNo;
		this.matchedEndLineNo = currentEndLineNo;
		
		
		// find class instances
		scp.findClazzInstance(scan, ft);
		ITree clazzIns = scp.getClazzInstance();
		
		// for each class instance, replace it with its class name
		String before_match = scn1.suspCodeStr;
		int whileCnt = 0;
		while(clazzIns != null && whileCnt <= 30){
			whileCnt ++;
			
			int suspExpStartPos = clazzIns.getPos();
			int suspExpEndPos = suspExpStartPos + clazzIns.getLength();
			// specified line start and end pos
			int suspCodeStartPos = scan.getPos();
			int suspCodeEndPos = suspCodeStartPos + scan.getLength();
			// part 1 and 2
			String suspCodePart1 = ft.getSubSuspiciouCodeStr(suspCodeStartPos, suspExpStartPos);
			String suspCodePart2 = ft.getSubSuspiciouCodeStr(suspExpEndPos,suspCodeEndPos);
			
			// conduct replacement
			String label = clazzIns.getLabel();
			// sometimes lalel may be "Name:area"
			if (label.contains(":")){
				label = label.split(":")[1];
			}
			
			String replace = "null";
			if(ft.varTypesMap.containsKey(label)){
				replace = ft.varTypesMap.get(label);
				
				// if is same as current class, then  
				if (replace.equals(clazz) && superClazz != null){
					replace = superClazz;
				}
				
				// if not, parse the file and get the class.
//				String[] cmd2 = {"/bin/sh","-c", " find " 
//						+ dp.srcPath + " -name " + replace + ".java"
//						};
//				// e.g., find /home/apr/d4j/Chart/Chart_1/source/ -name AbstractCategoryItemRenderer.java
//				String result = ShellUtils.shellRun2(cmd2);
//				String otherClazz = scp.getSuperClazz(new File(result.trim()));
//				if(otherClazz != null){
//					replace = otherClazz;
//				}
			}
			
			// FIXME: multi replace
			scn1.suspCodeStr = suspCodePart1  + replace + suspCodePart2;
			
			// set matchedFlag
			this.matchedFlag = "matched";
			
			// apply and re-parseAST
			Patch patch = new Patch();
			patch.setFixedCodeStr1(scn1.suspCodeStr);
			this.addPatchCodeToFile(scn1, patch);// Insert the patch.

			// re-parse
			scn1 = parseSuspiciousCode(sc);
			if (scn1 == null){
				System.err.println("scn1 is null. sc:" + sc.lineNumber);
				return new Pair<String, String>("null_ast", "null_ast");
			}
			ft.setSuspiciousCodeStr(scn1.suspCodeStr); // re-set ft.values
			ft.setSuspiciousCodeTree(scn1.suspCodeAstNode); //scn1.suspCodeAstNode
			// refind 
			scan = scn1.suspCodeAstNode; 
			// refresh scp
			scp = new SuspiciousCodeParser();
			suspiciousJavaFile = sc.classPath.replace(".", "/") + ".java";
			filePath = this.dp.srcPath + suspiciousJavaFile;
			scp.parseJavaFile(new File(filePath));
			// bug Fix:
			scp.resetClazzInstance();
			scp.findClazzInstance(scan, ft);
			clazzIns = scp.getClazzInstance();
//			ft.generatePatch(suspCodePart1  + replace + suspCodePart2);
		}
		// replace this.
		if (scn1.suspCodeStr.contains("this.")){
			if (superClazz != null ){
				scn1.suspCodeStr = scn1.suspCodeStr.replace("this.", superClazz + ".");
				this.matchedFlag = "super.matched";
			}else if (clazz != null){
				scn1.suspCodeStr = scn1.suspCodeStr.replace("this.", clazz + ".");
				this.matchedFlag = "this.matched";
			}else{
				print("cannot find both class and super class.");
			}
			
		}
		
		// restore
		backup("restore");
		
		// set map values
		String[] lines = scn1.suspCodeStr.split("\n");
		int linesNo = lines.length;
		int linesNo2 = currentEndLineNo - currentStartLineNo + 1;
		if (linesNo != linesNo2){
//			writeStringToFile("error.log", "linesNo != linesNo2 \n"
//					+ currentEndLineNo + " to " + currentStartLineNo
//					+ classPath + "\n\n",true);
		}else if(flag != "fixed_code"){
			// bug fix: add else if.
			// do nothing
		}else{
			int cnt = 0;
			for (int lineNo = currentStartLineNo; lineNo <= currentEndLineNo; lineNo ++){
				if (patchLinesMap.containsKey(lineNo)){
					patchLinesMap.put(lineNo, lines[cnt].trim());
				}
				cnt ++;
			}
		}
		return new Pair<String, String>(before_match, scn1.suspCodeStr);
	}
	
//	private void resetDP() {
//		this.dp.reset(logPath, fullPath);
//		this.dp = new DataPreparer(path);
//		dp.prepareData(buggyProject);
//		
//	}

	// get class name, super class, and method name string
	private Pair<String, String> getClazzSuperMethod(ITree scan, String flag) {
		// First: get class and super class (if exists) name 
		ITree parent = scan;
		// TODO: to test whether 1056 TimeSeries copy = (TimeSeries) super.clone(); is typeDecl
		while(!Checker.isTypeDeclaration(parent.getType()) ){
			if (Checker.isMethodDeclaration(parent.getType())){
//				String[] labels = parent.getLabel().split(",");
				// start to analyze method
				// e.g., public, @@LegendItemCollection, MethodName:getLegendItems, @@Argus:null
//				String returnVar = labels[1].trim();
//				String methodName = lables[2].trim();
				// Code improve: to do this in a simpler way!
				if(flag == "fixed_code"){
					this.patchMethod = parent.getLabel();
				}else{
					this.matchMethod = parent.getLabel();
				}
			}
			
			parent = parent.getParent();
			
		}
		// print(parent.toString());
		String[] clazzStrList = parent.toString().split(",");
		String superClazz = null;
		String clazz = null;
		for (String clazzStr : clazzStrList){
			if(clazzStr.contains("@@SuperClass:")){
				superClazz = clazzStr.split(":")[1];
			}
			if(clazzStr.contains("ClassName:")){
				clazz = clazzStr.split(":")[1];
			}
		}
		return new Pair<String, String>(clazz, superClazz);
	}

	class SuspNullExpStr implements Comparable<SuspNullExpStr> {
		public String expStr;
		public Integer startPos;
		public Integer endPos;
		
		public SuspNullExpStr(String expStr, Integer startPos, Integer endPos) {
			this.expStr = expStr;
			this.startPos = startPos;
			this.endPos = endPos;
		}

		@Override
		public int compareTo(SuspNullExpStr o) {
			int result = this.startPos.compareTo(o.startPos);
			 if (result == 0) {
				 result = this.endPos.compareTo(o.endPos);
			 }
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof SuspNullExpStr)) return false;
			return this.expStr.equals(((SuspNullExpStr) obj).expStr);
		}
		
	}
	
//	private void identifySuspiciousVariables(FixTemplate ft, ITree suspCodeAst, List<String> allSuspVariables, List<SuspNullExpStr> suspNullExpStrs) {
//		List<ITree> children = suspCodeAst.getChildren();
//		for (ITree child : children) {
//			int childType = child.getType();
//			if (Checker.isSimpleName(childType)) {
//				int parentType = suspCodeAst.getType();
//				if ((Checker.isAssignment(parentType) || Checker.isVariableDeclarationFragment(parentType))
//						&& suspCodeAst.getChildPosition(child) == 0) {
//					continue;
//				}
//				if ((Checker.isQualifiedName(parentType) || Checker.isFieldAccess(parentType) || Checker.isSuperFieldAccess(parentType)) &&
//						suspCodeAst.getChildPosition(child) == children.size() - 1) {
//					continue;
//				}
//				
//				String varName = ContextReader.readVariableName(child);
//				if (varName != null && !varName.endsWith(".length")) {
//					int startPos = child.getPos();
//					int endPos = startPos + child.getLength();
//					SuspNullExpStr snes = new SuspNullExpStr(varName, startPos, endPos);
//					if (!suspNullExpStrs.contains(snes) && !Checker.isAssignment(suspCodeAst.getType())
//							&& !Checker.isVariableDeclarationFragment(suspCodeAst.getType())) suspNullExpStrs.add(snes);
//					if (!allSuspVariables.contains(varName)) allSuspVariables.add(varName);
//				}
//				else identifySuspiciousVariables(ft, child, allSuspVariables, suspNullExpStrs);
//			} else if (Checker.isMethodInvocation(childType)) {
//				List<ITree> subChildren = child.getChildren();
//				if (subChildren.size() > 2) {
//					int startPos = child.getPos();
//					ITree subChild = subChildren.get(subChildren.size() - 2);
//					int endPos = subChild.getPos() + subChild.getLength();
//					String suspExpStr = ft.getSubSuspiciouCodeStr(startPos, endPos);
//					SuspNullExpStr snes = new SuspNullExpStr(suspExpStr, startPos, endPos);
//					if (!suspNullExpStrs.contains(snes) && !Checker.isAssignment(suspCodeAst.getType())
//							&& !Checker.isVariableDeclarationFragment(suspCodeAst.getType())) suspNullExpStrs.add(snes);
//				}
//				identifySuspiciousVariables(ft, child, allSuspVariables, suspNullExpStrs);
//			} else if (Checker.isArrayAccess(childType)) {
//				int startPos = child.getPos();
//				int endPos = startPos + child.getLength();
//				String suspExpStr = ft.getSubSuspiciouCodeStr(startPos, endPos);
//				SuspNullExpStr snes = new SuspNullExpStr(suspExpStr, startPos, endPos);
//				if (!suspNullExpStrs.contains(snes) && !Checker.isAssignment(suspCodeAst.getType())
//						&& !Checker.isVariableDeclarationFragment(suspCodeAst.getType())) suspNullExpStrs.add(snes);
//				identifySuspiciousVariables(ft, child, allSuspVariables, suspNullExpStrs);
//			} else if (Checker.isQualifiedName(childType) || Checker.isFieldAccess(childType) || Checker.isSuperFieldAccess(childType)) {
//				int parentType = suspCodeAst.getType();
//				if ((Checker.isAssignment(parentType) || Checker.isVariableDeclarationFragment(parentType))
//						&& suspCodeAst.getChildPosition(child) == 0) {
//					continue;
//				}
//				int startPos = child.getPos();
//				int endPos = startPos + child.getLength();
//				String suspExpStr = ft.getSubSuspiciouCodeStr(startPos, endPos);
//				
//				if (!suspExpStr.endsWith(".length")) {
//					SuspNullExpStr snes = new SuspNullExpStr(suspExpStr, startPos, endPos);
//					if (!suspNullExpStrs.contains(snes) && !Checker.isAssignment(suspCodeAst.getType())
//							&& !Checker.isVariableDeclarationFragment(suspCodeAst.getType())) suspNullExpStrs.add(snes);
//					if (!allSuspVariables.contains(suspExpStr)) allSuspVariables.add(suspExpStr);
//				}
//				int index1 = suspExpStr.indexOf(".");
//				int index2 = suspExpStr.lastIndexOf(".");
//				if (index1 != index2) identifySuspiciousVariables(ft, child, allSuspVariables, suspNullExpStrs);
//			} else if (Checker.isFieldAccess(childType) || Checker.isSuperFieldAccess(childType)) {
//				int parentType = suspCodeAst.getType();
//				if ((Checker.isAssignment(parentType) || Checker.isVariableDeclarationFragment(parentType))
//						&& suspCodeAst.getChildPosition(child) == 0) {
//					continue;
//				}
//				String nameStr = child.getLabel(); // "this."/"super." + varName
//				if (!allSuspVariables.contains(nameStr)) allSuspVariables.add(nameStr);
//			} else if (Checker.isComplexExpression(childType)) {
//				identifySuspiciousVariables(ft, child, allSuspVariables, suspNullExpStrs);
//			} else if (Checker.isStatement(childType)) break;
//		}
//	}
	
	@Override
	public void fixProcess() {
		// FIXME: this is a test code snippet
		// apr test here
		SuspiciousPosition sc = new SuspiciousPosition();
		sc.classPath = "org.jfree.data.time.TimeSeries";
		sc.lineNumber = 1057;
		SuspCodeNode scn1 = parseSuspiciousCode(sc);
		ITree scan = scn1.suspCodeAstNode;
		// assignment
//		int type = scan.getType();
//		if (Checker.isAssignment(type) || Checker.isExpressionStatement(type)){
		for (ITree itree : scan.getChildren()){
			print("itree in scan: " + itree.toTreeString());
		}
//		SuspiciousCodeParser scp = new SuspiciousCodeParser();
		ITree simpleName = null;//scp.findSimpleName(scan);	
		int suspExpStartPos = simpleName.getPos();
		int suspExpEndPos = suspExpStartPos + simpleName.getLength();
		
		FixTemplate ft = null;
		ft = new ExpressionReplacer();
		ft.setSuspiciousCodeStr(scn1.suspCodeStr);
		ft.setSuspiciousCodeTree(scn1.suspCodeAstNode);
		if (scn1.javaBackup == null) ft.setSourceCodePath(dp.srcPath);
		else ft.setSourceCodePath(dp.srcPath, scn1.javaBackup);
		
		int suspCodeStartPos = scan.getPos();
		int suspCodeEndPos = suspCodeStartPos + scan.getLength();
		String suspCodePart1 = ft.getSubSuspiciouCodeStr(suspCodeStartPos, suspExpStartPos);
		String suspCodePart2 = ft.getSubSuspiciouCodeStr(suspExpEndPos,suspCodeEndPos);
		ft.generatePatch(suspCodePart1  + "TimeSeries" + suspCodePart2);
		
		ITree parent = scan.getParent();
		while( !Checker.isTypeDeclaration(parent.getType()) ){
			parent = parent.getParent();
		}
//		print(parent.toString());
		String[] clazzStrList = parent.toString().split(",");
		String superClazz = "null";
		String clazz = "null";
		for (String clazzStr : clazzStrList){
			if(clazzStr.contains("@@SuperClass:")){
				superClazz = clazzStr.split(":")[1];
			}
			if(clazzStr.contains("ClassName:")){
				clazz = clazzStr.split(":")[1];
			}
		}
		
		ContextReader.readAllVariablesAndFields(scn1.suspCodeAstNode, ft.allVarNamesMap, ft.varTypesMap, ft.allVarNamesList, ft.sourceCodePath, ft.dic);
		
		
		// Read paths of the buggy project.
		if (!dp.validPaths) return;
		
		// Read suspicious positions.
		List<SuspiciousPosition> suspiciousCodeList = readSuspiciousCodeFromFile(metric, buggyProject, dp);
		if (suspiciousCodeList == null) return;
		
		List<SuspCodeNode> triedSuspNode = new ArrayList<>();
		log.info("=======PARFIXER: Start to fix suspicious code======");
		for (SuspiciousPosition suspiciousCode : suspiciousCodeList) {		
			SuspCodeNode scn = parseSuspiciousCode(suspiciousCode);
			if (scn == null) continue;

//			log.debug(scn.suspCodeStr);
			if (triedSuspNode.contains(scn)) continue;
			triedSuspNode.add(scn);
	        // Match fix templates for this suspicious code with its context information.
	        fixWithMatchedFixTemplates(scn);
	        
			if (minErrorTest == 0) break;
        }
		log.info("=======PARFIXER: Finish off fixing======");
		
		FileHelper.deleteDirectory(Configuration.TEMP_FILES_PATH + this.dataType + "/" + this.buggyProject);
	}
	
	protected void fixWithMatchedFixTemplates(SuspCodeNode scn) {
		
		// Parse context information of the suspicious code.
		List<Integer> contextInfoList = readAllNodeTypes(scn.suspCodeAstNode);
		List<Integer> distinctContextInfo = new ArrayList<>();
		for (Integer contInfo : contextInfoList) {
			if (!distinctContextInfo.contains(contInfo)) {
				distinctContextInfo.add(contInfo);
			}
		}
//		List<Integer> distinctContextInfo = contextInfoList.stream().distinct().collect(Collectors.toList());
		
		// generate patches with fix templates.
		FixTemplate ft = null;
		for (int contextInfo : distinctContextInfo) {
			if (Checker.isCastExpression(contextInfo)) {
				ft = new ClassCastChecker();
			} else if (Checker.isMethodInvocation(contextInfo) || Checker.isConstructorInvocation(contextInfo)
					|| Checker.isSuperConstructorInvocation(contextInfo) || Checker.isSuperMethodInvocation(contextInfo)
					|| Checker.isClassInstanceCreation(contextInfo)) {
//				// CollectionSizeChecker: method name must be "get".
				ft = new CollectionSizeChecker();
				generatePatches(ft, scn);
				if (this.minErrorTest == 0) break;
				
				ft = new ParameterAdder();
				generatePatches(ft, scn);
				if (this.minErrorTest == 0) break;
				
				ft = new ParameterRemover();
				generatePatches(ft, scn);
				if (this.minErrorTest == 0) break;
				
				ft = new ParameterReplacer();
				generatePatches(ft, scn);
				if (this.minErrorTest == 0) break;
				
				ft = new MethodReplacer();
			} else if (Checker.isIfStatement(contextInfo) || Checker.isWhileStatement(contextInfo) || Checker.isDoStatement(contextInfo) 
					|| (Checker.isReturnStatement(contextInfo) && isBooleanReturnMethod(scn.suspCodeAstNode))) {
				ft = new ExpressionReplacer();
				generatePatches(ft, scn);
				if (this.minErrorTest == 0) break;
				
				ft = new ExpressionRemover();
				generatePatches(ft, scn);
				if (this.minErrorTest == 0) break;
				
				ft = new ExpressionAdder();
			} else if (!Checker.withBlockStatement(scn.suspCodeAstNode.getType()) && Checker.isConditionalExpression(contextInfo)) {
				// TODO
			} else if (Checker.isSimpleName(contextInfo)) {
				ft = new NullPointerChecker();
//				generatePatches(ft, scn);
//				if (this.minErrorTest == 0) break;
//				
//				if (!distinctContextInfo.contains(25)) {// Do not re-initialize the variable(s) in IfStatement.
//					ft = new ObjectInitializer();
//				}
			} else if (Checker.isArrayAccess(contextInfo)) {
				ft = new RangeChecker();
//			} else if (Checker.isReturnStatement(contextInfo)) {
				// exchange true with false. TODO
			}
			if (ft == null) continue;
			generatePatches(ft, scn);
			if (this.minErrorTest == 0) break;
			ft = null;
		}
	}
	
	private boolean isBooleanReturnMethod(ITree suspCodeAstNode) {
		ITree parent = suspCodeAstNode.getParent();
		while (true) {
			if (parent == null) return false;
			if (Checker.isMethodDeclaration(parent.getType())) {
				break;
			}
			parent = parent.getParent();
		}
		
		String label = parent.getLabel();
		int indexOfMethodName = label.indexOf("MethodName:");

		// Read return type.
		String returnType = label.substring(label.indexOf("@@") + 2, indexOfMethodName - 2);
		int index = returnType.indexOf("@@tp:");
		if (index > 0) returnType = returnType.substring(0, index - 2);
		
		if ("boolean".equalsIgnoreCase(returnType))
			return true;
		
		return false;
	}

	private void generatePatches(FixTemplate ft, SuspCodeNode scn) {
		ft.setSuspiciousCodeStr(scn.suspCodeStr);
		ft.setSuspiciousCodeTree(scn.suspCodeAstNode);
		if (scn.javaBackup == null) ft.setSourceCodePath(dp.srcPath);
		else ft.setSourceCodePath(dp.srcPath, scn.javaBackup);
		ft.generatePatches();
		List<Patch> patchCandidates = ft.getPatches();
		
		// Test generated patches.
		if (patchCandidates.isEmpty()) return;
		testGeneratedPatches(patchCandidates, scn);
	}

	private List<Integer> readAllNodeTypes(ITree suspCodeAstNode) {
		List<Integer> nodeTypes = new ArrayList<>();
		nodeTypes.add(suspCodeAstNode.getType());
		List<ITree> children = suspCodeAstNode.getChildren();
		for (ITree child : children) {
			if (Checker.isStatement(child.getType())) break;
			nodeTypes.addAll(readAllNodeTypes(child));
		}
		return nodeTypes;
	}
	
	
	
	private void init() {
		// clear map
		this.patchLinesMap.clear();
		
		String logfile = "./match-log/" + Configuration.proj + '/' + Configuration.id + '/' ;
		File dir = new File(logfile);
		if (dir.exists() && dir.isDirectory()) {
			for (File file : dir.listFiles()){
				file.delete();
				print(file.getName() + " exists, and now clear it at the beginning of the process.");
			}
		}
		
		File file = new File("./error.log");
		if (file.exists()){
			file.delete();
			print(file.getName() + " exists, and now clear it at the beginning of the process.");
		}
	}


}

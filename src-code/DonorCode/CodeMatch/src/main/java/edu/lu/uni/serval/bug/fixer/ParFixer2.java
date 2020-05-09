package edu.lu.uni.serval.bug.fixer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.lu.uni.serval.config.Configuration;
import edu.lu.uni.serval.entity.Pair;
import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.main.FileOp;
import edu.lu.uni.serval.main.Triple;
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
import edu.lu.uni.serval.tbar.context.ContextReader2;
import edu.lu.uni.serval.utils.Checker;
import edu.lu.uni.serval.utils.FileHelper;
import edu.lu.uni.serval.utils.ShellUtils;
import edu.lu.uni.serval.utils.SuspiciousPosition;
import edu.lu.uni.serval.utils.SuspiciousCodeParser;

/**
 * This is to create a variable mapping/code matching version.
 * @author apr
 *
 */
public class ParFixer2 extends AbstractFixer {
	
	private static Logger log = LoggerFactory.getLogger(ParFixer2.class);
	public static String extraLog = "./match-log/" + Configuration.proj + '/' + Configuration.id + "/extra-info.log";
	
	// apr
	private static String logPath = null; // output path
	private int startLineNo = 0; 
	private int endLineNo = 0;
	private int matchedStartLineNo = 0;
	private int matchedEndLineNo = 0;
	private String fullPath = null;
	private String dpPath = null;
	private String projId = null;
//	private String matchedFlag = null;
	private String matchedLogPath = null;
	private List<String> fixed_code = new ArrayList<>();
	private List<String> fixed_code0 = new ArrayList<>();
	private List<String> fixed_code1 = new ArrayList<>();
	private List<String> similar_code = new ArrayList<>();
	private List<String> similar_code0 = new ArrayList<>();
	private List<String> similar_code1 = new ArrayList<>();
	private String classPath = "null";
	
	// code improvement: add HashMap for patch_match
	// add feature. (Map<String, String> more variableMapping strategies)
	private Map<Integer, Map<String, String>> patchLinesMap = new HashMap<Integer, Map<String, String>>(); 
	private Map<Integer, List<String>> modMap = new HashMap<>(); // to save all mods (fine-grained)
	private Map<Integer, List<String>> modMap0 = new HashMap<>(); // varMapping 0
	private Map<Integer, List<String>> modMap1 = new HashMap<>(); // varMapping 1
	
	private int patchStartLineNo = 0;
	private int patchEndLineNo = 0;
	// bug fix: I forget to set patchClassPath
	private String patchClassPath = "";
	private String matchClassPath = "";
	// code improve
	private String patchMethod = null;
	private String matchMethod = null;
	private String patchPackage = null;
	private String matchPackage = null;
	
	// code improve: only save the same fixCode once. (do not repeat)
	private Map<String, Integer> fixCodeSaveFlag = new HashMap<>();
	
	// code improve:
	// private File bkupFolder;
	private int parsedLinesCnt = 0;
	private int logStartLine;
	private int logEndLine;
	
	public ParFixer2(String path, String projectName, int bugId, String defects4jPath) {
		super(path, projectName, bugId, defects4jPath);
	}
	
	public ParFixer2(String path, String metric, String projectName, int bugId, String defects4jPath) {
		super(path, metric, projectName, bugId, defects4jPath);
	}
	
	public void initAll(){
		patchStartLineNo = 0;
		patchEndLineNo = 0;
		
		patchClassPath = "";
		matchClassPath = "";
		patchMethod = null;
		matchMethod = null;
		patchPackage = null;
		matchPackage = null;
		
		fixCodeSaveFlag.clear();
		fixCodeSaveFlag.put("ori", 0);
		fixCodeSaveFlag.put("0", 0);
		fixCodeSaveFlag.put("1", 0);
		
		// bug fix: clear again!
		patchLinesMap.clear();
		modMap.clear();
		modMap0.clear();
		modMap1.clear();
		// code improve: output to a matched file for each fix code.
		matchedLogPath = null;
		// bug fix: logPath should not be null. When fixing, try to change matchLinesFromEachFile method
		logPath = null;
	}
	
	/**
	 * To find matched similar code snippet(s) for the given fixed code snippet(s).
	 */
	public void matchLines() throws IOException{
		// init: delete previous output results 
		init();
				
		// get CodeSearch result output file
		File dir = new File(Configuration.linesFilePath);
		if (!dir.exists()){
			print("File : " + Configuration.linesFilePath + " does not exist!");
			return;
		}
		
		// list all files in the dir, and select what we want.
		File[] files = dir.listFiles(); 
		for (File file : files){
			String fileName = file.getName();
			int len = fileName.length();
			String filePath = null;
			
			// bug fix: exclude xxx.~ file. 
			if(fileName.substring(0,6).equals("lines_") 
					&& !fileName.substring(len-1, len).equals("~")){
				filePath = file.getAbsolutePath();

				// get patch lines
				//e.g., lines_org.joda.time.Partial:218-218_fixed.log
				String[] linesTmp = filePath.split(":")[1].split("_")[0].split("-");
				this.logStartLine = Integer.parseInt(linesTmp[0]);
				this.logEndLine = Integer.parseInt(linesTmp[1]);
				String classPath = fileName.split("_")[1].split(":")[0];
				
				// init all relevant
				initAll();
				for (int lineNo = logStartLine; lineNo <= logEndLine; lineNo ++){
					patchLinesMap.put(lineNo, new HashMap<String,String>()); // each patchLinesMap corresponds a "lines_xxx.log" file
					modMap = FileOp.getFineGrainedModifications(classPath, lineNo, Configuration.proj, Configuration.id);
					modMap0 = new HashMap<>(modMap);
					modMap1 = new HashMap<>(modMap);
				}
				
				// exclude type1 lines
				excludeIntrinsicLines(patchLinesMap);
				
				if(patchLinesMap.isEmpty()){
					print("patchLinesMap is empty, continue for next patch chunk.");
					continue;
				}
				
				matchLinesFromEachFile(filePath);
			}
			
			// a simple check.
//			if (filePath == null){
//				System.out.println("filePath not found in : " + Configuration.linesFilePath);
//				continue;
//			}
		}
		
		// TODO commented but useful.
		File file = new File(extraLog); // e.g., ./match-log/Chart/3/extra-info.log
		if (file.exists()){
			file.delete();
		}
	}
	
	/**
	 * exlcude type1 lines from pacthLinesMap
	 * @param patchLinesMap2
	 */
	private void excludeIntrinsicLines(Map<Integer, Map<String, String>> patchLinesMap2) {
		File dir = new File(Configuration.linesFilePath);
		// list all files in the dir, and select what we want.
		File[] files = dir.listFiles(); 
		for (File file : files){
			String fileName = file.getName();
			int len = fileName.length();
			// e.g., org.jfree.chart.renderer.category.AbstractCategoryItemRenderer:1797_type1.log
			if(len >= 10 && fileName.substring(len-10,len).equals("_intrinsic.log")){
				// get intrinsic lines
				int line = Integer.parseInt( fileName.split(":")[1].split("_")[0] );

				// exclude intrinsic lines now
				if (patchLinesMap2.containsKey(line)){
					patchLinesMap2.remove(line);
				}
			}
		}
	}

	public void matchLinesFromEachFile(String filePath) throws IOException{
		// read lines
		@SuppressWarnings("resource")
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(filePath)));
		SuspiciousPosition sc = new SuspiciousPosition();
		
		for (String line = br.readLine(); line != null; line = br.readLine()) {
			// /home/apr/d4j/fixed_bugs_dir/Chart/Chart_3/source/org/jfree/data/time/TimeSeries
			// e.g., line: fixed code:/home/apr/d4j/fixed_bugs_dir/Closure/Closure_2/
			// src/com/google/javascript/jscomp/TypeCheck.java-1575-1575
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
			
			if (fullPath.contains("/home/")){
				this.dpPath = fullPath.substring(0, index);
			}else{
				this.dpPath = "../CodeSearch/" + fullPath.substring(0, index);
			}
			
			classPath = classPath.replace("/", ".");
			sc.classPath = classPath;
			if(line.contains("fixed code:")){
				// feature implementation: 
				// bug fix:(closure 2) choose a larger <start, end> fixed code range
				if(this.startLineNo >= this.logStartLine && this.endLineNo <= this.logEndLine){
					patchStartLineNo = this.logStartLine;
					patchEndLineNo = this.logEndLine;

					// only for fixed code
					this.startLineNo = this.logStartLine;
					this.endLineNo = this.logEndLine;
				}else{
					patchStartLineNo = this.startLineNo;
					patchEndLineNo = this.endLineNo;
				}
				
				patchClassPath = classPath;

				if (logPath == null){
					// bug fix: file name.
					logPath = "./match-log/" + Configuration.proj + '/' + Configuration.id + '/' 
						+ classPath + "_" + logStartLine + "-" + logEndLine; 
				}
				if (matchedLogPath == null){
					// bug fix: change logPath to matchedLogPath
					matchedLogPath = "./match-log/" + Configuration.proj + '/' + Configuration.id + '/' 
						+ classPath + "_" + logStartLine + "-" + logEndLine + "_matched.log"; 
				}
				
				writeStringToFile(logPath, "fixed code: \n", true);
				runMatchSingleLine(sc, classPath, "fixed_code");;
			}else if(line.contains("similar code:")){
				// code improve: exclude identical similar code.
//				if(patchStartLineNo == startLineNo && patchEndLineNo == endLineNo
//						&& patchClassPath.equals(classPath)){
//					// bug fix: change  == into .equals
//					//com.google.javascript.jscomp.FlowSensitiveInlineVariables
//					//com.google.javascript.jscomp.FlowSensitiveInlineVariables
//					continue;
//				}
				
				this.matchClassPath = classPath;
				
				// TODO:This can be actually deleted. 
//				if (logPath == null){
//					logPath = "./match-log/" + Configuration.proj + '/' + Configuration.id + '/' 
//						+ classPath + "_" + this.startLineNo + "-" + this.endLineNo; 
//				}
//				if (matchedLogPath == null){
//					matchedLogPath = "./match-log/" + Configuration.proj + '/' + Configuration.id + '/' 
//						+ classPath + "_" + this.startLineNo + "-" + this.endLineNo + "_matched.log"; 
//				}
				
				writeStringToFile(logPath, "similar code: \n", true);
				runMatchSingleLine(sc, classPath, "sim_code");;
			}else{
				System.err.println("unexpected line format: " + line);
				return;
			}
		}
		br.close();
	}
	
	/**
	 * get before_match_code and after_match_code (if exists).
	 * flag: fixed_code, sim_code.
	 * @param sc
	 * @param classPath
	 * @param flag=fixed_code or sim_code
	 * @throws IOException
	 */
	public void runMatchSingleLine(SuspiciousPosition sc, String classPath, String flag) throws IOException{
		List<String> beforeMatches = new ArrayList<>();
		List<String> afterMatches  = new ArrayList<>();
		List<String> afterMatches1  = new ArrayList<>();
		
		// bug fix: re-set matchedStart && end LineNo 
		this.matchedStartLineNo = 0;
		this.matchedEndLineNo = 0;
		List<String>matchedFlags = new ArrayList<>(); 
		matchedFlags.add("");
		matchedFlags.add(""); // init two match flags for two variable mapping strategies.
		
		for (int lineNo = this.startLineNo; lineNo <= this.endLineNo; lineNo ++){
			sc.lineNumber = lineNo;
			Pair<String, List<String>> pair = matchSingleLine(sc, flag, matchedFlags);
			String beforeMatch = pair.getFirst();
			String afterMatch = pair.getSecond().get(0);
			String afterMatch1 = pair.getSecond().get(1); // get all matches (before, after, after1)
			
			// bug fix: closure2: ixed code: ---origianl--- com.google.javascript.jscomp.TypeCheck <1575, 1576> 
			//		 print extra \n. so fix it.
			beforeMatches.add(beforeMatch);
			afterMatches.add(afterMatch);
			afterMatches1.add(afterMatch1);
//			beforeMatches.add(beforeMatch+"\n");
//			afterMatches.add(afterMatch+"\n");
//			afterMatches1.add(afterMatch1+"\n");
		}
		
		record(logPath,beforeMatches, "origianl", classPath, this.startLineNo, this.endLineNo);
		// code improvement: print matched string only when the code snippet is changed.
		if(!matchedFlags.get(0).equals("")){
			record(logPath,afterMatches, "afterMatch-type0", classPath, this.startLineNo, this.endLineNo); // variableMapping 
		}
		if(!matchedFlags.get(1).equals("")){
			record(logPath,afterMatches1, "afterMatch-type1", classPath, this.startLineNo, this.endLineNo); // variableMapping1
		}
//		writeStringToFile(logPath, "\n\n", true);
		
		// set fixed code & similar code with three types: original, varMap0, varMap1. 
		if (flag.equals("fixed_code")){
			fixed_code.clear();
			fixed_code0.clear();
			fixed_code1.clear();
			
			fixed_code.addAll(beforeMatches);
			fixed_code0.addAll(afterMatches);
			fixed_code1.addAll(afterMatches1);
		}else{
			similar_code.clear();
			similar_code0.clear();
			similar_code1.clear();
			
			similar_code.addAll(beforeMatches);
			similar_code0.addAll(afterMatches);
			similar_code1.addAll(afterMatches1);
		}
		//Match-1:compare original code
		trimCode(fixed_code);
		trimCode(similar_code);
		compareFixedAndSim(fixed_code, similar_code, "ori");
		
		trimCode(fixed_code0);
		trimCode(similar_code0);
		compareFixedAndSim(fixed_code0, similar_code0, "0");
		
		trimCode(fixed_code1);
		trimCode(similar_code1);
		compareFixedAndSim(fixed_code1, similar_code1, "1");
	}
	
	/**
	 * This is to save List<String> into a file
	 * @param log
	 * @param matches
	 * @param flag
	 */
	private void record(String log, List<String> matches, String flag, 
			String classPath, int startLineNo, int endLineNo) {
		writeStringToFile(log, "---" + flag + "--- "
				+ classPath + " <" + startLineNo + ", " + endLineNo + ">\n", true);
		for (String match : matches){
			// fix bug: add trim() to wipe out "\n"
			if (match.trim().equals("")){
				continue;
			}
			writeStringToFile(log, match +"\n", true);
		}
		writeStringToFile(log, "\n", true);
	}

	private void trimCode(List<String> code){
		// bug fix: should change line <= into line <
		for(int line = 0; line < code.size(); line++){
			String lineCur = code.get(line);
			code.set(line, lineCur.trim());
		}
	}
	
	private void compareFixedAndSim(List<String> fixed_code_ori, 
			List<String> sim_code_ori, String mapType) {
		// delete all "" string in the two list.
		List<String> fixed_code = filterNullStr(fixed_code_ori);
		List<String> sim_code = filterNullStr(sim_code_ori);
				
		
		if(fixed_code.isEmpty() || sim_code.isEmpty()){
			return;
		}
		
		int equalFlag = 1;
		int lineNo ;
		String varMatchType = ""; // ori: ori match; 0: 0 match
		for (String fixLines : fixed_code){
			// bug fix: (a line may include several lines, so) split into lines
			// TODO: if with empty line, observe the fixed_lines (cnt).
			String[] fix_lines = fixLines.split("\n");
			for (String fixLine : fix_lines){
				fixLine = fixLine.trim();
				
				lineNo = -1;// init
				
//				if (!patchLinesMap.containsValue(Arrays.asList(mapType, fixLine))){
//					//writeStringToFile(extraLog, "patchLinesMap does not contain " + fixLine + "\n", true);
//					continue;
//				}
				int isPatchLineFlag = 0;
				for (Map.Entry<Integer, Map<String, String>> entry : patchLinesMap.entrySet()){
					lineNo = entry.getKey();
					Map<String, String> valueMap = entry.getValue();
					String line = valueMap.get(mapType); // get line according to mapType
					if (line.equals(fixLine)){
						isPatchLineFlag = 1; // in the map.
						break;
					}
				}
//				for (Map<String, String> valueMap : patchLinesMap.values()){
//					String line = valueMap.get(mapType); // get line according to mapType
//					if (line.equals(fixLine)){
//						isPatchLineFlag = 1; // in the map.
//						break;
//					}
//				}
				if(isPatchLineFlag == 0){ // is not a patch line
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
					}else if(mapType.equals("ori")){ // refactor.
						boolean match = matchFineGrained(modMap.get(lineNo), sim_lines);
						if(match){
							equalFlag2 = 1;
							varMatchType += " <ori-fine-grained> ";
							break;
						}else if(parseContainCheck(sim_lines, fixLine)){//TODO: an attempt
							// bug fix: this is the right place.!(time2) When the general varMap fails, try the parseMap.
							equalFlag2 = 1;
							varMatchType += " <parseContainCheck> ";
							break;
						}else if(parseContainCheck2(sim_lines, fixLine)){//TODO: an attempt
							// bug fix: this is the right place.!(time2) When the general varMap fails, try the parseMap.
							equalFlag2 = 1;
							varMatchType += " <parseContainCheck2> ";
							break;
						}
					}else if(mapType.equals("0")){ //varMap 0
						boolean match = matchFineGrained(modMap0.get(lineNo), sim_lines);
						if(match){
							equalFlag2 = 1;
							varMatchType += " <0-fine-grained> ";
							break;
						}else if(parseContainCheck(sim_lines, fixLine)){//TODO: an attempt
							equalFlag2 = 1;
							varMatchType += " <parseContainCheck> ";
							break;
						}else if(parseContainCheck2(sim_lines, fixLine)){//TODO: an attempt
							// bug fix: this is the right place.!(time2) When the general varMap fails, try the parseMap.
							equalFlag2 = 1;
							varMatchType += " <parseContainCheck2> ";
							break;
						}
					}else if(mapType.equals("1")){ //varMap 1
						boolean match = matchFineGrained(modMap1.get(lineNo), sim_lines);
						if(match){
							equalFlag2 = 1;
							varMatchType += " <1-fine-grained> ";
							break;
						}else if(parseContainCheck(sim_lines, fixLine)){//TODO: an attempt
							equalFlag2 = 1;
							varMatchType += " <parseContainCheck> ";
							break;
						}else if(parseContainCheck2(sim_lines, fixLine)){//TODO: an attempt
							// bug fix: this is the right place.!(time2) When the general varMap fails, try the parseMap.
							equalFlag2 = 1;
							varMatchType += " <parseContainCheck2> ";
							break;
						}
					}else{
						// do nothing.
//						writeStringToFile(extraLog, "Fail to match.\n", true);
					}
				}
					
//					else{ // if not directly contain fixLine. try modMap matching.
//						if(mapType.equals("ori")){ //origianl
////							print("modMap.get(lineNo):" + modMap.get(lineNo).toString());
//							
//						}
//						else if(mapType.equals("0")){ //varMap 0
//							boolean match = matchFineGrained(modMap0.get(lineNo), sim_lines);
//							if(match){
//								equalFlag2 = 1;
//								varMatchType += " <0-fine-grained> ";
//								break;
//							}
//						}
//						else if(mapType.equals("1")){ //varMap 1
//							boolean match = matchFineGrained(modMap1.get(lineNo), sim_lines);
//							if(match){
//								equalFlag2 = 1;
//								varMatchType += " <1-fine-grained> ";
//								break;
//							}
//						}else{
//							writeStringToFile(extraLog, "unknown mapType: " + mapType + " !", true);
//						}
//					}
//				}
				
				// if any patch line is not matched, break immediately.
				// fail to find fixed_line in similar code. thus equalFlag = 0
				if (equalFlag2 == 0){
					equalFlag = 0;
					break;
				}
			}
			if(equalFlag == 0){ //stop loop once any patched line is not matched.
				break;
			}
		}
		
		if (equalFlag == 1){
			if(fixCodeSaveFlag.get(mapType) == 0){
				record(matchedLogPath, fixed_code, "patch code " + mapType,
						patchClassPath, this.patchStartLineNo, this.patchEndLineNo);
				// bug fix: a copy&paste bug. should change logPath to matchedLogPath
//				writeStringToFile(matchedLogPath, "---patch code " + mapType + " --- "
//						+ patchClassPath + " <" + this.patchStartLineNo + ", " + this.patchEndLineNo + ">\n", true);
//				for (String match : fixed_code){
//					// fix bug: add trim() to wipe out "\n"
//					if (match.trim().equals("")){
//						continue;
//					}
//					writeStringToFile(matchedLogPath, match + "\n", true);
//				}
//				writeStringToFile(matchedLogPath, "\n", true);	

				fixCodeSaveFlag.put(mapType, 1);
			}
			String str = "";
			// bug fix: chart 10. add && xxx
			if(patchMethod.equals(matchMethod) && patchClassPath.equals(matchClassPath)){
				str = "fix ingredient(SameMethod) ";
			}else if(patchClassPath.equals(matchClassPath)){
				str = "fix ingredient(SameFile) ";
			}else if(patchPackage.equals(matchPackage)){
				str = "fix ingredient(SamePackage) ";
			}else{
				str = "fix ingredient ";
			}
			str += varMatchType; // add the flag.
			
			record(matchedLogPath, sim_code, str + " varMapType:" + mapType,
					classPath, this.startLineNo, this.endLineNo);
//			writeStringToFile(matchedLogPath, str
//					+ classPath + " <" + this.startLineNo + ", " + this.endLineNo + ">\n", true);
//			for (String match : sim_code){
//				// fix bug: add trim() to wipe out "\n"
//				if (match.trim().equals("")){
//					continue;
//				}
//				writeStringToFile(matchedLogPath, match + "\n", true);
//			}
//			writeStringToFile(matchedLogPath, "\n\n", true);
		}
	}

	private boolean parseContainCheck(List<String> sim_lines, String fixLine) {
//		String matchPattern = "[^a-z^A-Z^0-9^!^=^-^+^<^>^*^/]";
//		String matchPattern = "[^a-z^A-Z^0-9^!^=^\\-^\\+^<^>^*^/]";
		// code improve
		String matchPattern = "[^a-z^A-Z^0-9^!^=^\\-^\\+^<^>^*^/^\\.^&^|^_]";
		String fixLine_replace = fixLine.replaceAll(matchPattern, " ").replaceAll(" +", " ");// old-pattern:"[^a-z^A-Z^0-9^!^=]"
		for(String sim_line : sim_lines){
			String sim_line_replace = sim_line.replaceAll(matchPattern, " ").replaceAll(" +", " ");
			
			if(sim_line_replace.contains(fixLine_replace)){
				writeStringToFile(extraLog, "parseContainCheck() macth: \n" 
					+ "sim_line_replace: " + sim_line_replace +"\n"
					+ "fixLine_replace : " + fixLine_replace +"\n");
				return true;
			}
		}
		
		return false;
	}
	
	private boolean parseContainCheck2(List<String> sim_lines, String fixLine) {
//		String matchPattern = "[^a-z^A-Z^0-9^!^=^-^+^<^>^*^/]";
		String matchPattern = "[^a-z^A-Z^0-9^!^=^\\-^\\+^<^>^*^/^\\.^&^|^_]";
		String fixLine_replace = fixLine.replaceAll(matchPattern, " ").replaceAll(" +", " ");// old-pattern:"[^a-z^A-Z^0-9^!^=]"
		String[] mods = fixLine_replace.split("\\s+");
		
		for(String sim_line : sim_lines){
			String sim_line_replace = sim_line.replaceAll(matchPattern, " ").replaceAll(" +", " ");
			List<String> sim_lists = Arrays.asList(sim_line_replace.split(" "));
			int matchFlag = 1; // 0: not match 1:match
			for (String mod : mods){
				// bug fix: (math3)outside match
				if(!sim_lists.contains(mod)){
					matchFlag = 0;
					break;
				}
				
				//bug fix: time1: consider the mods order.
				int ind = sim_line.indexOf(mod);
				if(ind >= 0){ // if can be found
					sim_line = sim_line.substring(ind + mod.length());
				}else{ //else, fail to match
					matchFlag = 0;
					break;
				}
			}
			// check if the current line is matched
			if (matchFlag == 1){
				writeStringToFile(extraLog, "parseContainCheck2() macth: \n" 
						+ "sim_line_replace: " + sim_line_replace +"\n"
						+ "fixLine_replace : " + fixLine_replace +"\n");
				return true;
			}
			
			// bug fix: math3 outside match
//			if(sim_line_replace.contains(fixLine_replace)){
//				writeStringToFile(extraLog, "parseContainCheck2() macth: \n" 
//					+ "sim_line_replace: " + sim_line_replace +"\n"
//					+ "fixLine_replace : " + fixLine_replace +"\n");
//				return true;
//			}
		}
		
		return false;
	}

	private boolean matchFineGrained(List<String> mods, List<String> sim_lines) {
		// bug fix: NPE (time2)
		if(mods == null){
			return false;
		}
		
		for(String line : sim_lines){ // traverse each line
			int matchFlag = 1; // 0: not match 1:match
			for (String mod : mods){
				//bug fix: time1: consider the mods order.
				int ind = line.indexOf(mod);
				if(ind >= 0){ // if can be found
					line = line.substring(ind + mod.length());
				}else{ //else, fail to match
					matchFlag = 0;
					break;
				}
//				if(!line.contains(mod)) {
//					// if not contain any mod, break;
//					matchFlag = 0;
//					break;
//				}
			}
			
			// check if the current line is matched
			if (matchFlag == 1){
				return true;
			}
		}
		return false; //default return
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

	public Pair<String, List<String>> matchSingleLine(SuspiciousPosition sc, String flag, List<String> matchedFlags) throws IOException {
		// count 
		parsedLinesCnt ++;
		// this is for debugging print.
//		String str = "Parsed Lines Count: " + parsedLinesCnt 
//				+ " : " + sc.classPath + ":" + sc.lineNumber 
//				+ "\nabsolutePath:" + dp.srcPath + sc.classPath.replace(".", "/") + ".java";
//		print(str);
//		writeStringToFile(extraLog, str + "\n", true);
		
		// TODO: this judge branch can be moved before SuspCodeNode scn1 = parseSuspiciousCode(sc);
		// 		which may save some time.
		// (fix bug) if in matched. return "". else return "null_ast" str.
		if (sc.lineNumber <= this.matchedEndLineNo &&
				sc.lineNumber >= this.matchedStartLineNo){
			return new Pair<String, List<String>>("", Arrays.asList("",""));
		}
		
		// reset dp for each line.
		this.dp.reset(this.dpPath, this.projId);
		
		// get the AST node for the line
		SuspCodeNode scn1 = parseSuspiciousCode(sc);
		
		if (scn1 == null){
			String javaFilePath = fullPath + ".java";
			String results = ShellUtils.shellRunForFileRead(Arrays.asList(
				"cat " + javaFilePath + " | sed -n " + sc.lineNumber + "p" ), buggyProject);
			// bug fix: closure 2:com.google.javascript.jscomp.TypeCheck_1575-1576 else branch with many \n.
			results = results.trim();
			
			if (results.equals("")){
				results = "empty line";
			}
			
			// bug fix: closure 2. add fixed_line that cannot be parsed!
			if(flag.equals("fixed_code")){
				for(String type : Arrays.asList("ori","0","1")){
//					print("type:"+type);
					if (patchLinesMap.containsKey(sc.lineNumber)){
						Map<String, String> valueMap = patchLinesMap.get(sc.lineNumber); //get current valueMap
						valueMap.put(type, results);
						patchLinesMap.put(sc.lineNumber, valueMap); // refresh/update the valueMap
					}
				}
			}
			
			return new Pair<String, List<String>>(results, Arrays.asList(results,results));		
		}
		
		ITree scan = scn1.suspCodeAstNode; 
		
		// get clazz and super clazz
		Pair<String, String> clazzAndSuper = getClazzSuperMethod(sc.classPath, scan, flag);
		String clazz = clazzAndSuper.getFirst();
		String superClazz = clazzAndSuper.getSecond();
		
		// first initialze a FixTemplate
		FixTemplate ft = null;
		ft = new ExpressionReplacer();
		ft.setSuspiciousCodeStr(scn1.suspCodeStr);
		ft.setSuspiciousCodeTree(scan); //scn1.suspCodeAstNode
		if (scn1.javaBackup == null) ft.setSourceCodePath(dp.srcPath);
		else ft.setSourceCodePath(dp.srcPath, scn1.javaBackup);
		
		// parse classpath.java
		SuspiciousCodeParser scp = new SuspiciousCodeParser();
		String suspiciousJavaFile = sc.classPath.replace(".", "/") + ".java";
		String filePath = this.dp.srcPath + suspiciousJavaFile;
		scp.parseJavaFile(new File(filePath));
		int currentStartLineNo = scp.getUnit().getLineNumber(scan.getPos());
		int currentEndLineNo=  scp.getUnit().getLineNumber(scan.getPos()+ scan.getLength());
		
		// exclude repeated ast match
		// bug fix: (math81) change this.matchedEndLineNo >= currentStartLineNo into this.matchedEndLineNo >= currentEndLineNo
		if(this.matchedStartLineNo <= currentStartLineNo && 
				this.matchedEndLineNo >= currentEndLineNo){
			return new Pair<String, List<String>>("", Arrays.asList("",""));
		}else{
			// fix bug: sometimes the line number may match a if() ast that includes
			// even the before ast (thus causing repetition). 
			// In this situation, we want to exlude it.
			// P.S. after re-debugging by inserting a breakpoint here (math 81), I remember that when the line is a "}", it will correspond to a whole if() block. Therefore we need to exclude this situation.
			if (this.matchedStartLineNo != 0 && this.matchedEndLineNo != 0){
				if (currentStartLineNo <= this.matchedStartLineNo &&
						currentEndLineNo >= this.matchedEndLineNo){
					// TODO: change "" to null
					return new Pair<String, List<String>>("", Arrays.asList("",""));
				}
			}
		}
		this.matchedStartLineNo = currentStartLineNo;
		this.matchedEndLineNo = currentEndLineNo;
		
		// get all variables
		List<ITree> suspVars = new ArrayList<>();
		// FIXME: identifySuspiciousVariables method should be modified for my purpose 
		ContextReader.identifySuspiciousVariables(scan, suspVars, new ArrayList<String>());
		ContextReader.readAllVariablesAndFields(scan, ft.allVarNamesMap, ft.varTypesMap, ft.allVarNamesList, ft.sourceCodePath, ft.dic);
		
		// now, find all variables in patchLines (rather than the whole ast code!)
		// this will save much effort, I suppose.
		// now, I change my mind, I can use ContextReader.identifySuspiciousVariables to do this thing.
		
		// sort the suspVars list
		Collections.sort(suspVars, new Comparator<ITree>() {
			@Override
			public int compare(ITree  o1, ITree o2) {
				// if clazz1 greater than 2, compareTo return 1. 
				if(o1.getPos() >= o2.getPos()){
					return 1;
				} else{
					return -1;
				}
			}
		});
		
		// first identify whether xxclass.var exists in ft.varTypesMap:
		// chart 2 & math 81: only have this.var or var.
		String before_match = scn1.suspCodeStr; // before match
		
		// the clazzInstanceMap is to avoid repeated javaFileParsing for getting superClazz or clazz name. (when there are more than one variable mapping strategy)
		Map<String, String> clazzInstanceMap = new HashMap<>();
		String mappedCodeStr = variableMapping(ft, suspVars, scan, superClazz, 
				clazz, flag, currentStartLineNo, currentEndLineNo,clazzInstanceMap);
		// TODO: this can be uncommented.
//		print(before_match);
//		print("variable mapping 0 :\n" + mappedCodeStr);
//		print("");
		
		String mappedCodeStr1 = variableMapping1(ft, suspVars, scan, superClazz, 
				clazz, flag, currentStartLineNo, currentEndLineNo,clazzInstanceMap);
//		print(before_match);
//		print("variable mapping 1 :\n" + mappedCodeStr1);
//		print("");

		// set matchedFlag
		if(!before_match.equals(mappedCodeStr)){
			matchedFlags.set(0, "matched");
		}
		if(!before_match.equals(mappedCodeStr1)){
			matchedFlags.set(1, "matched");
		}
		
		// for ori patch lines
		// refactoring: set patchLinesMap values for fixed code snippets.
		String[] lines = before_match.split("\n");
		int linesNo = lines.length;
		int linesNo2 = currentEndLineNo - currentStartLineNo + 1;
		if(flag == "fixed_code" && linesNo == linesNo2){
			int cnt = 0;
			for (int lineNo = currentStartLineNo; lineNo <= currentEndLineNo; lineNo ++){
				if (patchLinesMap.containsKey(lineNo)){
					Map<String, String> valueMap = patchLinesMap.get(lineNo); //get current valueMap
					valueMap.put("ori", lines[cnt].trim());
					patchLinesMap.put(lineNo, valueMap); // refresh/update the valueMap
				}
				cnt ++;
			}
		}
		
		return new Pair<String, List<String>>(before_match, Arrays.asList(mappedCodeStr,mappedCodeStr1));
	}
	
	/**
	 * A variable mapping strategy: mapping all variables found in the code snippet.
	 * @param ft
	 * @param suspVars
	 * @param scan
	 * @param superClazz
	 * @param clazz
	 * @return
	 * @throws IOException
	 */
	private String variableMapping(FixTemplate ft, List<ITree> suspVars, ITree scan, 
				String superClazz, String clazz, String flag, 
				int currentStartLineNo, int currentEndLineNo,
				Map<String, String> clazzInstanceMap) throws IOException {
		// code improve: add clazz and superClazz (chart3)
		clazzInstanceMap.put(clazz, superClazz);			
		
		int startPosScan = scan.getPos();
		int endPosScan = startPosScan + scan.getLength();
		String mappedCodeStr = ""; // code str after variable mapping.
		int tmpStartPosScan = startPosScan;
		
		// modifiy ft.varTypesMap
		Map<String, String> varNewTypesMap = new HashMap<>();
		varNewTypesMap.putAll(ft.varTypesMap);
		for(Map.Entry<String, String> entry : ft.varTypesMap.entrySet()){
			String var = entry.getKey();
			String type = entry.getValue();
			if(SuspiciousCodeParser.isClass(type)){ // if is a class
				if(clazzInstanceMap.containsKey(type)){ // already in map
					varNewTypesMap.put(var, clazzInstanceMap.get(type));
				}else{
					// find its super class
					String clazzOrSuper = getClazzOrSuper(type);
					clazzInstanceMap.put(type, clazzOrSuper);
					varNewTypesMap.put(var, clazzOrSuper);
				}
			}else if(type.equals("Integer")){
				varNewTypesMap.put(var, "int"); //change Integer to int
			}else{
				// do nothing.
				varNewTypesMap.put(var, type + "Var"); //add Var, e.g., int to intVar
			}
		}

		// set values of modMap0 only when "flag" is "fixed_code"
		if (flag.equals("fixed_code")){
			for(Map.Entry<Integer,List<String>> entry : modMap0.entrySet()){
				int lineNo = entry.getKey();
				List<String> mods = entry.getValue();
				List<String> mapped_mods = new ArrayList<>();
				for (String mod : mods){
					if (varNewTypesMap.containsKey(mod)){ // if contains such var.
						mapped_mods.add(varNewTypesMap.get(mod));//put mapped_var
					}else{
						mapped_mods.add(mod);//else:put the original var/mod
					}
				}
				modMap0.put(lineNo, mapped_mods);
			}
		}
		
		
		for (ITree var : suspVars){
			// init
			String mappedVar = ""; // just for replace original varibale with mappedVar
			int startPos = var.getPos();
			int endPos =  startPos + var.getLength();

			// simple check (if out of bound)
			if (startPos < startPosScan || endPos > endPosScan){
				writeStringToFile(extraLog, var.getLabel() + "is out of index! \n",true);
				continue;
			}

			// code part 1
			String codePart1 = ft.getSubSuspiciouCodeStr(tmpStartPosScan, startPos);
			mappedCodeStr += codePart1; // first add unchanged code part.

			String label = var.getLabel();
			// bug fix: chart3: "Name:iteratoris not in ft.varTypesMap! "
			if (label.contains(":")){
				label = label.split(":")[1];
			}

			// code improve: more complicated var checking and var mapping.
			if(label.contains(".")){ //if var is : this.var || var.length || varA.varB.length
				// bug fix: change "." into "\\." when spliting
				String[] subLabels = label.split("\\.");
				for(String subLabel : subLabels){ // traverse subLabels
					if(varNewTypesMap.containsKey(subLabel)){
						mappedVar += varNewTypesMap.get(subLabel) + "."; // map variable into <xxxtype>Var
					}else if(varNewTypesMap.containsKey("this." + subLabel)){
						mappedVar += varNewTypesMap.get("this." + subLabel) + "."; // map variable into <xxxtype>Var
					}else{
						mappedVar += subLabel + ".";
					}
				}
				int len = mappedVar.length();
				mappedVar = mappedVar.substring(0,len-1);  // wipe out last additional "."
			}else{ // condition: var without "."
				if(varNewTypesMap.containsKey(label)){
					mappedVar = varNewTypesMap.get(label); // map variable into <xxxtype>Var
				}else if(varNewTypesMap.containsKey("this." + label)){
					mappedVar = varNewTypesMap.get("this." + label); // map variable into <xxxtype>Var
				}else{
					// bug fix: comment continue;
					mappedVar = label;
					writeStringToFile(extraLog, var.getLabel() + "is not in ft.varTypesMap! \n",true);
//					continue;
				}
			}
			mappedCodeStr += mappedVar; // add mapped var
			tmpStartPosScan = endPos; // change start pos. (because mapped code str already add varTypeMatched.
		}
		mappedCodeStr += ft.getSubSuspiciouCodeStr(tmpStartPosScan, endPosScan);// add last rest code str
		
		// replace this.
		if (mappedCodeStr.contains("this.")){
			if (superClazz != null ){
				mappedCodeStr = mappedCodeStr.replace("this.", superClazz + ".");
			}else if (clazz != null){
				mappedCodeStr = mappedCodeStr.replace("this.", clazz + ".");
			}else{
				print("cannot find both class and super class.");
			}

		}
		
		// refactoring: set patchLinesMap values for fixed code snippets.
		String[] lines = mappedCodeStr.split("\n");
		int linesNo = lines.length;
		int linesNo2 = currentEndLineNo - currentStartLineNo + 1;
		if(flag == "fixed_code" && linesNo == linesNo2){
			int cnt = 0;
			for (int lineNo = currentStartLineNo; lineNo <= currentEndLineNo; lineNo ++){
				if (patchLinesMap.containsKey(lineNo)){
					Map<String, String> valueMap = patchLinesMap.get(lineNo); //get current valueMap
					valueMap.put("0", lines[cnt].trim());
					patchLinesMap.put(lineNo, valueMap); // refresh/update the valueMap
				}
				cnt ++;
			}
		}
		
		return mappedCodeStr;
	}
	
	private String getClazzOrSuper(String type) throws IOException {
		
		String[] cmd = {"/bin/sh","-c", " find " 
				+ dp.srcPath + " -name " + type + ".java"
		};
		// e.g., find /home/apr/d4j/Chart/Chart_1/source/ -name AbstractCategoryItemRenderer.java
		String result = ShellUtils.shellRun2(cmd);

		// bug fix: closure89. there are two results when type is "IRFactory"
		String[] results = result.replace("\n","").split("\n");// bug fix: chart3 TimeSeries with \n
		if(results.length > 1){
			return type; // in this situation, return the type directly! TODO maybe there are better solutions in the future.
		}
		
		// bug fix: chart3: propertyChangeSupport is a class, but I cannot find it... And the result is ""
		if (result.trim().equals("")){
//			print("The cmd result is null in getClazzOrSuper() for " + type);
			// bug fix: change null to type
			return type;
		}
		
		SuspiciousCodeParser scp2 = new SuspiciousCodeParser();
		String filePath2 = result.trim();
		ITree rootTree = scp2.getRootTree(new File(filePath2));
		for(ITree itree : rootTree.getChildren()){
			if(Checker.isTypeDeclaration(itree.getType())){		
				// if is class declaration, get class and super class name for "type<which is a class type>"
				String[] clazzStrList = itree.toString().split(",");
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
				if(superClazz != null){
					return superClazz;
				}else if(clazz != null){
					return clazz;
				}else{
					print("error occurs in getClazzOrSuper(String type)");
					return type;
				}
			}
		}
		
		return null;
	}

	/**
	 * The second variable mapping strategy: only mapping class instances.
	 * @param ft
	 * @param suspVars
	 * @param scan
	 * @param superClazz
	 * @param clazz
	 * @return
	 * @throws IOException
	 */
	private String variableMapping1(FixTemplate ft, List<ITree> suspVars, ITree scan, 
				String superClazz, String clazz, String flag, 
				int currentStartLineNo, int currentEndLineNo,
				Map<String, String> clazzInstanceMap) throws IOException {
		
		int startPosScan = scan.getPos();
		int endPosScan = startPosScan + scan.getLength();
		String mappedCodeStr = ""; // code str after variable mapping.
		int tmpStartPosScan = startPosScan;
		
		// modifiy ft.varTypesMap
		Map<String, String> varNewTypesMap = new HashMap<>();
		varNewTypesMap.putAll(ft.varTypesMap);
		for(Map.Entry<String, String> entry : ft.varTypesMap.entrySet()){
			String var = entry.getKey();
			String type = entry.getValue();
			if(SuspiciousCodeParser.isClass(type)){
				if(clazzInstanceMap.containsKey(type)){ // already in map
					varNewTypesMap.put(var, clazzInstanceMap.get(type));
				}else{
					// find its super class
					String clazzOrSuper = getClazzOrSuper(type);
					clazzInstanceMap.put(type, clazzOrSuper);
					varNewTypesMap.put(var, clazzOrSuper);
				}
			}
		}
		
		// set values of modMap0 only when "flag" is "fixed_code"
		if (flag.equals("fixed_code")){
			for(Map.Entry<Integer,List<String>> entry : modMap1.entrySet()){
				int lineNo = entry.getKey();
				List<String> mods = entry.getValue();
				List<String> mapped_mods = new ArrayList<>();
				for (String mod : mods){
					if (varNewTypesMap.containsKey(mod)){ // if contains such var.
						mapped_mods.add(varNewTypesMap.get(mod));//put mapped_var
					}else{
						mapped_mods.add(mod);//else:put the original var/mod
					}
				}
				modMap1.put(lineNo, mapped_mods);
			}
		}

		for (ITree var : suspVars){
			// init
			String mappedVar = ""; // just for replace original varibale with mappedVar
			int startPos = var.getPos();
			int endPos =  startPos + var.getLength();

			// simple check (if out of bound)
			if (startPos < startPosScan || endPos > endPosScan){
				writeStringToFile(extraLog, var.getLabel() + "is out of index! \n",true);
				continue;
			}

			// code part 1
			String codePart1 = ft.getSubSuspiciouCodeStr(tmpStartPosScan, startPos);
			mappedCodeStr += codePart1; // first add unchanged code part.

			String label = var.getLabel();
			// bug fix: chart3: "Name:iteratoris not in ft.varTypesMap! "
			if (label.contains(":")){
				label = label.split(":")[1];
			}

			// code improve: more complicated var checking and var mapping.
			if(label.contains(".")){ //if var is : this.var || var.length || varA.varB.length
				// bug fix: change "." into "\\." when spliting
				String[] subLabels = label.split("\\.");
				for(String subLabel : subLabels){ // traverse subLabels
					if(varNewTypesMap.containsKey(subLabel)){ //if in var-type map
						String type = varNewTypesMap.get(subLabel);
						//if in clazzIns map
						// bug fix: 1)change containsKey into containsValue (as the type is already mapped)
						//          2) change clazzInstanceMap.get(type) into type.
						if(clazzInstanceMap.containsValue(type)){
							mappedVar += type + "."; // map clazz instance into its class name or super class name 
						}else{ // bug fix: add the else branch to avoid empty mappedVar (chart 2, similar code:/home/apr/d4j/Chart/Chart_2/source/org/jfree/chart/util/ArrayUtilities.java-184-184)
							mappedVar += subLabel + ".";
						}
					}else if(varNewTypesMap.containsKey("this." + subLabel)){
						// bug fix: time1: add this branch (learning from varmapping 0.
						String type = varNewTypesMap.get("this." + subLabel);
						if(clazzInstanceMap.containsValue(type)){
							mappedVar += type + "."; // map clazz instance into its class name or super class name 
						}else{ 
							mappedVar += subLabel + ".";//do nothing
						}
					}else{ // bug fix: add the rest
						mappedVar +=  subLabel + ".";
					}
				}
				int len = mappedVar.length();
				if (len > 0){ // a simple check
					mappedVar = mappedVar.substring(0,len-1);  // wipe out last additional "."
				}else{
					writeStringToFile(extraLog, "mappedVar is empty!", true);
				}
			}else{
				//bug fix:(time1) consider var (when it's a classIns)
				if(varNewTypesMap.containsKey(label)){ //if in var-type map
					String type = varNewTypesMap.get(label);
					//if in clazzIns map
					if(clazzInstanceMap.containsValue(type)){
						mappedVar = clazzInstanceMap.get(type); // map clazz instance into its class name or super class name 
					}else{
						mappedVar = label;
					}
				}else if(varNewTypesMap.containsKey("this." + label)){
					// bug fix: time1: add this branch (learning from varmapping 0.
					String type = varNewTypesMap.get("this." + label);
					if(clazzInstanceMap.containsValue(type)){
						mappedVar = type; // map clazz instance into its class name or super class name 
					}else{ 
						mappedVar = label;//do nothing
					}
				}else{ // bug fix: add the rest
					mappedVar =  label;
				}
			}
			mappedCodeStr += mappedVar; // add mapped var
			tmpStartPosScan = endPos; // change start pos. (because mapped code str already add varTypeMatched.
		}
		mappedCodeStr += ft.getSubSuspiciouCodeStr(tmpStartPosScan, endPosScan);// add last rest code str
		
		// replace this.
		if (mappedCodeStr.contains("this.")){
			if (superClazz != null ){
				mappedCodeStr = mappedCodeStr.replace("this.", superClazz + ".");
			}else if (clazz != null){
				mappedCodeStr = mappedCodeStr.replace("this.", clazz + ".");
			}else{
				print("cannot find both class and super class.");
			}
		}
		
		// refactoring: set patchLinesMap values for fixed code snippets.
		String[] lines = mappedCodeStr.split("\n");
		int linesNo = lines.length;
		int linesNo2 = currentEndLineNo - currentStartLineNo + 1;
		if(flag == "fixed_code" && linesNo == linesNo2){
			int cnt = 0;
			for (int lineNo = currentStartLineNo; lineNo <= currentEndLineNo; lineNo ++){
				if (patchLinesMap.containsKey(lineNo)){
					Map<String, String> valueMap = patchLinesMap.get(lineNo); //get current valueMap
					valueMap.put("1", lines[cnt].trim());
					patchLinesMap.put(lineNo, valueMap); // refresh/update the valueMap
				}
				cnt ++;
			}
		}
		
		return mappedCodeStr;
	}

	// get class name, super class, and method name string
	private Pair<String, String> getClazzSuperMethod(String classPath, ITree scan, String flag) {
		String superClazz = null;
		String clazz = null;
		ITree previousParent = null;
		
		// First: get class and super class (if exists) name 
		ITree parent = scan;
		// TODO: to test whether 1056 TimeSeries copy = (TimeSeries) super.clone(); is typeDecl
		//bug fix: math5
		while(parent != null){
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
			
//			java.lang.NullPointerException
			
			// get class and super class name.
//			int type = parent.getType();
			if(Checker.isTypeDeclaration(parent.getType())){
				// print(parent.toString());
				String[] clazzStrList = parent.toString().split(",");
				
				for (String clazzStr : clazzStrList){
					if(clazzStr.contains("@@SuperClass:")){
						superClazz = clazzStr.split(":")[1];
					}
					if(clazzStr.contains("ClassName:")){
						clazz = clazzStr.split(":")[1];
					}
				}
			}
			previousParent = parent; // try to get JavaFile
			parent = parent.getParent();
		}
		
		// get package.
//		parent = parent.getParent();
//		ITree package_info = parent.getChild(0);
		
		ITree package_info = previousParent.getChild(0);
		if(flag == "fixed_code"){
			this.patchPackage = package_info.getLabel();
//			print("patchPackage:"+patchPackage);
		}else{
			this.matchPackage = package_info.getLabel();
//			print("matchPackage:"+matchPackage);
		}
		
		if(clazz == null){
			String[] classpaths = classPath.split("\\.");
			clazz = classpaths[classpaths.length - 1];
		}
		if(superClazz == null){
			String[] classpaths = classPath.split("\\.");
			superClazz = classpaths[classpaths.length - 1];
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
				print(file.getName() + " exists, and now clear it.");
			}
		}
		
		File file = new File(extraLog); // e.g., ./match-log/Chart/3/extra-info.log
		if (file.exists()){
			file.delete();
//			print(file.getName() + " exists, and now clear it at the beginning of the process.");
		}
	}

	/**
	 * use print() to replace System.out.println()
	 * @param str
	 */
	public static void print(String str){
		System.out.println(str);
	}
}

class ValueComparator implements Comparator<String> {
    Map<String, Triple<String, Integer, Integer>> base;

    public ValueComparator(Map<String, Triple<String, Integer, Integer>> base) {
        this.base = base;
    }

    // Note: this comparator imposes orderings that are inconsistent with
    // equals.
    public int compare(String a, String b) {
        if (base.get(a).getSecond() >= base.get(b).getSecond()) {
            return 1;
        } else {
            return -1;
        } // returning 0 would merge keys
    }
}
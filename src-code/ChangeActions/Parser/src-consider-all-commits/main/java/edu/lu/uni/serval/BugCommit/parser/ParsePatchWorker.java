package edu.lu.uni.serval.BugCommit.parser;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.github.gumtreediff.tree.ITree;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import edu.lu.uni.serval.Configuration;
import edu.lu.uni.serval.BugCommit.BugDiff;
import edu.lu.uni.serval.diffentry.DiffEntryHunk;
import edu.lu.uni.serval.gumtree.regroup.HierarchicalActionSet;
import edu.lu.uni.serval.utils.FileHelper;

public class ParsePatchWorker extends UntypedActor {
	
	private String outputPath;
	private int bugHunkSize, fixHunkSize;
	
	public ParsePatchWorker(String outputPath, int bugHunkSize, int fixHunkSize) {
		this.outputPath = outputPath;
		this.bugHunkSize = bugHunkSize;
		this.fixHunkSize = fixHunkSize;
	}

	public static Props props(final String outputPath, final int bugHunkSize, final int fixHunkSize) {
		return Props.create(new Creator<ParsePatchWorker>() {

			private static final long serialVersionUID = -7615153844097275009L;

			@Override
			public ParsePatchWorker create() throws Exception {
				return new ParsePatchWorker(outputPath, bugHunkSize, fixHunkSize);
			}
			
		});
	}

	@Override
	public void onReceive(Object msg) throws Throwable {
		if (msg instanceof WorkMessage) {
			WorkMessage workMsg = (WorkMessage) msg;
			int workerId = workMsg.getId();
			
			List<MessageFile> msgFiles = workMsg.getMsgFiles();
			List<String> patchCommitIds = new ArrayList<>();
			
			// print current worker id, and its msgFiles size.
			System.out.println("\nworkerId: " + workerId + "\nmsgFiles.size: " + msgFiles.size());
			
			int numOfPatches = 0;
			int numOfHunks = 0;
			int numOfDiff = 0;
			int zeroG = 0;
			int overRap = 0;
			
			Map<DiffEntryHunk, List<HierarchicalActionSet>> allPatches = new HashMap<>();
			// init patchCommitId
			String patchCommitId = null;
			// this is to collect fix patterns/change actions/CCI(code change instructions)
			Map<String, Pair<Integer, List<String>>> opCntMap = new HashMap<>();
			
			// collect D4J bug CCI
			List<String> CCIList = new ArrayList<>();
			
			for (MessageFile msgFile : msgFiles) {
				File revFile = msgFile.getRevFile();
				File prevFile = msgFile.getPrevFile();
				File diffentryFile = msgFile.getDiffEntryFile();
				
				PatchParser parser =  new PatchParser();
//				System.out.format("\n\ncurrent revFile to be parsed: %s\n", revFile);
				
				final ExecutorService executor = Executors.newSingleThreadExecutor();
				// schedule the work
				File cciFile = new File(Configuration.AllCCI + revFile.getName());
				final Future<?> future = executor.submit(new RunnableParser(cciFile, msgFile, prevFile, revFile, diffentryFile, parser, bugHunkSize, fixHunkSize, msgFile.getId()));
				
				try {
					// wait for task to complete
					future.get(Configuration.TIMEOUT_THRESHOLD, TimeUnit.SECONDS);
					
					if (msgFile.getId() != null){  //fix a bug! original : else{   // for buggy programs in D4J
						List<HierarchicalActionSet> hASList = parser.getActionSets();
						analyzePatches3(hASList, msgFile.getProj(), msgFile.getId(), CCIList);  //List<String> d4jPatchStrList =
						
						continue; //skip next step.
					}
					
					patchCommitId = revFile.getName().substring(0, Configuration.commitIdLength);
					
					// only get commit history 
					if (msgFile.getId() == null 
							&& msgFile.getCommitTime().compareTo(Configuration.commitTime) < 0 ){
//						File cciFile = new File(Configuration.AllCCI + revFile.getName());
						if(cciFile.exists()){//Configuration.commitNoMap.get(patchCommitId) == Configuration.commitExecutedNoMap.get(patchCommitId
							readCCI(opCntMap, cciFile, patchCommitId);
							continue;
							// check record
//							String[] revFileNames = FileHelper.readFile(cciFile).split("\n");
//							for(String revName : revFileNames){
//								if (revName.trim().equals(revFile.getName())){//if already write
//									readCCI(opCntMap, cciFile, patchCommitId);
//									continue;
//								}
//							}
//							
//							if( ! Configuration.commitExecutedNoMap.containsKey(patchCommitId)){
//								readCCI(opCntMap, cciFile, patchCommitId);
//								continue;
//							}
						}else{ // not exist
//							Configuration.commitExecutedNoMap.put(patchCommitId, 0); // then execute next steps.
						}
						
						Map<DiffEntryHunk, List<HierarchicalActionSet>> patches = parser.getPatches();
//						Map<DiffEntryHunk, List<HierarchicalActionSet>> patches = msgFile.getPatches();
						
						if (patches.size() > 0) {
							if (!patchCommitIds.contains(patchCommitId)) {
								patchCommitIds.add(patchCommitId);
							}else{
//								System.out.format("Repeated PatchCommit: %s\n", patchCommitId);
							}
							
							// apr
							analyzePatches2(patches, patchCommitId, opCntMap, revFile.getName());
							
							// add executed patchCommitId
//							if(Configuration.commitExecutedNoMap.containsKey(patchCommitId)){
//								int number = Configuration.commitExecutedNoMap.get(patchCommitId);
//								Configuration.commitExecutedNoMap.put(patchCommitId, number + 1);
//								
//								System.out.format("repeated wrote patchCommitId %s \n", patchCommitId);
//							}else{
//								Configuration.commitExecutedNoMap.put(patchCommitId, 1);
//								System.out.println("Execute: Configuration.commitExecutedNoMap.put(patchCommitId, 1);");
//							}
						}
						allPatches.putAll(patches);
						numOfHunks += parser.hunks;
						numOfDiff += parser.diffs;
						zeroG += parser.zeroG;
						overRap += parser.overRap;
					}
//					else{
//						System.out.format("\n\ncurrent revFile to be parsed: %s  (size: %s)\n", revFile, parser.getPatches().size());
//						System.out.println("========================== Debug here =======================================");
//					}
				} catch (TimeoutException e) {
					future.cancel(true);
					System.err.println("#Timeout: " + revFile.getName());
//					e.printStackTrace();
				} catch (InterruptedException e) {
					System.err.println("#TimeInterrupted: " + revFile.getName());
//					e.printStackTrace();
				} catch (ExecutionException e) {
					System.err.println("#TimeAborted: " + revFile.getName());
//					e.printStackTrace();
				} finally {
					executor.shutdownNow();
				}
			}
			
			//check
//			System.out.println("Apr print: commitExecutedNoMap size:" + Configuration.commitExecutedNoMap.size());
//			for(Map.Entry<String, Integer> entry : Configuration.commitExecutedNoMap.entrySet()){
//				String id = entry.getKey();
//				if(Configuration.commitExecutedNoMap.get(id) != Configuration.commitNoMap.get(id)){
//					System.out.format("Apr print: commitExecutedNoMap wrong %s v.s. %s\n", Configuration.commitExecutedNoMap.get(id), Configuration.commitNoMap.get(id));
//				}
//			}
			
			System.out.println("Apr print: Selected bug-fix commits: " + patchCommitIds.size());
			
			numOfPatches = calculatePatches(allPatches);
			

			WorkerReturnMessage workerMsg = new WorkerReturnMessage(numOfPatches, this.stmtMaps, this.elementsMaps, opCntMap, CCIList);
			workerMsg.diffs = numOfDiff;
			workerMsg.hunks = numOfHunks;
			workerMsg.zeroG = zeroG;
			workerMsg.overRap = overRap;
			workerMsg.abstractElementsMaps = this.abstractElementsMaps;
			workerMsg.expElementMaps = this.expElementMaps;
			workerMsg.expMaps = this.expMaps;
			workerMsg.stmtBuggyElementTypesMaps = this.stmtBuggyElementTypesMaps;
			workerMsg.patchCommitIds = patchCommitIds;
			workerMsg.pureDelRootNodes = this.pureDelRootNodes;
			
			outputExpDepthData(this.outputPath + "examples/expDepth/" + workerId + ".txt", allPatches);

			FileHelper.outputToFile(this.outputPath + "examples/TypeChanges/" + workerId + ".txt", typeChangeExamples, false);
			typeChangeExamples.setLength(0);
			FileHelper.outputToFile(this.outputPath + "examples/AddModifiers/" + workerId + ".txt", addModifierExamples, false);
			addModifierExamples.setLength(0);
			FileHelper.outputToFile(this.outputPath + "examples/DelModifiers/" + workerId + ".txt", delModifierExamples, false);
			delModifierExamples.setLength(0);
			FileHelper.outputToFile(this.outputPath + "examples/UpdModifiers/" + workerId + ".txt", updateModifierExamples, false);
			updateModifierExamples.setLength(0);
			FileHelper.outputToFile(this.outputPath + "examples/UpdIdentifiers/" + workerId + ".txt", updateIdentifierExamples, false);
			updateIdentifierExamples.setLength(0);
			FileHelper.outputToFile(this.outputPath + "examples/UpdMethodOrClassIdentifiers/" + workerId + ".txt", updateMethodOrClassIdentifierExamples, false);
			updateMethodOrClassIdentifierExamples.setLength(0);
			FileHelper.outputToFile(this.outputPath + "examples/StmtTypeChanges/" + workerId + ".txt", stmtTypeChangeExamples, false);
			stmtTypeChangeExamples.setLength(0);
			
			this.getSender().tell(workerMsg, getSelf());
		} else {
			unhandled(msg);
		}
	}
	
	private void readCCI(Map<String, Pair<Integer, List<String>>> opCntMap, File cciFile, String patchCommitId) {
		String cciLines = FileHelper.readFile(cciFile);
		String[] cciOps = cciLines.split("\n");
		List<String> cciOpsList = new ArrayList<>();
		Collections.addAll(cciOpsList, cciOps);
		cciOpsList.add("");
		String cci = "";
		for (String cciOp : cciOpsList){
			if(cciOp.equals("")){
				//add cci
				if( ! opCntMap.containsKey(cci)) {
					opCntMap.put(cci, new Pair<>(1, Arrays.asList(patchCommitId)));
				}else{
					Pair<Integer, List<String>> pair = opCntMap.get(cci);
					int addCnt = pair.getFirst()+1;
					List<String> ids = new ArrayList<>();
					ids.addAll(pair.getSecond());
					ids.add(patchCommitId);
					opCntMap.put(cci, new Pair<>(addCnt, ids));
					
//					System.out.println("repeat");
				}
				cci = ""; // reinit
			}else{
				cci += cciOp + "\n";
			}
		}
		
		
		
	}

	/**
	 * This is to output the operations
	 * @param allPatches
	 * @param patchCommitId 
	 * @param opCntMap 
	 * @param revName 
	 */
	private List<String> analyzePatches2(Map<DiffEntryHunk, List<HierarchicalActionSet>> allPatches, String patchCommitId, Map<String, Pair<Integer, List<String>>> opCntMap, String revName) {
		// init && clear 
		List<String> strOpListAll = new ArrayList<>();
		String CCIPath = Configuration.CCIPath + patchCommitId + ".txt";
		String CCIPurePath = Configuration.CCIPurePath + patchCommitId + ".txt"; // only collect CII info.
		
		// fix : should be true, because one commit id may have more than one msgfile.
		FileHelper.outputToFile(CCIPath, "", true); // including CII, hunk and hAS info.
		FileHelper.outputToFile(CCIPurePath, "", true);
		
		// each hunk
		for(Map.Entry<DiffEntryHunk, List<HierarchicalActionSet>> entry:allPatches.entrySet()){
			DiffEntryHunk hunk = entry.getKey();
			List<HierarchicalActionSet> hASList = entry.getValue();
			
			// print hunk info 
			//System.out.println("hunk:\n" + hunk.toString() + "hASList: \n" + hASList.toString());
			FileHelper.outputToFile(CCIPath, "hunk:\n" + hunk.toString() + "\n\n\nhASList: \n" + hASList.toString() + "\n\n\n", true);
			
			for (HierarchicalActionSet hAS : hASList){
				// operations based on string
				String[] actLines = hAS.toString().split("\n");
//				System.out.println(actLines + "\n\n");
				int opCnt = 1;
				List<Op> opList = new ArrayList<>();
				int largestLevel = 0;
				
				for(String actLine : actLines){
					// is an op
					// print actLine info 
					// System.out.println("\n\nactLine:" + actLine+"\n\n");
					if (actLine.length() >= 3 && actLine.substring(0,3).equals("---")){
						Op op = new Op();
						
						// get level
						int levInd = 0;
						int level = 0;
						while(actLine.substring(levInd, levInd+3).equals("---")){
							levInd = levInd + 3;
							level ++;
						}
						op.setLevel(level);
						if (largestLevel <= level){
							largestLevel = level;
						}
						
						// get op
						String operator = actLine.split(" ")[0].substring(3*level);
						op.setOp(operator);

						// get opName
						op.setOpName("OP" + opCnt++);
						
						// get stmtType
						op.setStmtType(actLine.split(" ")[1].split("@@")[0]);
						
						opList.add(op);
					}else{ // not an op
						// just a print
						//System.out.println("not an op :" + actLine);
					}
				}
				
				// set parent and child op name
				int tmpCnt = 1;  // record op cnt
				String strOpList = "";
				for(Op op : opList){
					// set parent
					if (op.getLevel() == 1){ 
						op.setParentOpName("null"); // all 1 level have no parent
					}else{
						// smaller level
						int tmpCnt2 = tmpCnt;
						while (tmpCnt2 >= 2){ // fix: > to >=
							if(opList.get(tmpCnt2-2).getLevel() < op.getLevel()){
								op.setParentOpName("OP" + (tmpCnt2 - 1));
								break;
							}
							tmpCnt2 --;
						}
						if (op.getParentOpName() == null){
							op.setParentOpName("null");
						}
					}
					// set child 
					List<String> childOpNameList = new ArrayList<>();
					if (op.getLevel() == largestLevel || tmpCnt == opList.size()){  // debug
						childOpNameList.add("null");
						op.setChildOpNameList(childOpNameList); 
					}else{
						// may have more than one child.
						int tmpCnt2 = tmpCnt;
						while(tmpCnt2 < opList.size()){
							int minus = opList.get(tmpCnt2).getLevel() - op.getLevel();
							
							if(minus == 0){ // stop to search child.
								break;
							}else if(minus == 1){
								childOpNameList.add("OP" + (tmpCnt2 + 1));
							}
							tmpCnt2 ++;
						}
						if (childOpNameList.isEmpty()){
							childOpNameList.add("null"); // add null
						}
						op.setChildOpNameList(childOpNameList); 
					}
					// print op info 
					//System.out.println(op.getOpName() + " :  " + op.toString());
					strOpList += op.toString();
					tmpCnt++;
				}
				
				// print opStr info 
				//System.out.println("strOpList: \n" + strOpList);
				strOpListAll.add(strOpList);
				
				// get frequencies of fix patterns
				if( ! opCntMap.containsKey(strOpList)) {
					List<String> ids = new ArrayList<>();
					ids.add(patchCommitId);
					opCntMap.put(strOpList, new Pair<>(1, ids));
				}else{
					Pair<Integer, List<String>> pair = opCntMap.get(strOpList);
					int addCnt = pair.getFirst()+1;
					List<String> ids = new ArrayList<>();
					ids.addAll(pair.getSecond());
					ids.add(patchCommitId);
					opCntMap.put(strOpList, new Pair<>(addCnt, ids));
				}
				
				FileHelper.outputToFile(CCIPath, "CII:\n" + strOpList + "\n\n\n", true);
				FileHelper.outputToFile(CCIPurePath, strOpList + "\n\n", true);
				
//				if (new File(Configuration.AllCCI + patchCommitId).exists()){
//					System.out.format("patchCommitId %s write again.\n", patchCommitId);
//				}
				FileHelper.outputToFile(Configuration.AllCCI + revName, strOpList + "\n", true);
			}
//			System.out.println();
//			for (String str : strOpListAll){
//				FileHelper.outputToFile(this.outputPath + "CCI/" + patchCommitId + ".txt", str, true);
//			}
		}
		return strOpListAll;
		
	}
	
	/*
	 * regroup hASList
	 */
	private void analyzePatches3(List<HierarchicalActionSet> hASList, String proj, String id, List<String> CCIList) {
		// init && clear 
		for (HierarchicalActionSet hAS : hASList){
			// operations based on string
			// TODO: not sure if this is ok. just a temporary solution.
			String[] actLines = (hAS.toString()).split("\n");
			int opCnt = 1;
			List<Op> opList = new ArrayList<>();
			int largestLevel = 0;
			
			for(String actLine : actLines){
				if(actLine.length() > 4 && (actLine.substring(0,4).equals("INS ") || actLine.substring(0,4).equals("DEL ")
						|| actLine.substring(0,4).equals("UPD ") || actLine.substring(0,4).equals("MOV "))){
					Op op = new Op();
					op.setLevel(1);
					
					// get op
					String operator = actLine.split(" ")[0];
					op.setOp(operator);

					// get opName
					op.setOpName("OP" + opCnt++);
					
					// get stmtType
					op.setStmtType(actLine.split(" ")[1].split("@@")[0]);
					
					opList.add(op);
				}
				// is an op
				else if (actLine.length() >= 3 && actLine.substring(0,3).equals("---")){
					Op op = new Op();
					
					// get level
					int levInd = 0;
					int level = 0;
					while(actLine.substring(levInd, levInd+3).equals("---")){
						levInd = levInd + 3;
						level ++;
					}
					 
					level = level + 1;  // fix a bug!
					op.setLevel(level); //sepcial 1 for D4J bug
					if (largestLevel <= level){
						largestLevel = level;
					}
					
					// get op
					String operator = actLine.split(" ")[0].substring(3*(level-1)); // fix
					op.setOp(operator);

					// get opName
					op.setOpName("OP" + opCnt++);
					
					// get stmtType
					op.setStmtType(actLine.split(" ")[1].split("@@")[0]);
					
					opList.add(op);
				}else{ // not an op
					// just a print
					//System.out.println("not an op :" + actLine);
				}
			}
			
			// set parent and child op name
			int tmpCnt = 1;  // record op cnt
			String strOpList = "";
			for(Op op : opList){
				// set parent
				if (op.getLevel() == 1){ 
					op.setParentOpName("null"); // all 1 level have no parent
				}else{
					// smaller level
					int tmpCnt2 = tmpCnt;
					while (tmpCnt2 >= 2){ // fix: > to >=
						if(opList.get(tmpCnt2-2).getLevel() < op.getLevel()){
							op.setParentOpName("OP" + (tmpCnt2 - 1));
							break;
						}
						tmpCnt2 --;
					}
					if (op.getParentOpName() == null){
						op.setParentOpName("null");
					}
				}
				// set child 
				List<String> childOpNameList = new ArrayList<>();
				if (op.getLevel() == largestLevel || tmpCnt == opList.size()){  // debug
					childOpNameList.add("null");
					op.setChildOpNameList(childOpNameList); 
				}else{
					// may have more than one child.
					int tmpCnt2 = tmpCnt;
					while(tmpCnt2 < opList.size()){
						int minus = opList.get(tmpCnt2).getLevel() - op.getLevel();
						
						if(minus == 0){ // stop to search child.
							break;
						}else if(minus == 1){
							childOpNameList.add("OP" + (tmpCnt2 + 1));
						}
						tmpCnt2 ++;
					}
					if (childOpNameList.isEmpty()){
						childOpNameList.add("null"); // add null
					}
					op.setChildOpNameList(childOpNameList); 
				}
				// print op info 
				strOpList += op.toString();
				tmpCnt++;
			}
			// print opStr info 
			//System.out.println("strOpList: \n" + strOpList);
			CCIList.add(strOpList);
			
//			FileHelper.outputToFile(Configuration.BUGS + proj + "/" + id +"/CCI-info", 
//					"\n\nhASList: \n" + hASList.toString() + "\n\n"
//					+ "CCI:\n" + strOpList + "\n\n\n", true);
			FileHelper.outputToFile(Configuration.BUGS + proj + "/" + id +"/changeAction.txt", strOpList + "\n\n", true);
		}
	}

	private int calculatePatches(Map<DiffEntryHunk, List<HierarchicalActionSet>> allPatches) {
		int numOfPatches = 0;
		for (Map.Entry<DiffEntryHunk, List<HierarchicalActionSet>> entry : allPatches.entrySet()) {
			numOfPatches += entry.getValue().size();
		}
		return numOfPatches;
	}

	private StringBuilder patchesBuilder = new StringBuilder();
	/**
	 * <StmtType, <ActionType, Number>>.
	 */
	private Map<String, Map<String, Integer>> stmtMaps = new HashMap<>();
	/**
	 * <StmtType, <AbstractElement, Number>>
	 */
	private Map<String, Map<String, Integer>> abstractElementsMaps = new HashMap<>();
	/**
	 * <StmtType, <Expression, Number>>
	 */
	private Map<String, Map<String, Integer>> stmtBuggyElementTypesMaps = new HashMap<>();
	/**
	 * <StmtElement, <ActionType, Number>>.
	 */
	private Map<String, Map<String, Integer>> elementsMaps = new HashMap<>();
	/**
	 * <Exp, <expPart, Number>>.
	 */
	private Map<String, Map<String, Integer>> expElementMaps = new HashMap<>();
	/**
	 * <ParentExp, <subExp, Number>>
	 */
	private Map<String, Map<String, Integer>> expMaps = new HashMap<>();
	private int expId = 0;
	private List<String> expDepthList = new ArrayList<>();
	// added by apr
//	private int exp_cnt = 0;
	StringBuilder stmtTypeChangeExamples = new StringBuilder();
	boolean outputTypeChangeExp = false;
	boolean outputAddModifierExp = false;
	boolean outputDelModifierExp = false;
	boolean outputUpdModifierExp = false;
	boolean outputUpdIdentifierExp = false;
	boolean outputUpdMethodOrClassIdentifierExp = false;
	StringBuilder typeChangeExamples = new StringBuilder();
	StringBuilder addModifierExamples = new StringBuilder();
	StringBuilder delModifierExamples = new StringBuilder();
	StringBuilder updateModifierExamples = new StringBuilder();
	StringBuilder updateIdentifierExamples = new StringBuilder();
	StringBuilder updateMethodOrClassIdentifierExamples = new StringBuilder();
	int pureDelRootNodes = 0;

	/**
	 * Statements distribution in patches.
	 * 
	 * @param patches
	 */
	private void analyzePatches(Map<DiffEntryHunk, List<HierarchicalActionSet>> allPatches) {
		for (Map.Entry<DiffEntryHunk, List<HierarchicalActionSet>> entry : allPatches.entrySet()) {
			patchesBuilder.append(entry.getKey().getFile()).append("\n###Diff###\n").append(entry.getKey().toString()).append("\n###PATCH###\n");
			List<HierarchicalActionSet> patches = entry.getValue();
			for (HierarchicalActionSet patch : patches) {
				List<HierarchicalActionSet> subActions = patch.getSubActions();
				
				boolean deleteStmt = false;
				boolean insertStmt = false;
				int a = 0;
				for (HierarchicalActionSet subAction : subActions) {
					patchesBuilder.append(subAction.toString()).append("\n");
					String actionStr = subAction.getActionString();
					String actionType = actionStr.substring(0, 3);// UPD, INS, DEL, MOV
					String astNodeType = subAction.getAstNodeType();
					
					if (CodeNodes.stmtTypes.contains(astNodeType)) {
						addToMap(stmtMaps, astNodeType, actionType);
						
						if ("UPD".equals(actionType)) {
							if (astNodeType.endsWith("Declaration") && !astNodeType.equals("FieldDeclaration")) continue;
							List<HierarchicalActionSet> subSubActions = subAction.getSubActions();
							analyzeElementsAction(subSubActions, astNodeType); // buggy elements of statements.
						}
					} else {
//						System.err.println("STMT===" + actionStr);
					}
					
					if ("INS".equals(actionType)) {// && astNodeType.endsWith("Statement")
						insertStmt = true;
					}
					if ("DEL".equals(actionType)) {// && astNodeType.endsWith("Statement")
						deleteStmt = true;
						a ++;
					}
				}
				
				if (deleteStmt) {
					if (insertStmt) {
						stmtTypeChangeExamples.append(entry.getKey().getFile()).append("\n###Diff###\n").append(entry.getKey().toString()).append("\n");
						for (HierarchicalActionSet subAction : subActions) {
							String actionStr = subAction.getActionString();
							String actionType = actionStr.substring(0, 3);// UPD, INS, DEL, MOV
							String astNodeType = subAction.getAstNodeType();
							
							if (CodeNodes.stmtTypes.contains(astNodeType)) {
								if ("DEL".equals(actionType) || "INS".equals(actionType)) {
									if (astNodeType.endsWith("Declaration") && !astNodeType.equals("FieldDeclaration")) continue;
									List<HierarchicalActionSet> subSubActions = subAction.getSubActions();
									analyzeElementsAction(subSubActions, astNodeType); // buggy elements of statements.
								}
							}
						}
					} else {
						pureDelRootNodes += a;
					}
				}
				if (outputTypeChangeExp) {
					typeChangeExamples.append(entry.getKey().getFile()).append("\n###Diff###\n").append(entry.getKey().toString()).append("\n");
					outputTypeChangeExp = false;
				}
				if (outputAddModifierExp) {
					addModifierExamples.append(entry.getKey().getFile()).append("\n###Diff###\n").append(entry.getKey().toString()).append("\n");
					outputAddModifierExp = false;
				}
				if (outputDelModifierExp) {
					delModifierExamples.append(entry.getKey().getFile()).append("\n###Diff###\n").append(entry.getKey().toString()).append("\n");
					outputDelModifierExp = false;
				}
				if (outputUpdModifierExp) {
					updateModifierExamples.append(entry.getKey().getFile()).append("\n###Diff###\n").append(entry.getKey().toString()).append("\n");
					outputUpdModifierExp = false;
				}
				if (outputUpdIdentifierExp) {
					updateIdentifierExamples.append(entry.getKey().getFile()).append("\n###Diff###\n").append(entry.getKey().toString()).append("\n");
					outputUpdIdentifierExp = false;
				}
				if (outputUpdMethodOrClassIdentifierExp) {
					updateMethodOrClassIdentifierExamples.append(entry.getKey().getFile()).append("\n###Diff###\n").append(entry.getKey().toString()).append("\n");
					outputUpdMethodOrClassIdentifierExp = false;
				}
			}
		}
	}

	/**
	 * Elements distribution of statements in patches.
	 * @param actionSets
	 * @param parentNode
	 */
	private void analyzeElementsAction(List<HierarchicalActionSet> actionSets, String parentNode) {
		for (HierarchicalActionSet actionSet : actionSets) {
			String actionStr = actionSet.getActionString();
			String actionType  = actionStr.substring(0, 3);// UPD, INS, DEL, MOV
			String astNodeType = actionSet.getAstNodeType();
			
			if (CodeNodes.elements.contains(astNodeType)) {
				String node = parentNode + "_" + astNodeType;
				if (astNodeType.contains("Type")) {
					addToMap(this.abstractElementsMaps, parentNode + "_Type", actionType);
					if (actionType.equals("UPD")) {
						this.outputTypeChangeExp = true;
					}
				} else {
					addToMap(this.abstractElementsMaps, node, actionType);
					if (astNodeType.equals("Modifier")) {
						switch (actionType) {
						case "UPD":
							this.outputUpdModifierExp = true;
							break;
						case "INS":
							this.outputAddModifierExp = true;
							break;
						case "DEL":
							this.outputDelModifierExp = true;
							break;
						default:
							break;
						}
					}
				}
				addToMap(elementsMaps, node, actionType);
				if (actionType.equals("UPD") || actionType.equals("DEL")) {
					addToMap(stmtBuggyElementTypesMaps, parentNode, astNodeType);
				}
				
				if ("SingleVariableDeclaration".equals(astNodeType)) {
					List<HierarchicalActionSet> subActions = actionSet.getSubActions();
					for (HierarchicalActionSet subAction : subActions) {
						String subActionStr = actionSet.getActionString();
						String subActionType = subActionStr.substring(0, 3);// UPD, INS, DEL, MOV
						String subAstNodeType = subAction.getAstNodeType();
						
						if (CodeNodes.elements.contains(subAstNodeType)) {
							String subNode = parentNode + "_SingleVariableDeclaration_" + subAstNodeType;
							addToMap(elementsMaps, subNode, subActionType);
							if (subAstNodeType.contains("Type")) {
								addToMap(this.abstractElementsMaps, parentNode + "_SingleVariableDeclaration_Type", subActionType);
							} else {
								addToMap(this.abstractElementsMaps, subNode, subActionType);
							}
						} else if (CodeNodes.expressions.contains(subAstNodeType)) {
							addToMap(this.abstractElementsMaps, parentNode + "_SingleVariableDeclaration_Identifier", subActionType);
							analyzeExpressions(subAction, (++ expId) + "_" + astNodeType);
						} else {
							if (!"MethodDeclaration".equals(subAstNodeType) && !"TypeDeclaration".equals(astNodeType)) {
								if (CodeNodes.stmtTypes.contains(subAstNodeType)) {
//									addToMap(stmtMaps, subAstNodeType, subActionType);
									analyzeElementsAction(subAction.getSubActions(), subAstNodeType);
								} else if ("Block".equals(subAstNodeType)) {
									analyzeElementsAction(subAction.getSubActions(), subAstNodeType);
								} else {
//									System.err.println("ELEMENT===" + actionStr);
								}
							}
						}
						
						if (actionType.equals("UPD") || actionType.equals("DEL")) {
							addToMap(stmtBuggyElementTypesMaps, parentNode + "_SingleVariableDeclaration", subAstNodeType);
						}
					}
				} else if ("VariableDeclarationFragment".equals(astNodeType)) {
					int identifierNodePos = actionSet.getNode().getChild(0).getPos();
					List<HierarchicalActionSet> subActions = actionSet.getSubActions();
					for (HierarchicalActionSet subAction : subActions) {
						if (actionType.equals("UPD") || actionType.equals("DEL")) {
							addToMap(stmtBuggyElementTypesMaps, parentNode + "_VariableDeclarationFragment", subAction.getAstNodeType());
						}
						
						String subActionStr = subAction.getActionString();
						String subActionType = subActionStr.substring(0, 3);// UPD, INS, DEL, MOV
						String subAstNodeType = subAction.getAstNodeType();

						if (identifierNodePos == subAction.getNode().getPos()) {
							addToMap(elementsMaps, parentNode + "_VariableDeclarationFragment_Identifier", subActionType);
							addToMap(this.abstractElementsMaps, parentNode + "_VariableDeclarationFragment_Identifier", subActionType);
							if (actionType.equals("UPD")) {
								this.outputUpdIdentifierExp = true;
							}
						} else {
							addToMap(elementsMaps, parentNode + "_VariableDeclarationFragment_" + subAstNodeType, subActionType);
							addToMap(this.abstractElementsMaps, parentNode + "_VariableDeclarationFragment_Initializer", subActionType);
						}
					}
					analyzeExpressions(actionSet, (++ expId) + "_" + astNodeType);
				}
			} else if (CodeNodes.expressions.contains(astNodeType)) {
				if ("MethodDeclaration".equals(parentNode) || "TypeDeclaration".equals(parentNode) || "EnumDeclaration".equals(parentNode)) {
					if (actionType.equals("UPD")) {
						outputUpdMethodOrClassIdentifierExp = true;
					}
					if ("SimpleName".equals(astNodeType)) {
						addToMap(this.abstractElementsMaps, parentNode + "_Identifier", actionType);
					} else {
						addToMap(this.abstractElementsMaps, parentNode + "_Expression", actionType);
					}
				} else {
					addToMap(this.abstractElementsMaps, parentNode + "_Expression", actionType);
				}
				
				String node = parentNode + "_" + astNodeType;
				addToMap(elementsMaps, node, actionType);
				if (actionType.equals("UPD") || actionType.equals("DEL")) {
					addToMap(stmtBuggyElementTypesMaps, parentNode, astNodeType);
				}
				analyzeExpressions(actionSet, (++ expId) + "_" + astNodeType);
				
			} else {
				if (!"MethodDeclaration".equals(astNodeType) && !"TypeDeclaration".equals(astNodeType)) {
					if (CodeNodes.stmtTypes.contains(astNodeType)) {
						addToMap(stmtMaps, astNodeType, actionType);
						analyzeElementsAction(actionSet.getSubActions(), astNodeType);
					} else if ("Block".equals(astNodeType)) {
						analyzeElementsAction(actionSet.getSubActions(), astNodeType);
					} else {
//						System.err.println("ELEMENT===" + actionStr);
					}
				}
			}
		}
	}

	/**
	 * Elements of expressions in patches.
	 * 1. TO WHAT EXTENT.
	 * 2. HOW TO REPAIR.
	 * 3. Depth of expressions.
	 * 
	 * @param expActionSet
	 */
	private void analyzeExpressions(HierarchicalActionSet expActionSet, String expStr) {
		String actionStr = expActionSet.getActionString();
		String actionType  = actionStr.substring(0, 3);// UPD, INS, DEL, MOV
		// ignore INS.  only focus on buggy expressions
		String astNodeType = expActionSet.getAstNodeType();
		if (CodeNodes.expressions.contains(astNodeType)) {
			addToMap(expMaps, astNodeType, actionType);
		}
		if (actionType.equals("UPD") || actionType.equals("DEL")) {
			if (actionType.equals("UPD")) {
				analyzeBuggyExpressionElements(expActionSet, expStr);
			}
		}
	}
		

	private void analyzeBuggyExpressionElements(HierarchicalActionSet expActionSet, String expStr) {
		String actionStr = expActionSet.getActionString();
		String expType = expActionSet.getAstNodeType();
		List<HierarchicalActionSet> subExpActionSets = expActionSet.getSubActions();
		
		switch (expType) {
		case "ArrayAccess":   // arrayExpression, indexExpression
			for (HierarchicalActionSet subExpActionSet : subExpActionSets) {
				String subActionStr = subExpActionSet.getActionString();
				String actionType  = subActionStr.substring(0, 3);// UPD, INS, DEL, MOV
				String subExpType;
				if (subExpActionSet.getNode().getPos() == expActionSet.getNode().getPos()) {
					subExpType = "Array";
				} else {
					subExpType = "Index";
				}
				addToMap(expElementMaps, expType + "_" + subExpType, actionType);
//				if (!subExpActionSet.getAstNodeType().equals("SimpleName") && !subExpActionSet.getAstNodeType().equals("NumberLiteral")) {
				analyzeExpressions(subExpActionSet, expStr + "_" + subExpActionSet.getAstNodeType());
				this.expDepthList.add(expStr + "_" + subExpActionSet.getAstNodeType());
//				}
			}
			break;
		case "Assignment": // leftHandExp, operator, rightHandExp
			int operatorPos = expActionSet.getNode().getChild(1).getPos();
			for (HierarchicalActionSet subExpActionSet : subExpActionSets) {
				String subActionStr = subExpActionSet.getActionString();
				String actionType  = subActionStr.substring(0, 3);// UPD, INS, DEL, MOV
				String subExpType;
				if (subExpActionSet.getNode().getPos() == operatorPos) {
					subExpType = "operator";
				} else  if (subExpActionSet.getNode().getPos() < operatorPos) {
					subExpType = "leftHandExp";
				} else {
					subExpType = "rightHandExp";
				}
				addToMap(expElementMaps, expType + "_" + subExpType, actionType);
				if (!"operator".equals(subExpType)) {// && !subExpActionSet.getAstNodeType().equals("SimpleName") && !subExpActionSet.getAstNodeType().endsWith("Literal")) {
					analyzeExpressions(subExpActionSet, expStr + "_" + subExpActionSet.getAstNodeType());
				}
				this.expDepthList.add(expStr + "_" + subExpActionSet.getAstNodeType());
			}
			break;
		case "ConditionalExpression": // conditionalExp, thenExp, elseExp
			for (HierarchicalActionSet subExpActionSet : subExpActionSets) {
				String subActionStr = subExpActionSet.getActionString();
				String actionType  = subActionStr.substring(0, 3);// UPD, INS, DEL, MOV
				String subExpType;
				int pos = subExpActionSet.getNode().getPos();
				if (pos == expActionSet.getNode().getChild(0).getPos()) {
					subExpType = "conditionalExp";
				} else if (pos == expActionSet.getNode().getChild(1).getPos()) {
					subExpType = "thenExp";
				} else {
					subExpType = "elseExp";
				}
				addToMap(expElementMaps, expType + "_" + subExpType, actionType);
//				if (!subExpActionSet.getAstNodeType().equals("SimpleName") && !subExpActionSet.getAstNodeType().endsWith("Literal")) {
				analyzeExpressions(subExpActionSet, expStr + "_" + subExpActionSet.getAstNodeType());
//				}
				this.expDepthList.add(expStr + "_" + subExpActionSet.getAstNodeType());
			}
			break;
		case "InfixExpression": // leftExp, operator, rightExp, extendedOperands
			int operatorPos2 = expActionSet.getNode().getChild(1).getPos();
			for (HierarchicalActionSet subExpActionSet : subExpActionSets) {
				String subActionStr = subExpActionSet.getActionString();
				String actionType  = subActionStr.substring(0, 3);// UPD, INS, DEL, MOV
				String subExpType;
				int pos = subExpActionSet.getNode().getPos();
				if (pos < operatorPos2) {
					subExpType = "leftExp";
				} else if (pos == operatorPos2) {
					subExpType = "operator";
				} else {
					subExpType = "rightExp";
				}
				addToMap(expElementMaps, expType + "_" + subExpType, actionType);
				if (!"operator".equals(subExpType)) {// && !subExpActionSet.getAstNodeType().equals("SimpleName") && !subExpActionSet.getAstNodeType().endsWith("Literal")) {
					analyzeExpressions(subExpActionSet, expStr + "_" + subExpActionSet.getAstNodeType());
				}
				this.expDepthList.add(expStr + "_" + subExpActionSet.getAstNodeType());
			}
			break;
		case "MethodInvocation":      // Name, MethodName, arguments:Exp
		case "SuperMethodInvocation": // MethodName, argurments:Exp
			for (HierarchicalActionSet subExpActionSet : subExpActionSets) {
				String subActionStr = subExpActionSet.getActionString();
				String actionType  = subActionStr.substring(0, 3);// UPD, INS, DEL, MOV
				String subExpType;
				String subExpActTionStr = subExpActionSet.getActionString();
				if (subExpActTionStr.indexOf("SimpleName@@MethodName:") == 3 
						|| subExpActTionStr.indexOf("MethodInvocation@@MethodName:") == 3) {
					subExpType = "MethodName";
				} else if (subExpActTionStr.contains("@@Name:")) {
					subExpType = "Qualifier";
				} else {
					subExpType = "Arguments";
//					if (!subExpActionSet.getAstNodeType().equals("SimpleName") && !subExpActionSet.getAstNodeType().endsWith("Literal")) {
					analyzeExpressions(subExpActionSet, expStr + "_" + subExpActionSet.getAstNodeType());
//					}
				}
				addToMap(expElementMaps, expType + "_" + subExpType, actionType);
				this.expDepthList.add(expStr + "_" + subExpActionSet.getAstNodeType());
			}
			break;
		case "CastExpression":        	// Type, exp
		case "InstanceofExpression":  	// exp, instanceof, type,
			for (HierarchicalActionSet subExpActionSet : subExpActionSets) {
				String subActionStr = subExpActionSet.getActionString();
				String actionType  = subActionStr.substring(0, 3);// UPD, INS, DEL, MOV
				String subExpType = subExpActionSet.getAstNodeType();
				if (CodeNodes.elements.contains(subExpType)) {
					if (subExpType.contains("Type")) {
						subExpType = "Type";
					}
					addToMap(expElementMaps, expType + "_" + subExpType, actionType);
				} else {
					addToMap(expElementMaps, expType + "_exp", actionType);
//					if (!subExpActionSet.getAstNodeType().equals("SimpleName") && !subExpActionSet.getAstNodeType().endsWith("Literal")) {
					analyzeExpressions(subExpActionSet, expStr + "_" + subExpActionSet.getAstNodeType());
//					}
				}
				this.expDepthList.add(expStr + "_" + subExpActionSet.getAstNodeType());
			}
			break;
		case "ClassInstanceCreation": 	// Type, arguments:Exp, AnonymousClassDeclaration
			List<ITree> children = expActionSet.getNode().getChildren();
			int typePos = 0;
			for (ITree child : children) {
				int astType = child.getType();
				if (astType == 39 || astType == 5 || astType == 43 ||
						astType == 74 || astType == 75 || astType == 76 ||
						astType == 84 || astType == 87 || astType == 88) {
					typePos = child.getPos();
				}
			}
			for (HierarchicalActionSet subExpActionSet : subExpActionSets) {
				String subActionStr = subExpActionSet.getActionString();
				String actionType  = subActionStr.substring(0, 3);// UPD, INS, DEL, MOV
				String subExpType = subExpActionSet.getAstNodeType();
				int subExpPos = subExpActionSet.getNode().getPos();
				if (subExpPos == typePos) {
					if (subExpType.contains("Type")) {
						subExpType = "Type";
					}
					addToMap(expElementMaps, expType + "_" + subExpType, actionType);
				} else if (subExpPos > typePos) {
					addToMap(expElementMaps, expType + "_arguments", actionType);
//					if (!subExpActionSet.getAstNodeType().equals("SimpleName") && !subExpActionSet.getAstNodeType().endsWith("Literal")) {
					analyzeExpressions(subExpActionSet, expStr + "_" + subExpActionSet.getAstNodeType());
//					}
				} else {
					if (CodeNodes.expressions.contains(subExpType)) {
//						if (!subExpActionSet.getAstNodeType().equals("SimpleName") && !subExpActionSet.getAstNodeType().endsWith("Literal")) {
						addToMap(expElementMaps, expType + "_exp", actionType);
						analyzeExpressions(subExpActionSet, expStr + "_" + subExpActionSet.getAstNodeType());
//						}
					} else if (!"New".equals(subExpType)) {
//						System.err.println("EXP_ClassInstanceCreation===" + actionStr + "===" + subExpActionSet.getActionString());
					}
				}
				this.expDepthList.add(expStr + "_" + subExpActionSet.getAstNodeType());
			}
			break;
		case "PostfixExpression": 		// Exp, operator
		case "PrefixExpression":  		// operator, Exp
			for (HierarchicalActionSet subExpActionSet : subExpActionSets) {
				String subActionStr = subExpActionSet.getActionString();
				String actionType  = subActionStr.substring(0, 3);// UPD, INS, DEL, MOV
				String subExpType = subExpActionSet.getAstNodeType();
				if (CodeNodes.elements.contains(subExpType)) {
					addToMap(expElementMaps, expType + "_" + subExpType, actionType);
				} else if (CodeNodes.expressions.contains(subExpType)) {
					addToMap(expElementMaps, expType + "_exp", actionType);
					analyzeExpressions(subExpActionSet, expStr + "_" + subExpActionSet.getAstNodeType());
				} else {
//					System.err.println("EX_fixExp===" + subExpType);
				}
				this.expDepthList.add(expStr + "_" + subExpActionSet.getAstNodeType());
			}
			break;
		case "ArrayCreation": 			// ArrayType, ArrayInitializer
			for (HierarchicalActionSet subExpActionSet : subExpActionSets) {
				String subActionStr = subExpActionSet.getActionString();
				String actionType  = subActionStr.substring(0, 3);// UPD, INS, DEL, MOV
				if ("ArrayType".equals(subExpActionSet.getAstNodeType())) {
					addToMap(expElementMaps, expType + "_ArrayType", actionType);
				} else {
					addToMap(expElementMaps, expType + "_Initializer", actionType);
					analyzeExpressions(subExpActionSet, expStr + "_" + subExpActionSet.getAstNodeType());
				}
				this.expDepthList.add(expStr + "_" + subExpActionSet.getAstNodeType());
			}
			break;
		case "ArrayInitializer": 		// exps.
		case "FieldAccess":    			// Exp, SimpleName:identifier
		case "ParenthesizedExpression": // Exp
		case "QualifiedName": 			// Name, simpleName
		case "SuperFieldAccess": 		// Name, identifier,
		case "LambdaExpression": 		// parameters:SingleVariableDeclaration/VariableDeclarationFragment
			for (HierarchicalActionSet subExpActionSet : subExpActionSets) {
				String subActionStr = subExpActionSet.getActionString();
				String actionType  = subActionStr.substring(0, 3);// UPD, INS, DEL, MOV
				addToMap(expElementMaps, expType + "_" + subExpActionSet.getAstNodeType(), actionType);
//				if (!subExpActionSet.getAstNodeType().equals("SimpleName") && !subExpActionSet.getAstNodeType().endsWith("Literal")) {
				analyzeExpressions(subExpActionSet, expStr + "_" + subExpActionSet.getAstNodeType());
//				}
				this.expDepthList.add(expStr + "_" + subExpActionSet.getAstNodeType());
			}
			break;
		case "VariableDeclarationExpression": // modifiers, VariableDeclarationFragment
			for (HierarchicalActionSet subExpActionSet : subExpActionSets) {
				String subActionStr = subExpActionSet.getActionString();
				String actionType  = subActionStr.substring(0, 3);// UPD, INS, DEL, MOV
				String subExpType = subExpActionSet.getAstNodeType();
				if (CodeNodes.elements.contains(subExpType)) {
					addToMap(expElementMaps, expType + "_Modifier", actionType);
				} else {
					addToMap(expElementMaps, expType + "_VariableDeclarationFragment", actionType);
					analyzeExpressions(subExpActionSet, expStr + "_" + subExpActionSet.getAstNodeType());
				}
				this.expDepthList.add(expStr + "_" + subExpActionSet.getAstNodeType());
			}
			break;
		case "SimpleName":
		case "ThisExpression":
		case "BooleanLiteral":
		case "CharacterLiteral":
		case "NullLiteral":
		case "NumberLiteral":
		case "StringLiteral":
		case "TypeLiteral":
		case "NormalAnnotation":
		case "MarkerAnnotation":
		case "SingleMemberAnnotation":
		case "CreationReference":
		case "ExpressionMethodReference":
		case "MethodReference":
		case "SuperMethodReference":
		case "TypeMethodReference":
			String actionType1  = actionStr.substring(0, 3);// UPD, INS, DEL, MOV
			addToMap(expElementMaps, expType, actionType1);
			break;
		case "SingleVariableDeclaration":
			for (HierarchicalActionSet subExpActionSet : subExpActionSets) {
				String subActionStr = subExpActionSet.getActionString();
				String actionType  = subActionStr.substring(0, 3);// UPD, INS, DEL, MOV
				String subExpType = subExpActionSet.getAstNodeType();
				if (CodeNodes.elements.contains(subExpType)) {
					if (subExpType.contains("Type")) {
						addToMap(expElementMaps, expType + "_Type", actionType);
					} else {
						addToMap(expElementMaps, expType + "_Modifier", actionType);
					}
				} else if (CodeNodes.expressions.contains(subExpType)) {
					addToMap(expElementMaps, expType + "_Initializer", actionType);
					analyzeExpressions(subExpActionSet, expStr + "_" + subExpActionSet.getAstNodeType());
				} else {
//					System.err.println("EXP_single===" + actionStr);
				}
				this.expDepthList.add(expStr + "_" + subExpActionSet.getAstNodeType());
			}
			break;
		case "VariableDeclarationFragment":
			int operatorPos3 = expActionSet.getNode().getChild(0).getPos();
			for (HierarchicalActionSet subExpActionSet : subExpActionSets) {
				String subActionStr = subExpActionSet.getActionString();
				String actionType  = subActionStr.substring(0, 3);// UPD, INS, DEL, MOV
				if (operatorPos3 == subExpActionSet.getNode().getPos()) {
					addToMap(expElementMaps, expType + "_Identifier", actionType);
					if (actionType.equals("UPD")) {
						this.outputUpdIdentifierExp = true;
					}
				} else {
					addToMap(expElementMaps, expType + "_Initializer", actionType);
				}
//				if (!subExpActionSet.getAstNodeType().equals("SimpleName") && !subExpActionSet.getAstNodeType().endsWith("Literal")) {
				analyzeExpressions(subExpActionSet, expStr + "_" + subExpActionSet.getAstNodeType());
//				}
				this.expDepthList.add(expStr + "_" + subExpActionSet.getAstNodeType());
			}
			break;
		default:
			if (expType.endsWith("Statement")) {
				String actionType  = actionStr.substring(0, 3);// UPD, INS, DEL, MOV
				addToMap(stmtMaps, expType, actionType);
				if (actionType.equals("UPD") || actionType.equals("DEL")) {
					analyzeElementsAction(subExpActionSets, expType);
				}
			} else {
//				System.err.println("EXP_default===" + actionStr);
			}
			break;
		}
	}

	private void addToMap(Map<String, Map<String, Integer>> dataMap, String key, String subKey) {
		Map<String, Integer> subDataMap = dataMap.get(key);
		if (subDataMap == null) {
			subDataMap = new HashMap<>();
			subDataMap.put(subKey, 1);
			dataMap.put(key, subDataMap);
		} else {
			Integer subValue = subDataMap.get(subKey);
			if (subValue == null) {
				subDataMap.put(subKey, 1);
			} else {
				subDataMap.put(subKey, subValue + 1);
			}
		}
	}

	private void outputExpDepthData(String outputFileName, Map<DiffEntryHunk, List<HierarchicalActionSet>> allPatches) {
		StringBuilder builder = new StringBuilder();
		String startIndex = "--";
		int deepestIndex = -1;
		int deepestLength = 0;
		for (int i = 0, size = this.expDepthList.size(); i < size; i ++) {
			String expDepth = expDepthList.get(i);
			int length = expDepth.split("_").length;
			if (expDepth.startsWith(startIndex)) {
				if (length > deepestLength) {
					deepestLength = length;
					deepestIndex = i;
				}
			} else {
				if (!startIndex.equals("--")) {
					builder.append(this.expDepthList.get(deepestIndex)).append("\n");
				}
				startIndex = expDepth.substring(0, expDepth.indexOf("_") + 1);
				deepestLength = length;
				deepestIndex = i;
			}
		}
		// by apr
		if (!this.expDepthList.isEmpty()){
			builder.append(this.expDepthList.get(deepestIndex)).append("\n");
			
			FileHelper.outputToFile(outputFileName, builder, false);
		}
		
	}
}

package donor.main;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.Type;

import donor.modify.Modification;
import donor.modify.Revision;
import donor.parser.NodeUtils;
import donor.search.CodeBlock;
import donor.search.CodeBlockMatcher;
import donor.search.CodeSnippet;
import donor.search.FileUtils;
import donor.search.LocalLog;
import donor.search.Node;
import donor.search.Pair;
import donor.search.SimpleFilter;
import donor.search.Triple;

import java.util.regex.Pattern;

public class Main {
	private static String project_dir = System.getProperty("user.dir");
	private static String logfile = project_dir + "/codesearch.log";
	
	// for checking patch types (basic type = "intrinsic")
	private static Map<String, Pair<String, String>> lineTypesMap = new HashMap<String, Pair<String, String>>();
	
	// args
	private static String proj = "";
	private static String id = "";
	private static String buggy_src_path = "";
	private static String fixed_src_path = "";
	private static String target_source_path = "";
	
	// code block line range
	private static int snippet_line_range = 5;
	private static int MAX_LESS_THRESHOLD = 1;//3; TODO: attempt
	private static int MAX_MORE_THRESHOLD = 5;
	
	public static void main(String[] args) throws IOException{
		// parse parameter.
		Map<String, String> parameters = setParameters(args);
		
		// conduct code search for buggy lines
		proj = parameters.get("proj");
		id = parameters.get("id");
		buggy_src_path = parameters.get("buggy_src_path"); // not necessary
		fixed_src_path = parameters.get("fixed_src_path");
		target_source_path = parameters.get("target_source_path");
		downloadProjId(proj, id);
		
		// init search log
		proj = proj.toLowerCase();
		init();
		
		// obtain source code path (recipient code) & target path(for code search)
		// e.g., d4j-repo/fixed_bugs_dir/Chart/Chart_3/source
		fixed_src_path = FileUtils.get_source_path(fixed_src_path);  
		// e.g., d4j-repo/Chart/Chart_3/source
		target_source_path = FileUtils.get_source_path(target_source_path);
		
		// read fixed lines for each buggy program
		// e.g., org.jfree.data.time.TimeSeries:1057, org.jfree.data.time.TimeSeries:1058
		List<String> fixed_lines = FileUtils.readFile(proj, id, "fixed");

		// get all chunks && do code search for each chunk.
		List<List<String>> fixed_chunks = getFixedChunks(fixed_lines);
		for (List<String> fixed_chunk : fixed_chunks){
			FileUtils.checkPatchType(fixed_chunk, proj, id, lineTypesMap);
			codeSearchForLine(fixed_chunk, fixed_src_path, "fixed");
		}
	}

	/**
	 * do code search for each chunk of line(s).
	 * @param line_chunk
	 * @param src_path
	 * @param flag
	 */
	private static int codeSearchForLine(List<String> line_chunk, String src_path, String flag) {
		// get compilation unit for the line
		String first_line = line_chunk.get(0);
		String last_line = line_chunk.get(line_chunk.size() - 1);
		String line_path = src_path + "/" + first_line.split(":")[0].replace(".", "/") + ".java";
		print(line_path);
		CompilationUnit unit = (CompilationUnit)FileUtils.genASTFromFile(line_path, ASTParser.K_COMPILATION_UNIT);

		// get code snippet
		int first_lineNo = Integer.parseInt(first_line.split(":")[1]);
		int last_lineNo = Integer.parseInt(last_line.split(":")[1]);
		
		// get all intrinsic lines && save to file
		List<Integer> linesList = new ArrayList<>();
		for(String line : line_chunk){
			// find a intrinsic patch line
			if (lineTypesMap.containsKey(line) && lineTypesMap.get(line).getFirst().equals("intrinsic")){
				String typelog = "./search-log/" + proj + '/' + id + '/' + line + "_intrinsic.log";
				FileUtils.writeStringToFile(typelog,lineTypesMap.get(line).getSecond());
				print("line is intrinsic:" + line);
			}else{
				int lineNo = Integer.parseInt(line.split(":")[1]);
				linesList.add(lineNo);
			}
		}
		
		// bug fix: return if empty
		if (linesList.isEmpty()){
			return -1;
		}
		
		// TODO: make more attempts here.
		// for test: change 5 to 1
		CodeSnippet codeSnippet = new CodeSnippet(unit, linesList, snippet_line_range, null, MAX_LESS_THRESHOLD, MAX_MORE_THRESHOLD);

		// get code block
		CodeBlock codeBlock = new CodeBlock(line_path, unit, codeSnippet.getASTNodes());
		Integer methodID = codeBlock.getWrapMethodID();
		if(methodID == null){
			LocalLog.log("Find no block");
		}
		
		// get block list
		List<CodeBlock> blockList = new LinkedList<>();
		// TODO: why comment this line? (needs further attempts)
		// blockList.addAll(codeBlock.reduce());
		blockList.add(codeBlock);
		
		// prepare to do code search
		Set<String> haveTryBuggySourceCode = new HashSet<>();
		for(CodeBlock oneBuggyBlock : blockList){
			// get all values for the line.
			// List<ASTNode> buggyNodes = oneBuggyBlock.getNodes();
			
			// preparation.
			String currentBlockString = oneBuggyBlock.toSrcString().toString();
			if(currentBlockString == null || currentBlockString.length() <= 0){
				continue;
			}
			// skip duplicated source code
			if(haveTryBuggySourceCode.contains(currentBlockString)){
				continue;
			}
			haveTryBuggySourceCode.add(currentBlockString);
			
			Set<String> haveTryPatches = new HashSet<>();

			// get all variables can be used at buggy line
			Map<String, Type> usableVars = new HashMap<String, Type>();
			for(int line_no = first_lineNo; line_no <= last_lineNo; line_no ++){
				usableVars.putAll(NodeUtils.getUsableVarTypes(line_path, line_no));
			}
			
			// search candidate similar code block
			SimpleFilter simpleFilter = new SimpleFilter(oneBuggyBlock);
			List<Triple<CodeBlock, Double, String>> candidates = simpleFilter.filter(target_source_path, 0.3);
			
//			LocalLog.log("print candidates ---");
			// each chunk (of lines) corresponds a specified log
			// TODO: consider using linesList.get(0) to replace first_line, and the same operation for last_lineNo. 
			// e.g., ./search-log/chart/3/org.jfree.data.time.TimeSeries:1057-1058_fixed.log
			logfile = "./search-log/" + proj + '/' + id + '/' + first_line + "-"
					+ last_lineNo + "_" + flag + ".log";
			// e.g., ./search-log/chart/3/lines_org.jfree.data.time.TimeSeries:1057-1058_fixed.log
			String log2 = "./search-log/" + proj + '/' + id + "/lines_" + first_line + "-"
					+ last_lineNo + "_" + flag + ".log";
			
			// get all similar code 
			getAllPatches(oneBuggyBlock, candidates,
					usableVars, currentBlockString, haveTryPatches, logfile, log2, flag);
		}
		return 1;
	}

	private static void getAllPatches(CodeBlock oneBuggyBlock, List<Triple<CodeBlock, Double, String>> candidates,
			Map<String, Type> usableVars, String currentBlockString, Set<String> haveTryPatches,
			String logfile, String log2, String flag){
		
		// save original code snippet
		FileUtils.writeStringToFile(logfile, "-------- Original Code ---------\n",true);
		FileUtils.writeStringToFile(logfile, currentBlockString + "\n" 
			+ oneBuggyBlock.getFileName()
			+ "<" + oneBuggyBlock.getCodeRange().getFirst() 
			+ "," + oneBuggyBlock.getCodeRange().getSecond() + ">\n", true);
		
		FileUtils.writeStringToFile(log2, "fixed code:" 
				+ oneBuggyBlock.getFileName() + "-"
//				+ oneBuggyBlock.getClass() + "-"
				+ oneBuggyBlock.getCodeRange().getFirst() + "-"
				+ oneBuggyBlock.getCodeRange().getSecond()
				+ "\n"
				, true);
		
		//		int i = 1;
		for(Triple<CodeBlock, Double, String> similar : candidates){
			// TODO: whether we need to set a top N value.
			// only consider top 20 candidates
//			if (i > 2000){
//				break;
//			}
//			i++;
			
			FileUtils.writeStringToFile(logfile, "\n-------- Similar Code ---------\n",true);
			FileUtils.writeStringToFile(logfile,
					similar.getFirst().toString() 
					+ "\n" + similar.getSecond() 
					+ "\n" + similar.getThird()
					+ "\n",true);
			
			FileUtils.writeStringToFile(log2, "similar code:" 
					+ similar.getFirst().getFileName() + "-"
//					+ oneBuggyBlock.getClass() + "-"
					+ similar.getFirst().getCodeRange().getFirst() + "-"
					+ similar.getFirst().getCodeRange().getSecond()
					+ "\n"
					, true);
			
			// TODO: the rest code probably can be deleted.
			// compute transformation
			List<Modification> modifications = CodeBlockMatcher.match(oneBuggyBlock, similar.getFirst(), usableVars);
			if (modifications.size() == 0 && flag.equals("buggy")){
				FileUtils.writeStringToFile(logfile, "\n-------- No Patch ---------\n\n",true);
			}
			
			Map<String, Set<Node>> already = new HashMap<>();
			// try each transformation first
			List<Set<Integer>> list = new ArrayList<>();
			list.addAll(consistentModification(modifications));
			modifications = removeDuplicateModifications(modifications);
			for(int index = 0; index < modifications.size(); index++){
				Modification modification = modifications.get(index);
				String modify = modification.toString();
				Set<Node> tested = already.get(modify);
				if(tested != null){
					if(tested.contains(modification.getSrcNode())){
//						continue;
						// comment print 
//						print("continue");
					} else {
						tested.add(modification.getSrcNode());
					}
				} else {
					tested = new HashSet<>();
					tested.add(modification.getSrcNode());
					already.put(modify, tested);
				}
				Set<Integer> set = new HashSet<>();
				set.add(index);
				list.add(set);
			}
			
			//
			List<Modification> legalModifications = new ArrayList<>();
			while(true){
				for(Set<Integer> modifySet : list){
					for(Integer index : modifySet){
						modifications.get(index).apply(usableVars);
					}
					
					String replace = oneBuggyBlock.toSrcString().toString();
					if(replace.equals(currentBlockString)) {
						for(Integer index : modifySet){
							modifications.get(index).restore();
						}
						continue;
					}
					if(haveTryPatches.contains(replace)){
						if(flag.equals("buggy")){
							// also save repeated patch
							FileUtils.writeStringToFile(logfile, "\n\n-------- Repeated Patch ---------\n",true);
							FileUtils.writeStringToFile(logfile, replace, true);
						}
						
//						System.out.println("already try ...");
						for(Integer index : modifySet){
							modifications.get(index).restore();
						}
						if(legalModifications != null){
							for(Integer index : modifySet){
								legalModifications.add(modifications.get(index));
							}
						}
						continue;
					}
					
//					System.out.println("========");
//					System.out.println(replace);
//					System.out.println("========");
					
					if(flag.equals("buggy")){
						FileUtils.writeStringToFile(logfile, "\n\n-------- Patch ---------\n",true);
						FileUtils.writeStringToFile(logfile, replace,true);
					}
					
					haveTryPatches.add(replace);
					for(Integer index : modifySet){
						modifications.get(index).restore();
					}
				}
				if(legalModifications == null){
					break;
				}
				list = combineModification(legalModifications);
				modifications = legalModifications;
				legalModifications = null;
			}
		}
	}
	
	
	
	private static List<Set<Integer>> combineModification(List<Modification> modifications){
		List<Set<Integer>> list = new ArrayList<>();
		int length = modifications.size();
		if(length == 0){
			return list;
		}
		int[][] incompatibleMap = new int[length][length];
		for(int i = 0; i < length; i++){
			for(int j = i; j < length; j++){
				if(i == j){
					incompatibleMap[i][j] = 1;
				} else if(modifications.get(i).compatible(modifications.get(j))){
					incompatibleMap[i][j] = 0;
					incompatibleMap[j][i] = 0;
				} else {
					incompatibleMap[i][j] = 1;
					incompatibleMap[i][j] = 1;
				}
			}
		}
		List<Set<Integer>> baseSet = new ArrayList<>();
		for(int i = 0; i < modifications.size(); i++){
			Set<Integer> set = new HashSet<>();
			set.add(i);
			baseSet.add(set);
		}

		list.addAll(expand(incompatibleMap, baseSet, 2, 4));
		
		return list;
	}
	
	private static List<Set<Integer>> expand(int[][] incompatibleTabe, List<Set<Integer>> baseSet, int currentSize, int upperbound){
		List<Set<Integer>> rslt = new LinkedList<>();
		if(currentSize > upperbound){
			return rslt;
		}
		while(baseSet.size() > 1000){
			baseSet.remove(baseSet.size() - 1);
		}
		int length = incompatibleTabe.length;
		for(Set<Integer> base : baseSet){
			int minIndex = 0;
			for(Integer integer : base){
				if(integer > minIndex){
					minIndex = integer;
				}
			}
			
			for(minIndex ++; minIndex < length; minIndex ++){
				boolean canExd = true;
				for(Integer integer : base){
					if(incompatibleTabe[minIndex][integer] == 1){
						canExd = false;
						break;
					}
				}
				if(canExd){
					Set<Integer> expanded = new HashSet<>(base);
					expanded.add(minIndex);
					rslt.add(expanded);
				}
			}
		}
		
		if(rslt.size() > 0){
			rslt.addAll(expand(incompatibleTabe, rslt, currentSize + 1, upperbound));
		}
		
		return rslt;
	}
	
	private static List<Set<Integer>> consistentModification(List<Modification> modifications){
		List<Set<Integer>> result = new LinkedList<>();
		String regex = "[A-Za-z_][0-9A-Za-z_.]*";
		Pattern pattern = Pattern.compile(regex);
		for(int i = 0; i < modifications.size(); i++){
			Modification modification = modifications.get(i);
			if(modification instanceof Revision){
				Set<Integer> consistant = new HashSet<>();
				consistant.add(i);
				for(int j = i + 1; j < modifications.size(); j++){
					Modification other = modifications.get(j);
					if(other instanceof Revision){
						if(modification.compatible(other) && modification.getTargetString().equals(other.getTargetString())){
							ASTNode node = FileUtils.genASTFromSource(modification.getTargetString(), ASTParser.K_EXPRESSION);
							if(node instanceof Name || node instanceof FieldAccess || pattern.matcher(modification.getTargetString()).matches()){
								consistant.add(j);
							}
						}
					}
				}
				if(consistant.size() > 1){
					result.add(consistant);
				}
			}
		}
		
		return result;
	}
	
	private static List<Modification> removeDuplicateModifications(List<Modification> modifications){
		//remove duplicate modifications
		List<Modification> unique = new LinkedList<>();
		for (Modification modification : modifications) {
			boolean exist = false;
			for (Modification u : unique) {
				if (u.getRevisionTypeID() == modification.getRevisionTypeID()
						&& u.getSourceID() == modification.getSourceID()
						&& u.getTargetString().equals(modification.getTargetString())
						&& u.getSrcNode().toSrcString().toString().equals(modification.getSrcNode())) {
					exist = true;
					break;
				}
			}
			if(!exist){
				unique.add(modification);
			}
		}
		return unique;
	}
	
	/*
	 * delete/init log dir 
	 */
	private static void init() {
		logfile = "./search-log/" + proj + '/' + id + '/' ; // e.g., ./search-log/chart/3/
		File dir = new File(logfile);
		if (dir.exists() && dir.isDirectory()) {
			for (File file : dir.listFiles()){
				file.delete();
				print(file.getName() + " exists, and now clear it.");
			}
		}
	}
	
	/**
	 *  @description: This is to sort fixed lines and return fixed chunks
	 */
	private static List<List<String>> getFixedChunks(List<String> fixed_lines) {
		// sort
		Collections.sort(fixed_lines, new Comparator<String>() {
			@Override
			public int compare(String  o1, String o2) {
				String clazz1 = o1.split(":")[0];
				int lineNo1 = Integer.parseInt(o1.split(":")[1]);
				
				String clazz2 = o2.split(":")[0];
				int lineNo2 = Integer.parseInt(o2.split(":")[1]);
				
				// if clazz1 greater than 2, compareTo return 1. 
				if(clazz1.compareTo(clazz2) == 0){
					if(lineNo1 < lineNo2){
						return -1;
					}else if(lineNo1 > lineNo2){
						return 1;
					}else{
						return 0;
					}
				} 
				else if (clazz1.compareTo(clazz2) > 0){
					return 1;
				}else{
					return -1;
				}
			}
		});
		
		
		List<List<String>> chunks = new ArrayList<>();
		for(String line : fixed_lines){
			String clazz = line.split(":")[0];
			int lineNo = Integer.parseInt(line.split(":")[1]);
			if (chunks.isEmpty()){
				List<String> first_chunk = new ArrayList<>();
				first_chunk.add(line);
				chunks.add(first_chunk);
				continue;
			}
			
			// traveser the chunks nested list
			int flag = 0;
			for(List<String> chunk : chunks){
				int len = chunk.size();
				for(int i = 0; i < len; i++){
					String lineInChunk = chunk.get(i);
					String clazz_chunk = lineInChunk.split(":")[0];
					int lineNo_chunk = Integer.parseInt(lineInChunk.split(":")[1]);
					// judge if in the same chunk 
					if(clazz_chunk.equals(clazz) && Math.abs(lineNo_chunk - lineNo) == 1){
						chunk.add(line);
						flag = 1;
						break;
					}
				}
				if(flag ==1){
					break;
				}
			}
			// add new chunk
			if(flag == 0){
				List<String> new_chunk = new ArrayList<>();
				new_chunk.add(line);
				chunks.add(new_chunk);
			}
		}
		
		return chunks;
	}
	
	/**
	 * use print() to replace System.out.println()
	 * @param str
	 */
	public static void print(String str){
		System.out.println(str);
	}
	
	/*
	 * receive parameters
	 */
	private static Map<String, String> setParameters(String[] args) {
//		proj = parameters[0];
//		id = parameters[1];
//		buggy_src_path = parameters[2];
//		fixed_src_path = parameters[3];
//		target_source_path = parameters[4];
		
		Map<String, String> parameters = new HashMap<>();
		
        Option opt1 = new Option("proj","project_name",true,"e.g., Chart");
        opt1.setRequired(true);
        Option opt2 = new Option("id","id",true,"e.g., 3");
        opt2.setRequired(true);   
        Option opt3 = new Option("buggy_src_path","buggy_src_path",true,"e.g., d4j-repo/Chart/Chart_3");
        opt3.setRequired(true);
        Option opt4 = new Option("fixed_src_path","fixed_src_path",true,"e.g., d4j-repo/fixed_bugs_dir/Chart/Chart_3");
        opt4.setRequired(true);
        Option opt5 = new Option("target_source_path","target_source_path",true,"e.g., d4j-repo/Chart/Chart_3");
        opt4.setRequired(true);

        Options options = new Options();
        options.addOption(opt1);
        options.addOption(opt2);
        options.addOption(opt3);
        options.addOption(opt4);
        options.addOption(opt5);

        CommandLine cli = null;
        CommandLineParser cliParser = new DefaultParser();
        HelpFormatter helpFormatter = new HelpFormatter();

        try {
            cli = cliParser.parse(options, args);
        } catch (org.apache.commons.cli.ParseException e) {
            helpFormatter.printHelp(">>>>>> test cli options", options);
            e.printStackTrace();
        } 

        if (cli.hasOption("proj")){
        	parameters.put("proj", cli.getOptionValue("proj"));
        }
        if(cli.hasOption("id")){
        	parameters.put("id", cli.getOptionValue("id"));
        }
        if(cli.hasOption("buggy_src_path")){
        	parameters.put("buggy_src_path", cli.getOptionValue("buggy_src_path"));
        }
        if(cli.hasOption("fixed_src_path")){
        	parameters.put("fixed_src_path", cli.getOptionValue("fixed_src_path"));
        }
        if(cli.hasOption("target_source_path")){
        	parameters.put("target_source_path", cli.getOptionValue("target_source_path"));
        }
        
        System.out.format("Proj: %s, Id: %s\n", parameters.get("proj"), parameters.get("id"));
		return parameters;
    }
	
	// apr: simple run
	public static String shellRun2(String cmd) throws IOException {
        Process process= Runtime.getRuntime().exec(cmd);
        String results = getShellOut(process);
        return results;
	}
	// apr: simple run
	public static String shellRun2(String[] cmd) throws IOException {
		Process process= Runtime.getRuntime().exec(cmd);
		String results = getShellOut(process);
		return results;
	}
	
	private static String getShellOut(Process process) {
		ExecutorService service = Executors.newSingleThreadExecutor();
        Future<String> future = service.submit(new ReadShellProcessMain(process));
        String returnString = "";
        try {
            returnString = future.get(10800L, TimeUnit.SECONDS);
        } catch (InterruptedException e){
            future.cancel(true);
            e.printStackTrace();
            shutdownProcess(service, process);
            return "";
        } catch (TimeoutException e){
            future.cancel(true);
            e.printStackTrace();
            shutdownProcess(service, process);
            return "";
        } catch (ExecutionException e){
            future.cancel(true);
            e.printStackTrace();
            shutdownProcess(service, process);
            return "";
        } finally {
            shutdownProcess(service, process);
        }
        return returnString;
	}
	
	private static void shutdownProcess(ExecutorService service, Process process) {
		service.shutdownNow();
        try {
			process.getErrorStream().close();
			process.getInputStream().close();
	        process.getOutputStream().close();
		} catch (IOException e) {
			e.printStackTrace();
		}
        process.destroy();
	}
    
    public static void downloadProjId(String proj, String id) throws IOException{
//	    	String proj = "Chart";
//	    	String id = "10";
    	String projId = proj + "_" + id;
    	
    	String repoBuggy = "d4j-repo/";
		String repoFixed = "d4j-repo/fixed_bugs_dir/";
		String[] cmd = {"/bin/sh","-c", "cd " + repoBuggy 
				+ " && " + "/bin/bash single-download.sh "
				+ proj + " " + id + " 1"};
		// if exist, do not download
		if (new File(repoBuggy + proj + "/" + projId).exists()){
			System.out.println(repoBuggy + proj + "/" + projId + " exists, so skip downloading.");
		}else{
			shellRun2(cmd);
		}
		
		String[] cmd2 = {"/bin/sh","-c", "cd " + repoFixed 
				+ " && " + "/bin/bash  fixed_single_download.sh "
				+ proj + " " + id + " 1"};
		// if exist, do not download
		if (new File(repoFixed + proj + "/" + projId).exists()){
			System.out.println(repoFixed + proj + "/" + projId + " exists, so skip downloading.");
		}else{
			shellRun2(cmd2);
		}
    }
}

class ReadShellProcessMain implements Callable<String> {
    public Process process;

    public ReadShellProcessMain(Process p) {
        this.process = p;
    }

    public synchronized String call() {
        StringBuilder sb = new StringBuilder();
        BufferedInputStream in = null;
        BufferedReader br = null;
        try {
            String s;
            in = new BufferedInputStream(process.getInputStream());
            br = new BufferedReader(new InputStreamReader(in));
            while ((s = br.readLine()) != null && s.length()!=0) {
                if (sb.length() < 1000000){
                    if (Thread.interrupted()){
                        return sb.toString();
                    }
                    sb.append(System.getProperty("line.separator"));
                    sb.append(s);
                }
            }
            in = new BufferedInputStream(process.getErrorStream());
            br = new BufferedReader(new InputStreamReader(in));
            while ((s = br.readLine()) != null && s.length()!=0) {
                if (Thread.interrupted()){
                    return sb.toString();
                }
                if (sb.length() < 1000000){
                    sb.append(System.getProperty("line.separator"));
                    sb.append(s);
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            if (br != null){
                try {
                    br.close();
                } catch (IOException e){
                }
            }
            if (in != null){
                try {
                    in.close();
                } catch (IOException e){
                }
            }
            process.destroy();
        }
        return sb.toString();
    }
}


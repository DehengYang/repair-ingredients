package edu.lu.uni.serval.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import edu.lu.uni.serval.entity.Pair;

public class FileOp {
	private static File bkupFolder;
	
	private static List<String> basicType=  new ArrayList<>(Arrays.asList(
			"boolean", "byte", "char", "short", "int", "long", "float", "double",
			"Integer", "Float", "Double", "String", "Array",
			"=", "==", "!", "!=", "&", "&&", "+", "++", "-", "--",
			"<", "<=", ">", ">=", "/", "/=", "|", "||",	"*", "*=", "%", "%=",
			// extra: "===", "false", "true"
			"==="
			));
	
	/**
	 * backup and restore
	 * @param dpSrcPath
	 * @param flag
	 * @throws IOException
	 */
	public static void backup(String dpSrcPath, String flag) throws IOException{
		if (flag.equals("backup")){
			// first backup
			// without "/"
			String srcPath = null;
			if (dpSrcPath.endsWith("/")){
				srcPath = dpSrcPath.substring(0, dpSrcPath.length() - 1);
			}else{
				srcPath = dpSrcPath;
			}
			// this.bkupFolder
			bkupFolder = new File(srcPath + "-ori");
			if (bkupFolder.exists() && bkupFolder.isDirectory()){
				// already have a backup folder
			}else{
				FileUtils.copyDirectory(new File(dpSrcPath), bkupFolder);
			}
		}else{
			// restore
			FileUtils.deleteDirectory(new File(dpSrcPath));
			FileUtils.copyDirectory(bkupFolder, new File(dpSrcPath));
		}
	}
	
	public static List<String> readFile(String proj, String id, String flag) throws IOException {
		String file_path = "";
		proj = upperFirstCase(proj);
		if (flag.equals("buggy")){
			file_path = "buggy_locs/" + proj + '/' + proj + '_' + id + ".txt";
		}else if(flag.equals("fixed")){
			file_path = "buggy_locs/" + proj + '/' + proj + '_' + id + "_fixed.txt";
		}else if(flag.equals("modification")){
			file_path = "modifications/" + proj + '/' + proj + '_' + id + ".txt";
		}else{
			print("Invalid flag:" + flag);
			return null;
		}
		
		List<String> lines = new ArrayList<>();
		
		// bug fix: if file does not exist (especially when flag is modifications), return empty lines. 
		File file = new File(file_path);
		if(!file.exists()){
			return lines;
		}
		
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file_path));
			String line;
			while ((line = br.readLine()) != null) {
				lines.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				br.close();
			}
		}
		
		return lines;
	}
	
	/*
	 * to capitalize the first letter of a string
	 */
	private static String upperFirstCase(String str){
		if (str.length() > 0){
			return str.substring(0, 1).toUpperCase() + str.substring(1);
		}
		return null;
	}
	
	/**
	 * use print() to replace System.out.println()
	 * @param str
	 */
	public static void print(String str){
		System.out.println(str);
	}
	
	/**
	 * 
	 * @param classPath
	 * @param lineNo
	 * @param proj
	 * @param id
	 * @return
	 * @throws IOException
	 */
	public static Map<Integer, List<String>> getFineGrainedModifications(String classPath, int lineNo, String proj, String id) throws IOException {
		List<String> lines = readFile(proj, id, "modification");
		Map<Integer, List<String>> modMap = new HashMap<>(); // to save modifications for each lineNo
		for(int i = 0; i < lines.size(); i++){
			String line = lines.get(i).trim();
			String givenFixedLine = classPath + ":" + lineNo;
			// get modification of the patch line.
			if(givenFixedLine.contains(line)){ // judge if the given line is fixed line
				// if so, the next line includes all modifications. 
//				String modification = lines.get(i+1).replace("+", " ").replace("-", " ")
//						.replace("*", " ").replace("\\", " ")
//						.replace(">", " ").replace("<", " ")
//						.replace("\\.",	" ").replace("/",	" ")
//						.replace("|",	" ").replace("\"",	" ")
//						.replace(";",	" ")
//						.replace("(",	" ").replace(")",	" ")
//						.replace("{",	" ").replace("}",	" ")
//						.replace("!",	" ").replace("?",	" ")
//						; // only keep chars.  &&
				// bug fix:time1 use replaceAll rather than multiple-replace.
				String modification = lines.get(i+1).replaceAll("[^a-z^A-Z^0-9]", " ");
				// TODO:str = str.replaceAll("[^a-z^A-Z^0-9]", "");
				
				// bug fix:time1 change " " into \\s+ to split multi-blank spaces.
				// bug fix:time1 add trim()
				String[] mods = modification.trim().split("\\s+");
				//Time1:&& lastUnitField.equals(loopUnitField)
				//lastUnitField equals loopUnitField
				modMap.put(lineNo, Arrays.asList(mods));
				
				// Chart 1: only contains =
				
//				private static List<String> basicType=  new ArrayList<>(Arrays.asList(
//						"boolean", "byte", "char", "short", "int", "long", "float", "double",
//						"Integer", "Float", "Double", "String", "Array",
//						"=", "==", "!", "!=", "&", "&&", "+", "++", "-", "--",
//						"<", "<=", ">", ">=", "/", "/=", "|", "||",	"*", "*=", "%", "%=",
//						// extra: "===", "false", "true"
//						"==="
//						));
//				if (basicType.contains(modification)){
//					lineTypesMap.put(line, new Pair<>("type1", modification));
//				}else{
//					lineTypesMap.put(line, new Pair<>("type2", modification));
//				}
			}
		}
		
		return modMap;
	}

}

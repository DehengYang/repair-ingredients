package donor.search;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import donor.main.Main;
import donor.parser.Constant;
import donor.parser.Subject;

public class FileUtils {
	private static Logger logger = Logger.getLogger(Main.class.getName());

	// only contains(all lower case): string, double, int, float, char, 
	// =, !, !=, &&, &, ==, ++, +, -.
	// boolean, byte, char, short, int, long, float and double
	// Integer, Float, Double, String, Array
	private static List<String> basicType=  new ArrayList<>(Arrays.asList(
			"boolean", "byte", "char", "short", "int", "long", "float", "double",
			"Integer", "Float", "Double", "String", "Array",
			"=", "==", "!", "!=", "&", "&&", "+", "++", "-", "--",
			"<", "<=", ">", ">=", "/", "/=", "|", "||",	"*", "*=", "%", "%=",
			// extra: "===", "false", "true"
			"==="
			));
	
	//
	public static CompilationUnit genASTFromFile(String fileName){
		return (CompilationUnit)genASTFromSource(readFileToString(fileName), ASTParser.K_COMPILATION_UNIT);
	}
	
	public static ASTNode genASTFromSource(String icu, int type) {
		ASTParser astParser = ASTParser.newParser(AST.JLS8);
		Map<?, ?> options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_7, options);
		astParser.setCompilerOptions(options);
		astParser.setSource(icu.toCharArray());
		astParser.setKind(type);
		astParser.setResolveBindings(true);
		astParser.setBindingsRecovery(true);
		return astParser.createAST(null);
	}
	
	// generate ast from file
	public static ASTNode genASTFromFile(String line_path, int type) {
		String icu = readFileToString(line_path);
		ASTParser astParser = ASTParser.newParser(AST.JLS8);
		Map<?, ?> options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_7, options);
		astParser.setCompilerOptions(options);
		astParser.setSource(icu.toCharArray());
		astParser.setKind(type);
		astParser.setResolveBindings(true);
		astParser.setBindingsRecovery(true);
		return astParser.createAST(null);
	}
	
	/**
	 * iteratively search files with the root as {@code file}
	 * 
	 * @param file
	 *            : root file of type {@code File}
	 * @param fileList
	 *            : list to save all the files
	 * @return : a list of all files
	 */
	public static List<File> ergodic(File file, List<File> fileList) {
		if (file == null) {
			logger.info("ergodic Illegal input file : null.");
			return fileList;
		}
		File[] files = file.listFiles();
		if (files == null)
			return fileList;
		for (File f : files) {
			if (f.isDirectory()) {
				ergodic(f, fileList);
			} else if (f.getName().endsWith(".java"))
				fileList.add(f);
		}
		return fileList;
	}
	
	/**
	 * iteratively search the file in the given {@code directory}
	 * 
	 * @param directory
	 *            : root directory
	 * @param fileList
	 *            : list of file
	 * @return : a list of file
	 */
	public static List<String> ergodic(String directory, List<String> fileList) {
		if (directory == null) {
			logger.info("ergodic Illegal input file : null.");
			return fileList;
		}
		File file = new File(directory);
		File[] files = file.listFiles();
		if (files == null)
			return fileList;
		for (File f : files) {
			if (f.isDirectory()) {
				ergodic(f.getAbsolutePath(), fileList);
			} else if (f.getName().endsWith(Constant.SOURCE_FILE_SUFFIX))
				fileList.add(f.getAbsolutePath());
		}
		return fileList;
	}
	
	/**
	 * read file, return lines.
	 * @param proj
	 * @param id
	 * @param flag
	 * @return
	 * @throws IOException
	 */
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
	
	// read file string
	public static String readFileToString(String filePath) {
		if (filePath == null) {
			logger.info("readFileToString Illegal input file path : null.");
			return new String();
		}
		File file = new File(filePath);
		if (!file.exists() || !file.isFile()) {
			logger.info("readFileToString Illegal input file path : " + filePath);
			return new String();
		}
		return readFileToString(file);
	}
	
	// read file
	public static String readFileToString(File file) {
		if (file == null) {
			logger.info("readFileToString Illegal input file : null.");
			return new String();
		}
		StringBuffer stringBuffer = new StringBuffer();
		InputStream in = null;
		InputStreamReader inputStreamReader = null;
		try {
			in = new FileInputStream(file);
			inputStreamReader = new InputStreamReader(in, "UTF-8");
			char[] ch = new char[1024];
			int readCount = 0;
			while ((readCount = inputStreamReader.read(ch)) != -1) {
				stringBuffer.append(ch, 0, readCount);
			}
			inputStreamReader.close();
			in.close();

		} catch (Exception e) {
			if (inputStreamReader != null) {
				try {
					inputStreamReader.close();
				} catch (IOException e1) {
					return new String();
				}
			}
			if (in != null) {
				try {
					in.close();
				} catch (IOException e1) {
					return new String();
				}
			}
		}
		return stringBuffer.toString();
	}
	
	
	/**
	 * obtain source code path
	 * @param path
	 * @return
	 */
	public static String get_source_path(String path){	
		String src_path = "";
		final File folder = new File(path);
		for(File fileEntry : folder.listFiles()){
			if(fileEntry.isDirectory()){
				// try to find source, src, src/java, src/main/java
				// TODO: find . -name AbstractCategoryItemRenderer.java
				// ./source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java
				if (fileEntry.getName().equals("source")){
					src_path = fileEntry.getPath();
					break;
				}else if(fileEntry.getName().equals("src")) {
					// check whether src/main/java
					String src_path_tmp = fileEntry.getPath() + "/main/java";
					File src_path_tmp_file = new File(src_path_tmp);

					// check whether src/java
					String src_path_tmp_2 = fileEntry.getPath() + "/java";
					File src_path_tmp_file_2 = new File(src_path_tmp_2);

					// check src/main/java
					if (src_path_tmp_file.isDirectory()){
						src_path = src_path_tmp;
					}else if(src_path_tmp_file_2.isDirectory()){
						// check src/java
						src_path = src_path_tmp_2;
					}else{
						// check src
						src_path = fileEntry.getPath();
					}
					break;
				}
			}
		}
//		print("src_path: \n"+ src_path + '\n');
		
		return src_path;
	}

	/**
	 * write {@code string} into file with mode as "not append"
	 * 
	 * @param filePath
	 *            : path of file
	 * @param string
	 *            : message
	 * @return
	 */
	public static boolean writeStringToFile(String filePath, String string) {
		return writeStringToFile(filePath, string, false);
	}

	/**
	 * write {@code string} to file with mode as "not append"
	 * 
	 * @param file
	 *            : file of type {@code File}
	 * @param string
	 *            : message
	 * @return
	 */
	public static boolean writeStringToFile(File file, String string) {
		return writeStringToFile(file, string, false);
	}

	/**
	 * write {@code string} into file with specific mode
	 * 
	 * @param filePath
	 *            : file path
	 * @param string
	 *            : message
	 * @param append
	 *            : writing mode
	 * @return
	 */
	public static boolean writeStringToFile(String filePath, String string, boolean append) {
		if (filePath == null) {
			LocalLog.log("#writeStringToFile Illegal file path : null.");
			return false;
		}
		File file = new File(filePath);
		return writeStringToFile(file, string, append);
	}

	/**
	 * write {@code string} into file with specific mode
	 * 
	 * @param file
	 *            : file of type {@code File}
	 * @param string
	 *            : message
	 * @param append
	 *            : writing mode
	 * @return
	 */
	public static boolean writeStringToFile(File file, String string, boolean append) {
		if (file == null || string == null) {
			LocalLog.log("#writeStringToFile Illegal arguments : null.");
			return false;
		}
		if (!file.exists()) {
			try {
				file.getParentFile().mkdirs();
				file.createNewFile();
			} catch (IOException e) {
				LocalLog.log("#writeStringToFile Create new file failed : " + file.getAbsolutePath());
				return false;
			}
		}
		BufferedWriter bufferedWriter = null;
		try {
			bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, append), "UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			bufferedWriter.write(string);
			bufferedWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (bufferedWriter != null) {
				try {
					bufferedWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return true;
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
	
	public static Subject getSubject(String name, int id){
		String fileName = Constant.PROJ_INFO + "/" + name + "/" + id + ".txt";
		File file = new File(fileName);
		if(!file.exists()){
			System.out.println("File : " + fileName + " does not exist!");
			return null;
		}
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		String line = null;
		List<String> source = new ArrayList<>();
		try {
			while((line = br.readLine()) != null){
				source.add(line);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(source.size() < 4){
			System.err.println("PROJEC INFO CONFIGURE ERROR !");
			System.exit(0);
		}
		
		String ssrc = source.get(0);
		String sbin = source.get(1);
		String tsrc = source.get(2);
		String tbin = source.get(3);
		
		Subject subject = new Subject(name, id, ssrc, tsrc, sbin, tbin);
		return subject;
	}
	
	/**
	 * use print() to replace System.out.println()
	 * @param str
	 */
	public static void print(String str){
		System.out.println(str);
	}

	public static void checkPatchType(List<String> fixed_chunk, String proj, String id, Map<String, Pair<String, String>> lineTypesMap) throws IOException {
		List<String> lines = readFile(proj, id, "modification");
		for(int i = 0; i < lines.size(); i++){
			String line = lines.get(i).trim();
			// get modification of the patch line.
			if(fixed_chunk.contains(line)){
				String modification = lines.get(i+1).trim();
				// Chart 1: only contains =
				if (basicType.contains(modification)){
					lineTypesMap.put(line, new Pair<>("intrinsic", modification));
				}else{
					lineTypesMap.put(line, new Pair<>("type2", modification));
				}
			}
		}
	}
	
}
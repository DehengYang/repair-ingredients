package edu.lu.uni.serval.BugCommit;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import edu.lu.uni.serval.Configuration;
import edu.lu.uni.serval.utils.FileHelper;

/*
 * Added by apr
 * This is to get all D4J bugs diff info
 * 
 */
public class BugDiff {
	public static Date getCommitTime(String commitId) throws IOException, ParseException{
		String[] cmd = {"/bin/sh","-c", "cd " + Configuration.SUBJECTS_PATH + Configuration.PROJECT 
				+ " && " + " git show -s --format=%ci " 
				+ commitId
				};
		String gitTime = BugDiff.shellRun2(cmd).trim();
		
		String timeLine = gitTime.split("\\+")[0].trim();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		//debug
//		System.out.format("commitId: %s, timeLine: %s\n", commitId, timeLine);
		
		Date commitTime = format.parse(timeLine);
		return commitTime;
	}
	
	public String getSrc(String proj, int id){
		String allPath = Configuration.HOME + "../d4j-info/src_path/" + proj.toLowerCase() + "/" + id + ".txt" ;
		return FileHelper.readFile(allPath).split("\n")[0]; // srcPath
	}
	
	
	public void rmRepoDir(String proj, int id) throws IOException{
		String repoBuggy = Configuration.D4J_REPO;
		String repoFixed = Configuration.D4J_REPO + "fixed_bugs_dir/";
		
		
		String srcPath = getSrc(proj, id);
		String buggySrcPath = repoBuggy + proj + "/" + proj + "_" + id + srcPath;
		String fixedrcPath = repoFixed + proj + "/" + proj + "_" + id + srcPath;
		
		String buggySrcPath1 = repoBuggy + proj + "/" + proj + "_" + id;
		String fixedrcPath1 = repoFixed + proj + "/" + proj + "_" + id;
		String buggySrcPath2 = repoBuggy + "/repo/" + proj + "/" + proj + "_" + id;
		String fixedrcPath2 = repoFixed + "/repo/" + proj + "/" + proj + "_" + id;
		
		
		// remove dir
		String[] cmdRemove1 = {"/bin/sh","-c", "cd " + repoBuggy 
				+ " && " + "rm -rf  " + buggySrcPath1 + " && rm -rf  " + buggySrcPath2
				};
		shellRun2(cmdRemove1); 
		String[] cmdRemove2 = {"/bin/sh","-c", "cd " + repoBuggy 
				+ " && rm -rf  " + fixedrcPath1 + " && rm -rf  " + fixedrcPath2
				};
		shellRun2(cmdRemove2); 
	}
	
	/*
	 * get diff info by running shell script
	 */
	public String getShellDiff(String proj, int id) throws IOException{
		String repoBuggy = Configuration.D4J_REPO;
		String repoFixed = Configuration.D4J_REPO + "fixed_bugs_dir/";
		String[] cmd = {"/bin/sh","-c", "cd " + repoBuggy 
				+ " && " + "/bin/bash single-download.sh "
				+ proj + " " + id + " 1"};
		shellRun2(cmd); // download buggy program
		
		String[] cmd2 = {"/bin/sh","-c", "cd " + repoFixed 
				+ " && " + "/bin/bash  fixed_single_download.sh "
				+ proj + " " + id + " 1"};
		shellRun2(cmd2); // download fixed program
		
		String srcPath = getSrc(proj, id);
		String buggySrcPath = repoBuggy + proj + "/" + proj + "_" + id + srcPath;
		String fixedrcPath = repoFixed + proj + "/" + proj + "_" + id + srcPath;
		
		String buggySrcPath1 = repoBuggy + proj + "/" + proj + "_" + id;
		String fixedrcPath1 = repoFixed + proj + "/" + proj + "_" + id;
		String buggySrcPath2 = repoBuggy + "/repo/" + proj + "/" + proj + "_" + id;
		String fixedrcPath2 = repoFixed + "/repo/" + proj + "/" + proj + "_" + id;
		
		String[] cmd3 = {"/bin/sh","-c", "cd " + repoBuggy 
				+ " && " + "/usr/bin/diff -Naur " 
				+ buggySrcPath + " " + fixedrcPath
				};
		String shellDiff = shellRun2(cmd3);
		
		// remove dir
//		String[] cmdRemove1 = {"/bin/sh","-c", "cd " + repoBuggy 
//				+ " && " + "rm -rf  " + buggySrcPath1 + " && rm -rf  " + buggySrcPath2
//				};
//		shellRun2(cmdRemove1); 
//		String[] cmdRemove2 = {"/bin/sh","-c", "cd " + repoBuggy 
//				+ " && rm -rf  " + fixedrcPath1 + " && rm -rf  " + fixedrcPath2
//				};
//		shellRun2(cmdRemove2); 
		
//		System.out.println("shellDiff: \n" + shellDiff); //debug
		return shellDiff;
	}
	
	/*
	 * Not only target chart, but also other programs.
	 */
	public Map<Integer, List<String>> getChart() throws IOException{
		String proj = Configuration.PROJ_BUG;
		Map<Integer, List<String>> diffMap = new HashMap<>();
		
		// e.g., for chart, from Chart-1 to Chart-26
		for(int id = 1; id <= Configuration.numOfBugs.get(Configuration.PROJ_BUG); id++){ //Configuration.numOfBugs.get(Configuration.PROJ_BUG)

			if (Configuration.singleBug && 
					Integer.parseInt(Configuration.ID) != id){
				continue;
			}
			// seems a method to get diff, but not used
//			String diffPath = "/home/apr/env/defects4j/framework/projects/Chart/patches/" + id + ".src.patch";
//			String diffInfo = FileHelper.readFile(diffPath);
			
			String shellDiff = getShellDiff(proj, id);
			
			// save shell diff
			String targetPath = Configuration.BUGS + proj + "/" + id + "/bugDiff.txt";
			FileHelper.outputToFile(targetPath, shellDiff, false);
			
			// find and save buggy file(s) & fixed file(s)
			String[] diffLines = shellDiff.split("\n");
			List<String> diffHunkList = new ArrayList<>();
			boolean curFlag = false;
			for (String line : diffLines){
				// save buggy and fixed files
				if(line.length() > 4){
					if(line.substring(0,4).equals("--- ")){
						cpBugFix(line, "bug",proj,id);
					}
					if(line.substring(0,4).equals("+++ ")){
						cpBugFix(line, "fix",proj,id);
					}
				}
				
				if(line.length() > 1){
					// is a diff info
//					System.out.format("1:%s\n2:%s\n3:%s\n",line.substring(0,1), line.substring(1,2), line );
//					System.out.print(line.substring(0,1).equals("-"));
//					System.out.print(line.substring(0,1).equals("+"));
					if( (line.substring(0,1).equals("-") ||    //debug: change '-' to "-"
							line.substring(0,1).equals("+")) //debug: change '+' to "+"
							&& line.substring(1,2).equals(" ")){
						if(curFlag == true){
							String diff = diffHunkList.get(diffHunkList.size() - 1);
							diff = diff + line;
							diffHunkList.set(diffHunkList.size() - 1, diff.trim());
						}else{
							diffHunkList.add(line);
						}
						
						curFlag = true; // is a diff
						// save diff into diffHunkList, only conjunctive lines are saved.
					}else{
						curFlag = false;
					}
				}else{
					curFlag = false;
				}
			}
			
			rmRepoDir(proj, id);
			
			diffMap.put(id, diffHunkList);
		}
		return diffMap;
	}
	
	
	private void cpBugFix(String line, String flag, String proj, int id) throws IOException {
		String buggyPath = line.split(" ")[1].split("\t")[0];
		File buggyFile = new File(buggyPath);
		String fileName;
		if (flag.equals("bug")){
			fileName = "buggy-" + buggyFile.getName();
		}else{
			fileName = "fixed-" + buggyFile.getName();
		}
		
		String targetPath = Configuration.BUGS + proj + "/" + id + "/" + fileName;
		///home/apr/ALL_APR_TOOLS/Pre-PatchParse/PatchParser-D4J/data/PatchCommits/Keywords/jfreechart/Chart/1/
		FileHelper.outputToFile(targetPath, "", false);
		String result = shellRun2("cp " + buggyPath + " " + targetPath);
		
		// remove dir now.
	}

	// simple run
	public static String shellRun2(String cmd) throws IOException {
//		System.out.format("1 cmd to run: %s\n\n",cmd);
        Process process= Runtime.getRuntime().exec(cmd);
        String results = getShellOut(process);
        return results;
	}
	// simple run
	public static String shellRun2(String[] cmd) throws IOException {
		String str = "";
		for(int i = 0;i<cmd.length;i++){
			str += cmd[i] + " ";
		}
//		System.out.format("2 cmd to run: %s\n\n",str);
		Process process= Runtime.getRuntime().exec(cmd);
		String results = getShellOut(process);
		return results;
	}
	
	private static String getShellOut(Process process) {
		ExecutorService service = Executors.newSingleThreadExecutor();
        Future<String> future = service.submit(new ReadShellProcess(process));
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
}

class ReadShellProcess implements Callable<String> {
    public Process process;

    public ReadShellProcess(Process p) {
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

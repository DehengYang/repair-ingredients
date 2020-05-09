package edu.lu.uni.serval.BugCommit;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.revwalk.RevCommit;

import edu.lu.uni.serval.Configuration;
import edu.lu.uni.serval.BugCommit.parser.Pair;
import edu.lu.uni.serval.git.exception.GitRepositoryNotFoundException;
import edu.lu.uni.serval.git.exception.NotValidGitRepositoryException;
import edu.lu.uni.serval.git.travel.CommitDiffEntry;
import edu.lu.uni.serval.git.travel.GitRepository;
import edu.lu.uni.serval.utils.FileHelper;

//modified by apr
public class PatchRelatedCommits {
	
	// Parameters:
	// projectsPath: Configuration.SUBJECTS_PATH -> ../subjects/
	// outputPath: Configuration.PATCH_COMMITS_PATH -> ../data/PatchCommits/
	// *urlPath: Configuration.BUG_REPORTS_PATH -> ../data/BugReports/
	// * means not used now.
	public void collectCommits(String projectsPath, String outputPath, String urlPath) {
		File[] projects = new File(projectsPath).listFiles();
		
		// delete/init ../data/PatchCommits/ dir.
		if(Configuration.DELETE_PatchCommitsDir){
			FileHelper.deleteDirectory(outputPath);
		}

		for (File project : projects) {
			// only collect Configuration.PROJECT
			if (!project.isDirectory()  || 
					! project.getName().equals(Configuration.PROJECT)) {
				continue;
			}
			
			// parse repo
			String repoName = project.getName();
			String revisedFilesPath = "";
			String previousFilesPath = "";
			String gitRepoPath = project.getPath() + "/.git";
			GitRepository gitRepo = new GitRepository(gitRepoPath, revisedFilesPath, previousFilesPath);
			try {
				System.out.println("\nProject: " + repoName);
				gitRepo.open();
				
				List<RevCommit> commits = gitRepo.getAllCommits(false);
				System.out.println("All Commits: " + commits.size());
				
				//get all target (e.g., Chart) bugs diff info 
				Configuration.PROJECT = project.getName();
				BugDiff bugDiff = new BugDiff();
				Map<Integer, List<String>>  diffMap = bugDiff.getChart();  // id, diffHunkList
				Map<String,  List<Pair<String, String>>>  commitMap = new HashMap<>();
				List<CommitDiffEntry> commitsDiffentries = gitRepo.getCommitDiffEntries(commits);
				// TODO: only false in debugging mode.
				gitRepo.createFilesForGumTree(outputPath + "Keywords/" + repoName + "_allCommits/", commitsDiffentries, commitMap, Configuration.PRINT_ALLCOMMIT);
//				gitRepo.outputCommitMessages(outputPath + "CommitMessage/" + repoName + "_allCommits.txt", commits);
				
//				matchCommitId2(); // read from commit-db
				if (Configuration.PROJ_BUG.equals("Closure")){
					System.out.println("Read commit id from commit-db.");
					matchCommitId2(); // read from commit-db
				}else{
					System.out.println("Match commit id from all commits.");
					matchCommitId(diffMap, commitMap);
				}
				
				List<RevCommit> keywordPatchCommits = gitRepo.filterCommits(commits); // searched by keywords.
				System.out.println("Keywords-matching Commits: " + keywordPatchCommits.size());
				
				List<CommitDiffEntry> unlinkedPatchCommitDiffentries = gitRepo.getCommitDiffEntries(keywordPatchCommits);
				gitRepo.createFilesForGumTree(outputPath + "Keywords/" + repoName + "/", unlinkedPatchCommitDiffentries, true);
				gitRepo.outputCommitMessages(outputPath + "CommitMessage/" + repoName + "_Keywords.txt", keywordPatchCommits);
//				gitRepo.outputCommitMessages(outputPath + "CommitMessage/" + repoName + "_Linked.txt", linkedPatchCommits);
				
				outputCommitIds(outputPath + "CommitIds/" + repoName + "_Keywords.txt", keywordPatchCommits);
//				outputCommitIds(outputPath + "CommitIds/" + repoName + "_Linked.txt", linkedPatchCommits);
			} catch (GitRepositoryNotFoundException e) {
				e.printStackTrace();
			} catch (NotValidGitRepositoryException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NoHeadException e) {
				e.printStackTrace();
			} catch (GitAPIException e) {
				e.printStackTrace();
			} finally {
				gitRepo.close();
			}
		}
	}

	// only for chart, of which the commit-db does not contain commit ids.
	private void matchCommitId(Map<Integer, List<String>> diffMap, Map<String, List<Pair<String, String>>> commitMap) throws IOException {
		// traverse all commits of repo. (e.g., jfreechart)
		for(Map.Entry<String, List<Pair<String,String>>> entry : commitMap.entrySet()){
			String commitId = entry.getKey();
			List<Pair<String,String>> diffEntryList =  entry.getValue();
			
			// for debug. Chart 3
//			if (!commitId.equals("00ebcc58")){
//				continue;
//			}
			
			// read all diffs of a commit
			String diffEntryContent = "";
			for(Pair<String,String> diffEntry : diffEntryList){
				File diffEntryFile = new File(diffEntry.getSecond()); 
				// ../data/PatchCommits/Keywords/jfreechart_allCommits/DiffEntries/00ebcc58_cf48acfb_source#org#jfree#data#time#TimeSeries.txt
				if(diffEntryFile.exists()){// check if exist
					diffEntryContent += FileHelper.readFile(diffEntryFile).replace("\n", "");
				}
			}
			
			// each bug diff
			for(Map.Entry<Integer, List<String>> entryBug : diffMap.entrySet()){
				int id = entryBug.getKey();
				List<String> diffHunks = entryBug.getValue();
				int isThisCommitFlag = 0;
				
				for (String diffHunk : diffHunks){
					// when the diff hunk is "- }", may need to skip. i.e., do not add flag with 1
					if (diffEntryContent.indexOf(diffHunk) == -1){
						continue; // not contain 
					}else{
						isThisCommitFlag++;
					}
				}
				
				// This is strict commit match (i.e., ==)
				// if (isThisCommitFlag == diffHunks.size()){
				
				// This is loose commit match (i.e., >= 0.5)
				// if (isThisCommitFlag >= 0.5*diffHunks.size()){ // > 0){
				
				if (isThisCommitFlag == diffHunks.size()){
//					System.out.format("This is a buggy commit: %s\n%s\n%s\n\n", fileName, diffEntry, commitId);
					String targetPath = Configuration.BUGS + Configuration.PROJ_BUG + "/" + id + "/CommitId-" + commitId;
					String[] cmd3 = {"/bin/sh","-c", "cd " + Configuration.SUBJECTS_PATH + Configuration.PROJECT 
							+ " && " + " git show -s --format=%ci " 
							+ commitId
							};
					String commitTime = BugDiff.shellRun2(cmd3);
					
					FileHelper.outputToFile(targetPath, commitTime, true);
				}
			}
		}
	}
	
	// for closure, of which the commit-db is available
	private void matchCommitId2() throws IOException {
		String[] commitContent = FileHelper.readFile(Configuration.commitDB + Configuration.PROJ_BUG + "/commit-db").split("\n");
		Map<Integer, String> idCommitMap = new HashMap<>();
		
		for(String commitLine : commitContent){
			int id = Integer.parseInt(commitLine.split(",")[0]);
			String commitId = commitLine.split(",")[2].trim().substring(0, Configuration.commitIdLength);
			idCommitMap.put(id, commitId);
		}
		
		for(int id = 1; id <= Configuration.numOfBugs.get(Configuration.PROJECT); id++){
			String targetPath = Configuration.BUGS + Configuration.PROJ_BUG + "/" + id + "/CommitId-" + idCommitMap.get(id);
			String[] cmd3 = {"/bin/sh","-c", "cd " + Configuration.SUBJECTS_PATH + Configuration.PROJECT 
					+ " && " + " git show -s --format=%ci " 
					+ idCommitMap.get(id)
					};
			String commitTime = BugDiff.shellRun2(cmd3);
			
			FileHelper.outputToFile(targetPath, commitTime, true);
		}
	}

	private void outputCommitIds(String fileName, List<RevCommit> commits) {
		StringBuilder builder = new StringBuilder("Previous Commit Id ------ Patch Commit Id.");
		for (RevCommit commit : commits) {
			builder.append(commit.getParents()[0].getId().name()).append(" ------ ").append(commit.getId().name());
		}
		FileHelper.outputToFile(fileName, builder, false);
	}

}

package edu.lu.uni.serval.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.CompilationUnit;

import edu.lu.uni.serval.AST.ASTGenerator;
import edu.lu.uni.serval.AST.ASTGenerator.TokenType;
import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.par.templates.FixTemplate;
import edu.lu.uni.serval.parser.JavaFileParser;
import edu.lu.uni.serval.tbar.utils.Checker;

/**
 * Parse the suspicious code into an AST.
 * 
 * @author anonymous
 *
 */
public class SuspiciousCodeParser {

	private File javaFile;
	private CompilationUnit unit = null;
	private ITree suspiciousCodeAstNode = null;
	private String suspiciousCodeStr = null;
	
	private BuggyMethod buggyMethod = null;
	
	// apr
//	private List<ITree>clazzNameList = new ArrayList<ITree>();
	// find only one clazzInstance each time
	private ITree clazzInstance = null;
	public ITree getClazzInstance(){
		return this.clazzInstance;
	}
	public void resetClazzInstance(){
		this.clazzInstance = null;
	}
	
	public CompilationUnit getUnit(){
		return unit;
	}
	
	public void parseJavaFile(File javaFile) {
		this.javaFile = javaFile;
		unit = new JavaFileParser().new MyUnit().createCompilationUnit(javaFile);
	}
	
	public ITree getRootTree(File javaFile) {
		this.javaFile = javaFile;
		unit = new JavaFileParser().new MyUnit().createCompilationUnit(javaFile);
		ITree rootTree = new ASTGenerator().generateTreeForJavaFile(javaFile, TokenType.EXP_JDT);
		return rootTree;
	}
	
	public String getSuperClazz(File javaFile){
		unit = new JavaFileParser().new MyUnit().createCompilationUnit(javaFile);
		ITree rootTree = new ASTGenerator().generateTreeForJavaFile(javaFile, TokenType.EXP_JDT);
		
		for (ITree child : rootTree.getChildren()){
			if(child.getLabel().contains("ClassName:")){
				String[] clazzStrList = child.toString().split(",");
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
				}else{
					return clazz;
				}
			}
		}
		return null;
	}
	
	public void parseSuspiciousCode(File javaFile, int suspLineNum) {
		this.javaFile = javaFile;
		unit = new JavaFileParser().new MyUnit().createCompilationUnit(javaFile);
		ITree rootTree = new ASTGenerator().generateTreeForJavaFile(javaFile, TokenType.EXP_JDT);
		identifySuspiciousCodeAst(rootTree, suspLineNum);
	}

	public void parseSuspiciousMethod(File javaFile, int buggyLine) {
		this.javaFile = javaFile;
		unit = new JavaFileParser().new MyUnit().createCompilationUnit(javaFile);
		ITree rootTree = new ASTGenerator().generateTreeForJavaFile(javaFile, TokenType.EXP_JDT);
		identifySuspiciousMethodAst(rootTree, buggyLine);
	}

	private void identifySuspiciousCodeAst(ITree tree, int suspLineNum) {
		List<ITree> children = tree.getChildren();
		
		for (ITree child : children) {
//			System.out.println("child in identifySuspiciousCodeAst: " + child.toString());
			int startPosition = child.getPos();
			int endPosition = startPosition + child.getLength();
			int startLine = this.unit.getLineNumber(startPosition);
			int endLine = this.unit.getLineNumber(endPosition);
			if (endLine == -1) endLine = this.unit.getLineNumber(endPosition - 1);
			if (startLine <= suspLineNum && suspLineNum <= endLine) {
				if (startLine == suspLineNum || endLine == suspLineNum) {
					if (Checker.isBlock(child.getType())) {
						identifySuspiciousCodeAst(child, suspLineNum);
						continue;
					}else{
						if (!isRequiredAstNode(child)) {
							child = traverseParentNode(child);
							if (child == null) break;
						}
						this.suspiciousCodeAstNode = child;
						this.suspiciousCodeStr = readSuspiciousCode();
//						break;// FIXME: one code line might contain several statements.
					}
				} else {
					identifySuspiciousCodeAst(child, suspLineNum);
				}
				break;
			} else if (startLine > suspLineNum) {
				break;
			}
		}
	}
	
	// apr from SimFix
	public static boolean isClass(String name){
		if(name == null) return false;
		
		// exclude non primitive types
		if (name.equals("String") ||  name.equals("Integer") ||  name.equals("List")
				||  name.equals("Array") ||  name.equals("Double") ||  name.equals("Float")){
			return false;
		}
		
		if(!name.matches("[a-zA-Z]+")){
//			System.out.println("class name judge in isClass (return false): " + name);
			return false;
		}
		
		return Character.isUpperCase(name.charAt(0)) && !name.toUpperCase().equals(name);
	}
	
	// apr
	public void findClazzInstance(ITree tree, FixTemplate ft) {
		
		List<ITree> children = tree.getChildren();
		
		for (ITree child : children) {
//			System.out.println("child in findSimpleName: " + child.toString());
//					+ " type: " + child.getType());
//			if(ft.allVarNamesMap.containsKey(child))
			String label = child.getLabel();
			// sometimes lalel may be "Name:area"
			if (label.contains(":")){
				label = label.split(":")[1];
			}
			String type = "null";
			if(ft.varTypesMap.containsKey(label)){
				type = ft.varTypesMap.get(label);
			}
			// check if class name
			if (Checker.isSimpleName(child.getType()) &&  isClass(type)) {
//				System.out.println("find class instance: " + child.toString());
//				this.clazzNameList.add(child);
				clazzInstance = child;
				break;
			}else{
				findClazzInstance(child,ft);
			}
		}
	}

	private void identifySuspiciousMethodAst(ITree tree, int buggyLine) {
		List<ITree> children = tree.getChildren();
		
		for (ITree child : children) {
			
			int startPosition = child.getPos();
			int endPosition = startPosition + child.getLength();
			int startLine = this.unit.getLineNumber(startPosition);
			int endLine = this.unit.getLineNumber(endPosition);
			if (endLine == -1) endLine = this.unit.getLineNumber(endPosition - 1);
			if (startLine <= buggyLine && buggyLine <= endLine) {
				if (Checker.isMethodDeclaration(child.getType())) {
					buggyMethod = new BuggyMethod();
					buggyMethod.classPath = this.javaFile.getPath();
					buggyMethod.startLine = startLine;
					buggyMethod.endLine = endLine;
					break;
				} else {
					identifySuspiciousMethodAst(child, buggyLine);
				}
			} else if (startLine > buggyLine) {
				break;
			}
		}
	}
	
	private boolean isRequiredAstNode(ITree tree) {
		int astNodeType = tree.getType();
		if (Checker.isStatement(astNodeType) 
				|| Checker.isFieldDeclaration(astNodeType)
				|| Checker.isMethodDeclaration(astNodeType)
				|| Checker.isTypeDeclaration(astNodeType)) {
			return true;
		}
		return false;
	}

	private ITree traverseParentNode(ITree tree) {
		ITree parent = tree.getParent();
		if (parent == null) return null;
		if (!isRequiredAstNode(parent)) {
			parent = traverseParentNode(parent);
		}
		return parent;
	}

	private String readSuspiciousCode() {
		String javaFileContent = FileHelper.readFile(this.javaFile);
		int startPos = this.suspiciousCodeAstNode.getPos();
		int endPos = startPos + this.suspiciousCodeAstNode.getLength();
		return javaFileContent.substring(startPos, endPos);
	}

	public ITree getSuspiciousCodeAstNode() {
		return suspiciousCodeAstNode;
	}

	public String getSuspiciousCodeStr() {
		return suspiciousCodeStr;
	}
	
	public BuggyMethod getBuggMethod() {
		return buggyMethod;
	}

	public class BuggyMethod {
		public String classPath;
//		public String methodName;
		public int startLine;
		public int endLine;
	}
	
}

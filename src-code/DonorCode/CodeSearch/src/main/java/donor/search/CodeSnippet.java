package donor.search;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;


public class CodeSnippet {
	private CompilationUnit unit = null;
	private int extendedLine = 0;
	private int last_extendedLine = 0;
	private Statement extendedStatement = null;
	private int lineRange = 0;
	private List<ASTNode> nodes = new ArrayList<>(); 
	private int currentLines = 0;
	private int MAX_LESS_THRESHOLD = 0;
	private int MAX_MORE_THRESHOLD = 5;
	
	public CodeSnippet(CompilationUnit unit, int extendedLine, int lineRange) {
		this(unit, extendedLine, lineRange, null);
	}
	
	public CodeSnippet(CompilationUnit unit, int extendedLine, int lineRange, Statement extendedStatement){
		this(unit, extendedLine, lineRange, extendedStatement, 0, 0);
	}
	
	// initialization. 
	// e.g., unit, lineNo, 5, null, 3
	public CodeSnippet(CompilationUnit unit, List<Integer> linesList,
			int lineRange, Statement extendedStatement,
			int max_less_threshold, int max_more_threshold) {
		this.unit = unit;
		this.extendedLine = linesList.get(0);
		this.last_extendedLine = last_extendedLine;
		this.lineRange = lineRange;
		this.extendedStatement = extendedStatement;
		this.MAX_LESS_THRESHOLD = max_less_threshold;
		this.MAX_MORE_THRESHOLD = max_more_threshold;
		
//		for (int lineNo = extendedLine; lineNo <= last_extendedLine; lineNo ++){
		for(int lineNo : linesList){
			// bug fix: exclude repeated AST 
			int repeat = 0;
			for (ASTNode node : nodes){
				int start = unit.getLineNumber(node.getStartPosition());
				int end = unit.getLineNumber(node.getStartPosition() + node.getLength());
				if (start <= lineNo && lineNo <= end){
					repeat = 1;
					break;
				}
			}
			if (repeat == 0){
				searchNodes(lineNo);
			}
			
		}
//		searchNodes(extendedLine);
//		searchNodes(last_extendedLine);
	}
	public CodeSnippet(CompilationUnit unit, int first_extendedLine,
			int lineRange, Statement extendedStatement,
			int max_less_threshold, int max_more_threshold) {
		this.unit = unit;
		this.extendedLine = first_extendedLine;
		this.last_extendedLine = last_extendedLine;
		this.lineRange = lineRange;
		this.extendedStatement = extendedStatement;
		this.MAX_LESS_THRESHOLD = max_less_threshold;
		this.MAX_MORE_THRESHOLD = max_more_threshold;
		search();
	}
	
	public List<ASTNode> getASTNodes(){
		return nodes;
	}
	
	// for only adding first and last line
	private void searchNodes(int line){
		// initialize
		this.extendedStatement = null;
		extendedLine = line;
		
		// 0 is column
		int position = this.unit.getPosition(line, 0);

		// Instantiate a new node finder using the given root node, the given start and the given length.
		// TODO: I think 20 can also be changed to 100, 1000. The length here seems do not influence the results.
		NodeFinder finder = new NodeFinder(this.unit, position, 20);

		// Returns the innermost node that fully contains the selection.
		ASTNode prefind = finder.getCoveringNode();
		// comment print 
//		Main.print("-prefind class name: " + prefind.getClass().getName());
		
		// prefind is not null, and prefind is not Statement
		while (prefind != null && !(prefind instanceof Statement)) {	
			// Returns this node's parent node, or null if this is the root node.
			prefind = prefind.getParent();
//			Main.print("prefind class name: " + prefind.getClass().getName());
		}

//		if (prefind != null){
//			// comment print 			
////			Main.print("-prefind class name: " + prefind.getClass().getName());
//		}

		// the accept method is to find the extendedLine ast node (by traverse visiting).
		if(prefind != null){
			// Accepts the given visitor on a visit of the current node.
			prefind.accept(new FindExactLineVisitor());
		} else {
			// comment print 
//			System.out.println("Executing branch: this.unit.accept(new Traverse());");
			this.unit.accept(new Traverse());
		}
		
		// directly add extendedStatement
		if(this.extendedStatement != null){
			this.nodes.add(this.extendedStatement);
		}
	}
	
	private void search(){
		// if the extended statement/line is not given
		if(this.extendedStatement == null){
			// 0 is column
//			this.extendedLine = 254; // just for test.
			int position = this.unit.getPosition(this.extendedLine, 0);
			
			// Instantiate a new node finder using the given root node, the given start and the given length.
			// TODO: I think 20 can also be changed to 100, 1000. The length here seems do not influence the results.
			NodeFinder finder = new NodeFinder(this.unit, position, 20);
			
			// Returns the innermost node that fully contains the selection.
			ASTNode prefind = finder.getCoveringNode();
			
			// TODO: 
			// Exception in thread "main" java.lang.NullPointerException for chart 3
//			Main.print("prefind class name: " + prefind.getClass().getName());
			
			//LocalLog.log("prefind:" + prefind.toString());
			
			// prefind is not null, and prefind is not Statement
			while (prefind != null && !(prefind instanceof Statement)) {
				// comment print 
//				Main.print("prefind class name: " + prefind.getClass().getName());
				
				// Returns this node's parent node, or null if this is the root node.
				prefind = prefind.getParent();
			}
			
//			if (prefind != null){
//				Main.print("prefind class name: " + prefind.getClass().getName());
//			}
			
			// the accept method is to find the extendedLine ast node (by traverse visiting).
			if(prefind != null){
				// Accepts the given visitor on a visit of the current node.
				prefind.accept(new FindExactLineVisitor());
			} else {
				// TODO: I cannot imagine how the program comes into this branch.
				// Answer: when String line_path = "/home/apr/d4j/Chart/Chart_1/source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java";
				//         and 	int lineNo = 188;
//				System.out.println("Executing branch: this.unit.accept(new Traverse());");
				this.unit.accept(new Traverse());
			}
		}
		// extend the statement to meet the requirement
		// this extendedStatement may be null when the lineNo is not a completeline
		// e.g., Chart 1 line 254 org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java
        // this.legendItemLabelGenerator
        // = new StandardCategorySeriesLabelGenerator(); // with line break
//		LocalLog.log("this.extendedStatement:\n" + this.extendedStatement.toString());
		if(this.extendedStatement != null){
			// conduct a simple extend
			List<ASTNode> list = simpleExtend(this.extendedStatement);
			// if successfully extend
			if(list.size() > 0){
				this.nodes.addAll(list);
			} else {
				// if fail to extend
				this.currentLines = getValidLineNumber(this.extendedStatement);
				// if currentLines < 5 + 3 = 8
				if(this.lineRange - this.currentLines > MAX_LESS_THRESHOLD){
					// reset currentLines to 0
					this.currentLines = 0;
					
					// conduct extend
					this.nodes = extend(this.extendedStatement);
				} else {
					// if block
					if(this.extendedStatement instanceof Block){
						ASTNode node = this.extendedStatement.getParent();
						if (node instanceof IfStatement || node instanceof SwitchCase || node instanceof ForStatement
								|| node instanceof EnhancedForStatement || node instanceof WhileStatement) {
							this.extendedStatement = (Statement) node;
						}
					}
					// directly add extendedStatement if not block, otherwise add parent node
					this.nodes.add(this.extendedStatement);
				}
			}
		}
	}
	
	// extend the extended_statement (node)
	private List<ASTNode> extend(ASTNode node){
		List<ASTNode> result = new ArrayList<>();
	    List<ASTNode> list = getAllSiblingNodes(node);
	    int selfIndex = -1;
	    // get index of given node in the sibling nodes list
	    for(int i = 0; i < list.size(); i++){
	    	if(list.get(i) == node){
	    		selfIndex = i;
	    		break;
	    	}
	    }
	    // find self position
	    if(selfIndex != -1){
	    	int left = selfIndex - 1;
	    	int right = selfIndex + 1;
	    	boolean leftExt = true;
	    	boolean rightExt = true;
	    	// while currentLines < 8
	    	while(lineRange - currentLines > MAX_LESS_THRESHOLD){
	    		boolean extended = false;
	    		int leftLine = Integer.MAX_VALUE;
	    		int rightLine = Integer.MAX_VALUE;
	    		
	    		// if left is valid (not < 0) and leftExtend flag is true
	    		if(left >= 0 && leftExt){
	    			// count left node lines 
	    			leftLine = getValidLineNumber(list.get(left));
	    			
	    			// if left is switch, set leftExt flag as false
	    			// TODO: why?
	    			if(list.get(left) instanceof SwitchCase){
	    				leftExt = false;
	    			}
	    			
	    			// currentLines + leftLine < 10
	    			if((currentLines + leftLine - lineRange) < MAX_MORE_THRESHOLD ){
	    				currentLines += leftLine;
	    				left --;
	    				extended = true;
	    			}
	    		}
	    		
	    		// if right is valid and rightExt flag is true
	    		if(right < list.size() && rightExt){
	    			rightLine = getValidLineNumber(list.get(right));
	    			
	    			// same as left
	    			if(list.get(right) instanceof SwitchCase){
	    				rightExt = false;
	    			}
	    			
	    			// if currentLines + rightLine < 10
	    			if((currentLines + rightLine - lineRange) < MAX_MORE_THRESHOLD){
	    				currentLines += rightLine;
	    				right ++;
	    				extended = true;
	    			}
	    		}
	    		
	    		// if fail to extend for both left and right siblings
	    		if(!extended){
	    			if(leftLine != Integer.MAX_VALUE || rightLine != Integer.MAX_VALUE){
	    				// try to add least size sibling
	    				if(leftLine < rightLine){
	    					currentLines += leftLine;
	    					left --;
	    				}
	    			}
	    			break;
	    		}
	    	}
	    	
	    	// if currentLines < 10 and currentLines < 8 and node parent is not MD and Switch
	    	// TODO: this seems a bug here.
			if ((currentLines - lineRange) < MAX_MORE_THRESHOLD && lineRange - currentLines > MAX_LESS_THRESHOLD
					&& !(node.getParent() instanceof MethodDeclaration) && !(node.getParent() instanceof SwitchStatement)) {
				currentLines = 0;
				// extend to its parent
				result.addAll(extend(node.getParent()));
			} else {
				boolean first = true;
		    	for(int i = left + 1; i < right; i ++){
		    		// if first is true and left is valid; and left is Switch.
		    		if(first && left >= 0 && list.get(left) instanceof SwitchCase){
		    			result.add(list.get(left));
		    		}
		    		
		    		// first == false
		    		first = false;
		    		
		    		// add left node
		    		result.add(list.get(i));
		    	}
	    	}
		// else: selfIndex == -1. cannot find sibling
		// TODO: I cannot guess how to come into this branch
	    } else {
	    	ASTNode parent = node.getParent();
	    	int line = getValidLineNumber(parent);
	    	if(line < lineRange){
	    		// if MD, directly add 
	    		if(parent instanceof MethodDeclaration){
	    			result.add(node);
	    		} else {
	    			// else extend parent
	    			result.addAll(extend(parent));
	    		}
	    	} else {
	    		// TODO: Those if-else statements inside this method are actually doing a trade-off
	    		
	    		// if line > 10 || parent is MD
	    		if(line - lineRange > MAX_MORE_THRESHOLD || parent instanceof MethodDeclaration){
	    			result.add(node);
	    		} else {
	    			result.add(parent);
	    		}
	    	}
	    }
	    return result;
	}
	
	// get all sibling node of the given node
	public static List<ASTNode> getAllSiblingNodes(ASTNode node){
		List<ASTNode> siblings = new ArrayList<>();
		StructuralPropertyDescriptor structuralPropertyDescriptor = node.getLocationInParent();
		if (structuralPropertyDescriptor == null) {
			// return null list
			return siblings;
		} else if(structuralPropertyDescriptor.isChildListProperty()){
			List list = (List) node.getParent().getStructuralProperty(structuralPropertyDescriptor);
			for(Object object : list){
				if(object instanceof ASTNode){
					siblings.add((ASTNode) object);
				}
			}
		} 
//		else if(structuralPropertyDescriptor.isChildProperty()){
//			ASTNode child = (ASTNode) node.getParent().getStructuralProperty(structuralPropertyDescriptor);
//			siblings.add(child);
//		}
		return siblings;
 	}
	
	private List<ASTNode> simpleExtend(ASTNode node){
		List<ASTNode> rslt = new ArrayList<>();
		ASTNode parent = node;
		while (parent != null) {
			// if the ast is if/for/while/do
			if (parent instanceof IfStatement || parent instanceof ForStatement
					|| parent instanceof EnhancedForStatement || parent instanceof DoStatement
					|| parent instanceof WhileStatement) {
				int line = getValidLineNumber(parent);
				// MAX_MORE_THRESHOLD = 5
				// lineRange = 5
				// max_less_threshold = 3
				// if this if/for/while statement block size (lines) less than 10
				if(line - lineRange < MAX_MORE_THRESHOLD){
					rslt.add(parent);
					currentLines = line;
				}
				break;
			} else if(parent instanceof MethodDeclaration){
				// transfer to methodDeclaration
				// Method declaration AST node type. A method declaration is the union of a method declaration and a constructor declaration. 
				MethodDeclaration mdDeclaration = (MethodDeclaration) parent;
				
				// Block statement AST node type. 
				//  	getBody()
				// Returns the body of this method declaration, or null if this method has no body.
				Block block = mdDeclaration.getBody();
				int line = 0;
				
				// statements(): Returns the live list of statements in this block.
				// this is to get the lines count of this MD
				for(Object object : block.statements()){
					line += getValidLineNumber((ASTNode) object);
				}
				
				// line < 10
				if(line - lineRange < MAX_MORE_THRESHOLD){
					for(Object object : block.statements()){
						rslt.add((ASTNode) object);
					}
					break;
				}
			}
			parent = parent.getParent();
		}
		
		// print result info
		// comment print 
//		if (rslt.size() == 0){
//			LocalLog.log("Find no ast nodes in SimpleExtend()");
//		}else{
//			LocalLog.log("Ast nodes in SimpleExtend():");
//			for (ASTNode astnode : rslt){
//				// Returns a string representation of this node suitable for debugging purposes only.
//				LocalLog.log(astnode.toString());
//			}
//		}
		
		return rslt;
	}
	
	// build method info str
	/*
	 * return : currentClassName + method retType + method name + method paras
	 */
	public static  String buildMethodInfoString(MethodDeclaration node) {
		// get full class name 
		String currentClassName = getFullClazzName(node);
		if (currentClassName == null) {
			return null;
		}
		StringBuffer buffer = new StringBuffer(currentClassName + "#");

		String retType = "?";
		// get return type of the MD
		if (node.getReturnType2() != null) {
			retType = node.getReturnType2().toString();
		}
		StringBuffer param = new StringBuffer("?");
		// for all node(MD) paras
		for (Object object : node.parameters()) {
			// if not single var
			if (!(object instanceof SingleVariableDeclaration)) {
				param.append(",?");
			} else {
				SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration) object;
				param.append("," + singleVariableDeclaration.getType().toString());
			}
		}
		// add method return type
		buffer.append(retType + "#");
		// add method name
		buffer.append(node.getName().getFullyQualifiedName() + "#");
		// add method params, NOTE: the first parameter starts at index 1.
		buffer.append(param);
		return buffer.toString();
	}
	
	private static String getFullClazzName(MethodDeclaration node) {
		String clazz = "";
		// filter those methods that defined in anonymous classes
		ASTNode parent = node.getParent();
		while (parent != null) {
			// parent is CIC: create a class instance 
			if (parent instanceof ClassInstanceCreation) {
				return null;
			// parent is TD
			} else if(parent instanceof TypeDeclaration){
				clazz = ((TypeDeclaration) parent).getName().getFullyQualifiedName();
				break;
			} else if(parent instanceof EnumDeclaration){
				clazz = ((EnumDeclaration) parent).getName().getFullyQualifiedName();
				break;
			}
			parent = parent.getParent();
		}
		if(parent != null){
			while(parent != null){
				if(parent instanceof CompilationUnit){
					String packageName = ((CompilationUnit) parent).getPackage().getName().getFullyQualifiedName();
					// this is to return full clazz name (with package name)
					clazz = packageName + "." + clazz;
					return clazz;
				}
				parent = parent.getParent();
			}
		}
		return null;
	}
	
	// get number of valid lines 
	public static int getValidLineNumber(ASTNode statement){
		if(statement == null){
			return 0;
		}
		String[] contents = statement.toString().split("\n");
		int line = 0;
		boolean comment_start_flag = false;
		for(String string : contents){
			string = string.trim();
			// empty line
			if(string.length() == 0){
				continue;
			}
			// comment for single line
			if(string.startsWith("//")){
				continue;
			}
			// comment start for multi-lines
			if(string.startsWith("/*")){
				comment_start_flag = true;
				continue;
			}
			// comment end for multi-lines
			if(string.contains("*/")){
				comment_start_flag = false;
				continue;
			}
			// comment in multi-lines
			if(comment_start_flag){
				continue;
			}
			// meaningless lines
			if(string.equals("{") || string.equals("}")){
				continue;
			}
			line ++;
		}
		return line;
	}
	
	class Traverse extends ASTVisitor {
		
		public boolean visit(MethodDeclaration node){
			
			int start = unit.getLineNumber(node.getStartPosition());
			int end = unit.getLineNumber(node.getStartPosition() + node.getLength());
			if(start <= extendedLine && extendedLine <= end){
				FindExactLineVisitor visitor = new FindExactLineVisitor();
				node.accept(visitor);
				return false;
			}
			return true;
		}
	}
	
	/**
	 * find statement of exact line number
	 * @author Jiajun
	 * @date Jun 14, 2017
	 */
	class FindExactLineVisitor extends ASTVisitor{
		
		// Visits the given type-specific AST node.
		public boolean visit(AssertStatement node) {
			int start = unit.getLineNumber(node.getStartPosition());
			if(start == extendedLine){
				extendedStatement = node;
				return false;
			}
			return true;
		}
		
		public boolean visit(BreakStatement node) {
			int start = unit.getLineNumber(node.getStartPosition());
			if(start == extendedLine){
				extendedStatement = node;
				return false;
			}
			return true;
		}
		
		public boolean visit(Block node) {
			return true;
		}
		
		public boolean visit(ConstructorInvocation node) {
			int start = unit.getLineNumber(node.getStartPosition());
			if(start == extendedLine){
				extendedStatement = node;
				return false;
			}
			return true;
		}
		
		public boolean visit(ContinueStatement node) {
			int start = unit.getLineNumber(node.getStartPosition());
			if(start == extendedLine){
				extendedStatement = node;
				return false;
			}
			return true;
		}
		
		public boolean visit(DoStatement node) {
			int start = unit.getLineNumber(node.getExpression().getStartPosition());
			if(start == extendedLine){
				extendedStatement = node;
				return false;
			}
			return true;
		}
		
		public boolean visit(EmptyStatement node) {
			return true;
		}
		
		public boolean visit(EnhancedForStatement node) {
			int start = unit.getLineNumber(node.getExpression().getStartPosition());
			if(start == extendedLine){
				extendedStatement = node;
				return false;
			}
			return true;
		}
		
		public boolean visit(ExpressionStatement node) {
			int start = unit.getLineNumber(node.getStartPosition());
			if(start == extendedLine){
				extendedStatement = node;
				return false;
			}
			return true;
		}
		
		public boolean visit(ForStatement node) {
			int position = 0;
			if(node.getExpression() != null){
				position = node.getExpression().getStartPosition();
			} else if(node.initializers() != null && node.initializers().size() > 0){
				position = ((ASTNode)node.initializers().get(0)).getStartPosition();
			} else if(node.updaters() != null && node.updaters().size() > 0){
				position = ((ASTNode)node.updaters().get(0)).getStartPosition();
			}
			int start = unit.getLineNumber(position);
			if(start == extendedLine){
				extendedStatement = node;
				return false;
			}
			return true;
		}
		
		public boolean visit(IfStatement node) {
			int start = unit.getLineNumber(node.getExpression().getStartPosition());
			if(start == extendedLine){
				extendedStatement = node;
				return false;
			}
			return true;
		}
		
		public boolean visit(LabeledStatement node) {
			int start = unit.getLineNumber(node.getStartPosition());
			if(start == extendedLine){
				extendedStatement = node;
				return false;
			}
			return true;
		}
		
		public boolean visit(ReturnStatement node) {
			int start = unit.getLineNumber(node.getStartPosition());
			if(start == extendedLine){
				extendedStatement = node;
				return false;
			}
			return true;
		}
		
		public boolean visit(SuperConstructorInvocation node) {
			int start = unit.getLineNumber(node.getStartPosition());
			if(start == extendedLine){
				extendedStatement = node;
				return false;
			}
			return true;
		}
		
		public boolean visit(SwitchCase node) {
			int start = unit.getLineNumber(node.getStartPosition());
			if(start == extendedLine){
				extendedStatement = node;
				return false;
			}
			return true;
		}
		
		public boolean visit(SwitchStatement node) {
			return true;
		}
		
		public boolean visit(SynchronizedStatement node) {
			return true;
		}
		
		public boolean visit(ThrowStatement node) {
			int start = unit.getLineNumber(node.getStartPosition());
			if(start == extendedLine){
				extendedStatement = node;
				return false;
			}
			return true;
		}
		
		public boolean visit(TryStatement node) {
			return true;
		}
		
		public boolean visit(TypeDeclarationStatement node){
			return true;
		}
		
		public boolean visit(VariableDeclarationStatement node){
			int start = unit.getLineNumber(node.getStartPosition());
			if(start == extendedLine){
				extendedStatement = node;
				return false;
			}
			return true;
		}
		
		public boolean visit(WhileStatement node) {
			int start = unit.getLineNumber(node.getExpression().getStartPosition());
			if(start == extendedLine){
				extendedStatement = node;
				return false;
			}
			return true;
		}
	}
}

/**
 * Copyright (C) anonymous. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by anonymous.
 */
package donor.search;

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Type;

import donor.metric.*;
import donor.metric.Variable.USE_TYPE;
import donor.modify.Modification;
import donor.parser.node.expr.Expr;

/**
 * @author Jiajun
 * @date Jun 23, 2017
 */
public abstract class Node {
	protected int _startLine = 0; // start line of a node
	protected int _endLine = 0; // end line of a node
	protected ASTNode _originalNode = null; // ori node
	protected Node _parent = null; // parent
	protected NewFVector _fVector = null; // f_vector
	protected TYPE _nodeType = TYPE.UNKNOWN; // unknown type (initial value)

	// initialize
	protected Node(int startLine, int endLine, ASTNode node){
		this(startLine, endLine, node, null);
	}
	
	// initialize with parent node (containing sline, eline, and ASTNode).
	protected Node(int startLine, int endLine, ASTNode node, Node parent){
		_startLine = startLine;
		_endLine = endLine;
		_originalNode = node;
		_parent = parent;
	}
	
	public int getStartLine(){
		return _startLine;
	}
	
	public int getEndLine(){
		return _endLine;
	}
	
	public ASTNode getOriginalAST(){
		return _originalNode;
	}
	
	public Node getParent(){
		return _parent;
	}
	
	public TYPE getNodeType(){
		return _nodeType;
	}
	
	public void setParent(Node parent){
		_parent = parent;
	}
	
	// get feature vector
	public NewFVector getFeatureVector(){
		if(_fVector == null){
			// TODO: this method seems still not realized
			computeFeatureVector();
		}
		return _fVector;
	}
	
	// match method
	public abstract boolean match(Node node, Map<String, String> varTrans, Map<String, Type> allUsableVariables,
			List<Modification> modifications);
	
	public abstract String simplify(Map<String, String> varTrans, Map<String, Type> allUsableVariables);

	public abstract StringBuffer toSrcString();
	
	// a series of get methods
	public abstract USE_TYPE getUseType(Node child);
	public abstract List<Node> getChildren();
	public abstract List<Literal> getLiterals();
	public abstract List<Variable> getVariables();
	public abstract List<LoopStruct> getLoopStruct();
	public abstract List<CondStruct> getCondStruct();
	public abstract List<OtherStruct> getOtherStruct();
	public abstract List<MethodCall> getMethodCalls();
	public abstract List<Operator> getOperators();
	
	// compute vector
	public abstract void computeFeatureVector();
	
	// reduce method
	public abstract List<CodeBlock> reduce();
	
	// adapt, restore, and backup modifications
	public abstract boolean adapt(Modification modification);
	public abstract boolean restore(Modification modification);
	public abstract boolean backup(Modification modification);
	
	@Override
	public String toString() {
		return toSrcString().toString();
	}
	
	// type enumerations
	public static enum TYPE{
		ARRACC("ArrayAccess"),
		ARRCREAT("ArrayCreation"),
		ARRINIT("ArrayInitilaization"),
		ASSIGN("Assignment"),
		BLITERAL("BooleanLiteral"),
		CAST("CastExpression"),
		CLITERAL("CharacterLiteral"),
		CLASSCREATION("ClassInstanceCreation"),
		COMMENT("Annotation"),
		CONDEXPR("ConditionalExpression"),
		DLITERAL("DoubleLiteral"),
		FIELDACC("FieldAccess"),
		FLITERAL("FloatLiteral"),
		INFIXEXPR("InfixExpression"),
		INSTANCEOF("InstanceofExpression"),
		INTLITERAL("IntLiteral"),
		LABEL("Name"),
		LLITERAL("LongLiteral"),
		MINVOCATION("MethodInvocation"),
		NULL("NullLiteral"),
		NUMBER("NumberLiteral"),
		PARENTHESISZED("ParenthesizedExpression"),
		POSTEXPR("PostfixExpression"),
		PREEXPR("PrefixExpression"),
		QNAME("QualifiedName"),
		SNAME("SimpleName"),
		SLITERAL("StringLiteral"),
		SFIELDACC("SuperFieldAccess"),
		SMINVOCATION("SuperMethodInvocation"),
		SINGLEVARDECL("SingleVariableDeclation"),
		THIS("ThisExpression"),
		TLITERAL("TypeLiteral"),
		VARDECLEXPR("VariableDeclarationExpression"),
		VARDECLFRAG("VariableDeclarationFragment"),
		ANONYMOUSCDECL("AnonymousClassDeclaration"),
		ASSERT("AssertStatement"),
		BLOCK("Block"),
		BREACK("BreakStatement"),
		CONSTRUCTORINV("ConstructorInvocation"),
		CONTINUE("ContinueStatement"),
		DO("DoStatement"),
		EFOR("EnhancedForStatement"),
		FOR("ForStatement"),
		IF("IfStatement"),
		RETURN("ReturnStatement"),
		SCONSTRUCTORINV("SuperConstructorInvocation"),
		SWCASE("SwitchCase"),
		SWSTMT("SwitchStatement"),
		SYNC("SynchronizedStatement"),
		THROW("ThrowStatement"),
		TRY("TryStatement"),
		TYPEDECL("TypeDeclarationStatement"),
		VARDECLSTMT("VariableDeclarationStatement"),
		WHILE("WhileStatement"),
		UNKNOWN("Unknown");
		
		private String _name = null;
		private TYPE(String name){
			_name = name;
		}
		
		@Override
		public String toString() {
			return _name;
		}
	}
	
}

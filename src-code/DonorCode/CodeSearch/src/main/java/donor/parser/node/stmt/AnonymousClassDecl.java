/**
 * Copyright (C) anonymous. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by anonymous.
 */
package donor.parser.node.stmt;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Type;

import donor.metric.CondStruct;
import donor.metric.Literal;
import donor.metric.LoopStruct;
import donor.metric.MethodCall;
import donor.metric.NewFVector;
import donor.metric.Operator;
import donor.metric.OtherStruct;
import donor.metric.Variable;
import donor.metric.Variable.USE_TYPE;
import donor.modify.Modification;
import donor.search.CodeBlock;
import donor.search.Node;

/**
 * @author Jiajun
 * @date Jun 23, 2017
 */
public class AnonymousClassDecl extends Node {

	
	public AnonymousClassDecl(int startLine, int endLine, ASTNode node) {
		super(startLine, endLine, node);
		_nodeType = TYPE.ANONYMOUSCDECL;
	}

	@Override
	public boolean adapt(Modification modification) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean restore(Modification modification) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean backup(Modification modification) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean match(Node node, Map<String, String> varTrans, Map<String, Type> allUsableVariables, List<Modification> modifications) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public StringBuffer toSrcString() {
		return new StringBuffer(_originalNode.toString());
	}

	@Override
	public List<Literal> getLiterals() {
		return new LinkedList<>();
	}

	@Override
	public List<Variable> getVariables() {
		return new LinkedList<>();
	}

	@Override
	public List<LoopStruct> getLoopStruct() {
		return new LinkedList<>();
	}
	
	@Override
	public List<CondStruct> getCondStruct() {
		return new LinkedList<>();
	}

	@Override
	public List<MethodCall> getMethodCalls() {
		return new LinkedList<>();
	}

	@Override
	public List<Operator> getOperators() {
		return new LinkedList<>();
	}
	
	@Override
	public List<OtherStruct> getOtherStruct() {
		return new LinkedList<>();
	}

	@Override
	public void computeFeatureVector() {
		_fVector = new NewFVector();
	}
	
	@Override
	public USE_TYPE getUseType(Node child) {
		return _parent.getUseType(this);
	}
	
	@Override
	public List<Node> getChildren() {
		return new ArrayList<>();
	}

	@Override
	public String simplify(Map<String, String> varTrans, Map<String, Type> allUsableVariables) {
		return toSrcString().toString();
	}
	
	@Override
	public List<CodeBlock> reduce() {
		return new LinkedList<CodeBlock>();
	}
}

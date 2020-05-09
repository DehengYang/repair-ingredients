/**
 * Copyright (C) anonymous. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by anonymous.
 */
package donor.parser.node.stmt;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;

import donor.metric.CondStruct;
import donor.metric.Literal;
import donor.metric.LoopStruct;
import donor.metric.MethodCall;
import donor.metric.Operator;
import donor.metric.OtherStruct;
import donor.metric.Variable;
import donor.metric.Variable.USE_TYPE;
import donor.search.CodeBlock;
import donor.search.Node;

/**
 * @author Jiajun
 * @date Jun 23, 2017
 */
public abstract class Stmt extends Node{
	
	protected Stmt(int startLine, int endLine, ASTNode node, Node parent) {
		super(startLine, endLine, node, parent);
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
	public USE_TYPE getUseType(Node child) {
		if(_parent == null){
			return USE_TYPE.USE_UNKNOWN;
		} else {
			return _parent.getUseType(this);
		}
	}
	
	@Override
	public List<CodeBlock> reduce() {
		List<CodeBlock> linkedList = new LinkedList<>();
		for(Node node : getChildren()){
			linkedList.addAll(node.reduce());
		}
		return linkedList;
	}
}

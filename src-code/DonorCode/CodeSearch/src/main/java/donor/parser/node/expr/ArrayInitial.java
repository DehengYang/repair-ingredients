/**
 * Copyright (C) anonymous. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by anonymous.
 */
package donor.parser.node.expr;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Condition;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Type;

import donor.metric.Literal;
import donor.metric.MethodCall;
import donor.metric.NewFVector;
import donor.metric.Operator;
import donor.metric.Variable;
import donor.modify.Modification;
import donor.parser.NodeUtils;
import donor.search.Node;

/**
 * @author Jiajun
 * @date Jun 23, 2017
 */
public class ArrayInitial extends Expr {

	private List<Expr> _expressions = null;
	
	/**
	 * ArrayInitializer:
     *           { [ Expression { , Expression} [ , ]] }
	 */
	public ArrayInitial(int startLine, int endLine, ASTNode node) {
		super(startLine, endLine, node);
		_nodeType = TYPE.ARRINIT;
	}
	
	public void setExpressions(List<Expr> expressions){
		_expressions = expressions;
	}

	@Override
	public boolean match(Node node, Map<String, String> varTrans, Map<String, Type> allUsableVariables, List<Modification> modifications) {
		boolean match = false;
		if(node instanceof ArrayInitial){
			match = true;
			// TODO : to finish
		} else {
			List<Node> children = node.getChildren();
			List<Modification> tmp = new ArrayList<>();
			if(NodeUtils.nodeMatchList(this, children, varTrans, allUsableVariables, tmp)){
				match = true;
				modifications.addAll(tmp);
			}
		}
		return match;
	}

	@Override
	public boolean adapt(Modification modification) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean restore(Modification modification) {
		return true;
	}

	@Override
	public boolean backup(Modification modification) {
		return true;
	}
	
	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer("{");
		if(_expressions.size() > 0){
			stringBuffer.append(_expressions.get(0).toSrcString());
			for(int i = 1; i < _expressions.size(); i++){
				stringBuffer.append(",");
				stringBuffer.append(_expressions.get(i).toSrcString());
			}
		}
		stringBuffer.append("}");
		return stringBuffer;
	}

	@Override
	public List<Literal> getLiterals() {
		List<Literal> list = new LinkedList<>();
		if(_expressions != null){
			for(Expr expr : _expressions){
				list.addAll(expr.getLiterals());
			}
		}
		return list;
	}

	@Override
	public List<Variable> getVariables() {
		List<Variable> list = new LinkedList<>();
		if(_expressions != null){
			for(Expr expr : _expressions){
				list.addAll(expr.getVariables());
			}
		}
		return list;
	}
	
	@Override
	public List<MethodCall> getMethodCalls() {
		List<MethodCall> list = new LinkedList<>();
		if(_expressions != null){
			for(Expr expr : _expressions){
				list.addAll(expr.getMethodCalls());
			}
		}
		return list;
	}
	
	@Override
	public List<Operator> getOperators() {
		List<Operator> list = new LinkedList<>();
		if(_expressions != null){
			for(Expr expr : _expressions){
				list.addAll(expr.getOperators());
			}
		}
		return list;
	}

	@Override
	public void computeFeatureVector() {
		_fVector = new NewFVector();
		if(_expressions != null){
			for(Expr expr : _expressions){
				_fVector.combineFeature(expr.getFeatureVector());
			}
		}
	}

	@Override
	public List<Node> getChildren() {
		return new ArrayList<>();
	}

	@Override
	public String simplify(Map<String, String> varTrans, Map<String, Type> allUsableVariables) {
		// TODO Auto-generated method stub
		return null;
	}
}

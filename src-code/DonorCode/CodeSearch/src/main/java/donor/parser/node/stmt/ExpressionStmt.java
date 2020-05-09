/**
 * Copyright (C) anonymous. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by anonymous.
 */
package donor.parser.node.stmt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Type;

import donor.metric.CondStruct;
import donor.metric.Literal;
import donor.metric.MethodCall;
import donor.metric.NewFVector;
import donor.metric.Operator;
import donor.metric.Variable;
import donor.modify.Modification;
import donor.parser.node.expr.Expr;
import donor.search.Node;

/**
 * @author Jiajun
 * @date Jun 23, 2017
 */
public class ExpressionStmt extends Stmt {

	private Expr _expression = null;
	
	private Expr _expression_replace = null;
	
	/**
	 * ExpressionStatement:
     *	StatementExpression ;
	 */
	public ExpressionStmt(int startLine, int endLine, ASTNode node) {
		this(startLine, endLine, node, null);
	}
	
	public ExpressionStmt(int startLine, int endLine, ASTNode node, Node parent) {
		super(startLine, endLine, node, parent);
	}

	public void setExpression(Expr expression){
		_expression = expression;
	}
	
	@Override
	public boolean match(Node node, Map<String, String> varTrans, Map<String, Type> allUsableVariables, List<Modification> modifications) {
		return _expression.match(node, varTrans, allUsableVariables, modifications);
	}

	@Override
	public boolean adapt(Modification modification) {
		return false;
	}

	@Override
	public boolean restore(Modification modification) {
		return false;
	}

	@Override
	public boolean backup(Modification modification) {
		return false;
	}
	
	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		if(_expression_replace != null){
			stringBuffer.append(_expression_replace.toSrcString());
		} else {
			stringBuffer.append(_expression.toSrcString());
		}
		stringBuffer.append(";");
		return stringBuffer;
	}

	@Override
	public List<Literal> getLiterals() {
		return _expression.getLiterals();
	}

	@Override
	public List<Variable> getVariables() {
		return _expression.getVariables();
	}
	
	@Override
	public List<CondStruct> getCondStruct() {
		return _expression.getCondStruct();
	}

	@Override
	public List<MethodCall> getMethodCalls() {
		return _expression.getMethodCalls();
	}

	@Override
	public List<Operator> getOperators() {
		return _expression.getOperators();
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new NewFVector();
		_fVector.combineFeature(_expression.getFeatureVector());
	}
	
	@Override
	public List<Node> getChildren() {
		List<Node> list = new ArrayList<>();
		list.add(_expression);
		return list;
	}
	
	@Override
	public String simplify(Map<String, String> varTrans, Map<String, Type> allUsableVariables) {
		String expression = _expression.simplify(varTrans, allUsableVariables);
		if(expression == null){
			return null;
		}
		return expression + ";";
	}
}

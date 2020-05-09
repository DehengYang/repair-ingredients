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

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Type;

import donor.metric.Literal;
import donor.metric.MethodCall;
import donor.metric.NewFVector;
import donor.metric.Operator;
import donor.metric.Variable;
import donor.metric.Variable.USE_TYPE;
import donor.modify.Modification;
import donor.parser.NodeUtils;
import donor.search.Node;
import donor.search.Pair;

/**
 * @author Jiajun
 * @date Jun 23, 2017
 */
public class InstanceofExpr extends Expr {

	private Expr _expression = null;
	private String _operator = "instanceof";
	private Type _instanceType = null;
	
	/**
	 * InstanceofExpression:
     *	Expression instanceof Type
	 */
	public InstanceofExpr(int startLine, int endLine, ASTNode node) {
		super(startLine, endLine, node);
		_nodeType = TYPE.INSTANCEOF;
	}
	
	public void setExpression(Expr expression){
		_expression = expression;
	}
	
	public void setInstanceType(Type instanceType){
		_instanceType = instanceType;
	}

	@Override
	public boolean match(Node node, Map<String, String> varTrans, Map<String, Type> allUsableVariables, List<Modification> modifications) {
		boolean match = false;
		if(node instanceof InstanceofExpr){
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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean backup(Modification modification) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_expression.toSrcString());
		stringBuffer.append(" instanceof ");
		stringBuffer.append(_instanceType.toString());
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
	public List<MethodCall> getMethodCalls() {
		return _expression.getMethodCalls();
	}

	@Override
	public List<Operator> getOperators() {
		List<Operator> list = new LinkedList<>();
		Operator operator = new Operator(this, Operator.KIND.INS);
		list.add(operator);
		list.addAll(_expression.getOperators());
		return list;
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new NewFVector();
		_fVector.inc(_operator);
		_fVector.combineFeature(_expression.getFeatureVector());
	}
	
	@Override
	public USE_TYPE getUseType(Node child) {
		return USE_TYPE.USE_INFIX_EXP;
	}
	
	@Override
	public List<Node> getChildren() {
		return new ArrayList<>();
	}

	@Override
	public String simplify(Map<String, String> varTrans, Map<String, Type> allUsableVariables) {
		Map<SName, Pair<String, String>> record = NodeUtils.tryReplaceAllVariables(this, varTrans, allUsableVariables);
		if(record == null){
			return null;
		}
		NodeUtils.replaceVariable(record);
		String string = toSrcString().toString();
		NodeUtils.restoreVariables(record);
		return string;
	}
	
}

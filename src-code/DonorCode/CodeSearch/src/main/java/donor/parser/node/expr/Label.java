/**
 * Copyright (C) anonymous. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by anonymous.
 */
package donor.parser.node.expr;

import org.eclipse.jdt.core.dom.ASTNode;

/**
 * @author Jiajun
 * @date Jun 23, 2017
 */
public abstract class Label extends Expr {

	/**
	 * Name:
     *	SimpleName
     *	QualifiedName
	 */
	public Label(int startLine, int endLine, ASTNode node) {
		super(startLine, endLine, node);
	}

}

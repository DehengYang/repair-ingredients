/**
 * Copyright (C) anonymous. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by anonymous.
 */
package donor.metric;

import donor.search.Node;

/**
 * @author Jiajun
 * @date Jun 23, 2017
 */
public class Operator extends Feature {
	
	public static enum KIND{
		INFIX,
		POSTFIX,
		PREFIX,
		ACC,
		INS
	}
	
	private KIND _kind = null;
	
	public Operator(Node node, KIND kind) {
		super(node);
		_kind = kind;
	}
}

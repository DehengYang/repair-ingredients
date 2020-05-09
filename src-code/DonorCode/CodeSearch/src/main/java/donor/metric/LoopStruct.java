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
public class LoopStruct extends Feature {

	public static enum KIND {
		FOR,
		EFOR,
		WHILE,
		DO
	}
	
	private KIND _kind = null;
	
	public LoopStruct(Node node, KIND kind) {
		super(node);
		_kind = kind;
	}
	
}

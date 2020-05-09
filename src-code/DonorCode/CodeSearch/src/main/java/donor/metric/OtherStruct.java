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
 * @date Jun 28, 2017
 */
public class OtherStruct extends Feature {

	public static enum KIND {
		BREAK,
		CONTINUE,
		RETURN,
		THROW
	}
	
	private KIND _kind = null;
	
	public OtherStruct(Node node, KIND kind) {
		super(node);
		_kind = kind;
	}

	public KIND getKind(){
		return _kind;
	}
	
}

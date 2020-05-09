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
public abstract class Feature {
	
	protected Node _node = null;
	
	protected Feature(Node node) {
		_node = node;
	}
	
	public Node getNode(){
		return _node;
	}
}

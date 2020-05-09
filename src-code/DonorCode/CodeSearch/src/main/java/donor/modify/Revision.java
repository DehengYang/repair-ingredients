/**
 * Copyright (C) anonymous. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by anonymous.
 */
package donor.modify;

import donor.search.Node;
import donor.search.Node.TYPE;

/**
 * @author Jiajun
 * @date Jun 30, 2017
 */
public class Revision extends Modification {

	public Revision(Node node, int srcID, String target, TYPE changeNodeType) {
		super(node, srcID, target, changeNodeType, 0);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String toString() {
		return "[REV | " + _nodeType + " | " + _sourceID + "]" + _node.toString().replace("\n", " ") + "=>" + _target.replace("\n", " ");
	}
	
}

/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.blacklisting;

import java.util.Objects;
import java.util.function.Predicate;

import org.cryptomator.filesystem.Node;

public class SamePathPredicate implements Predicate<Node> {

	private final Node node;

	private SamePathPredicate(Node node) {
		Objects.requireNonNull(node);
		this.node = node;
	}

	@Override
	public boolean test(Node other) {
		return node.parent().equals(other.parent()) && node.name().equals(other.name());
	}

	public static SamePathPredicate forNode(Node node) {
		return new SamePathPredicate(node);
	}

}

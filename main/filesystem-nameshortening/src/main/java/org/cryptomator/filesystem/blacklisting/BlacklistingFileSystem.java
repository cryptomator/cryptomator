/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.blacklisting;

import java.util.function.Predicate;

import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.Node;
import org.cryptomator.filesystem.delegating.DelegatingFileSystem;

class BlacklistingFileSystem extends BlacklistingFolder implements DelegatingFileSystem {

	public BlacklistingFileSystem(Folder root, Predicate<Node> hiddenNodes) {
		super(null, root, hiddenNodes);
	}

	@Override
	public Folder getDelegate() {
		return delegate;
	}

}

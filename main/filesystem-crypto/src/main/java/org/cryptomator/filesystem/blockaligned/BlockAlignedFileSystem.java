/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.blockaligned;

import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.delegating.DelegatingFileSystem;

class BlockAlignedFileSystem extends BlockAlignedFolder implements DelegatingFileSystem {

	public BlockAlignedFileSystem(Folder delegate, int blockSize) {
		super(null, delegate, blockSize);
	}

	@Override
	public Folder getDelegate() {
		return delegate;
	}

}

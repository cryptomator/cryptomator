/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.blacklisting;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.delegating.DelegatingFile;

class BlacklistingFile extends DelegatingFile<BlacklistingFolder> {

	public BlacklistingFile(BlacklistingFolder parent, File delegate) {
		super(parent, delegate);
	}

}

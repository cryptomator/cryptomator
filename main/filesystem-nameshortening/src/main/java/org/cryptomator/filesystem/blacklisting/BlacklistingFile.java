package org.cryptomator.filesystem.blacklisting;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.delegating.DelegatingFile;

class BlacklistingFile extends DelegatingFile<BlacklistingFolder> {

	public BlacklistingFile(BlacklistingFolder parent, File delegate) {
		super(parent, delegate);
	}

}

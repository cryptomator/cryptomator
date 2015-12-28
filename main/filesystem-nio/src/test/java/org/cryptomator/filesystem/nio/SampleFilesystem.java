package org.cryptomator.filesystem.nio;

import org.apache.commons.lang3.StringUtils;

public enum SampleFilesystem {

	EMPTY_FILESYSTEM, SOME_FOLDERS_AND_FILES

	;

	public String directoryName() {
		return StringUtils.removeEnd(name(), "_FILESYSTEM").toLowerCase();
	}

}

package org.cryptomator.ui.util;

import java.io.File;

/**
 * Class for processing a file path.
 * If the filepath is a UNC file path (syntax: \\server[@SSL][@port][\path]), the "@SSL" and "@port" part is ripped off.
 */
public class ProcessFilePath {

	public static String processFilePath(String path) {
		int uncIndex = path.indexOf('@');
		int resourceIndex = path.indexOf('\\', 2);
		if (System.getProperty("os.name").contains("Windows") && path.startsWith("\\\\") && uncIndex >= 0 && (resourceIndex == -1 || uncIndex < resourceIndex)) {
			//the returned file has a UNC-Path, which needs further processing
			if (resourceIndex >= 0) {
				//the optional path part exists, therefore we cut everything else out
				return path.substring(0, uncIndex).concat(path.substring(resourceIndex));
			} else {
				return path.substring(0, uncIndex);
			}
		} else {
			return path;
		}
	}

}

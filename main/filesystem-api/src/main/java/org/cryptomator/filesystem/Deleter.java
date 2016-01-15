package org.cryptomator.filesystem;

public class Deleter {

	/**
	 * Deletes all and only the content of a given {@link Folder} but <b>not</b> the folder itself.
	 */
	public static void deleteContent(Folder folder) {
		if (folder.exists()) {
			folder.folders().forEach(Folder::delete);
			folder.files().forEach(File::delete);
		}
	}

}

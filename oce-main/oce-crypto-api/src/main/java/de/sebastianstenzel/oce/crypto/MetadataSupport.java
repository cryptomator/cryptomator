/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package de.sebastianstenzel.oce.crypto;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.NoSuchFileException;

@Deprecated
public interface MetadataSupport {

	/**
	 * Opens the file with the given name for writing. Overwrites existing files.
	 * 
	 * @return Outputstream ready to write to. Must be closed by caller.
	 * @throws IOException
	 */
	OutputStream openMetadataForWrite(String filename, Level location) throws IOException;

	/**
	 * Opens the file with the given name without locking it.
	 * 
	 * @return InputStream ready to read from. Must be closed by caller.
	 * @throws NoSuchFileException
	 * @throws IOException
	 */
	InputStream openMetadataForRead(String filename, Level location) throws NoSuchFileException, IOException;

	enum Level {
		/**
		 * Root folder of the encrypted store.
		 */
		ROOT_FOLDER,

		/**
		 * Parent folder of the current object.
		 */
		PARENT_FOLDER,

		/**
		 * If the current object is a folder, its own location. If the current object is a file, behaves the same as {@link #PARENT_FOLDER}.
		 */
		CURRENT_FOLDER;
	}

}

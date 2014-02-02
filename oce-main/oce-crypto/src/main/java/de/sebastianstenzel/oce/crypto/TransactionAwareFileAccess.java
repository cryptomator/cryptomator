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
import java.nio.file.Path;

/**
 * IoC for I/O streams. The streams provied by these methods are closed by the caller. Thus the callee implementing this interface must not
 * close the streams again.
 */
public interface TransactionAwareFileAccess {
	
	/**
	 * @return Path relative to the current working directory, regardless of leading slashes.
	 */
	Path resolveUri(String uri);

	InputStream openFileForRead(Path path) throws IOException;

	OutputStream openFileForWrite(Path path) throws IOException;

}

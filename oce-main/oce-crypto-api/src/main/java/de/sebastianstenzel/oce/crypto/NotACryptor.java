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
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;

import de.sebastianstenzel.oce.crypto.io.SeekableByteChannelInputStream;
import de.sebastianstenzel.oce.crypto.io.SeekableByteChannelOutputStream;

/**
 * @deprecated Just for test purposes.
 */
@Deprecated
public class NotACryptor implements Cryptor {

	@Override
	public String encryptPath(String cleartextPath, char encryptedPathSep, char cleartextPathSep, MetadataSupport metadataSupport) {
		return cleartextPath;
	}

	@Override
	public String decryptPath(String encryptedPath, char encryptedPathSep, char cleartextPathSep, MetadataSupport metadataSupport) {
		return encryptedPath;
	}

	@Override
	public Long decryptedContentLength(SeekableByteChannel encryptedFile, MetadataSupport metadataSupport) throws IOException {
		return encryptedFile.size();
	}

	@Override
	public Long decryptedFile(SeekableByteChannel encryptedFile, OutputStream plaintextFile) throws IOException {
		final InputStream in = new SeekableByteChannelInputStream(encryptedFile);
		return IOUtils.copyLarge(in, plaintextFile);
	}

	@Override
	public Long encryptFile(InputStream plaintextFile, SeekableByteChannel encryptedFile) throws IOException {
		final OutputStream out = new SeekableByteChannelOutputStream(encryptedFile);
		return IOUtils.copyLarge(plaintextFile, out);
	}

	@Override
	public Filter<Path> getPayloadFilesFilter() {
		return new Filter<Path>() {

			@Override
			public boolean accept(Path entry) throws IOException {
				/* all files are "encrypted" */
				return true;
			}
		};
	}

	@Override
	public void swipeSensitiveData() {
		// do nothing.
	}

}

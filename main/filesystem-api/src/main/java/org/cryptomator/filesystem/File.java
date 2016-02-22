/*******************************************************************************
 * Copyright (c) 2015 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 ******************************************************************************/
package org.cryptomator.filesystem;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * A {@link File} in a {@link FileSystem}.
 * 
 * @author Markus Kreusch
 */
public interface File extends Node, Comparable<File> {

	static final int EOF = -1;

	/**
	 * <p>
	 * Opens this file for reading.
	 * <p>
	 * An implementation guarantees, that per {@link FileSystem} and
	 * {@code File} while a {@code ReadableFile} is open no {@link WritableFile}
	 * can be open and vice versa. A {@link ReadableFile} is open when returned
	 * from this method and not yet closed using {@link ReadableFile#close()}.
	 * <br>
	 * A limitation to the number of {@code ReadableFiles} is in general not
	 * required but may be set by a specific implementation.
	 * <p>
	 * If a {@link WritableFile} for this {@code File} is open the invocation of
	 * this method will block regarding the specified timeout.<br>
	 * In addition implementations may block to lock the required IO resources
	 * to read the file.
	 * 
	 * @return a {@link ReadableFile} to work with
	 * @throws UncheckedIOException
	 *             if an {@link IOException} occurs while opening the file, the
	 *             file does not exist or is a directory
	 */

	ReadableFile openReadable() throws UncheckedIOException;

	/**
	 * <p>
	 * Opens this file for writing.
	 * <p>
	 * If the file does not exist a new empty file is created.
	 * <p>
	 * An implementation guarantees, that per {@link FileSystem} and
	 * {@code File} only one {@link WritableFile} is open at a time. A
	 * {@code WritableFile} is open when returned from this method and not yet
	 * closed using {@link WritableFile#close()} or
	 * {@link WritableFile#delete()}.<br>
	 * In addition while a {@code WritableFile} is open no {@link ReadableFile}
	 * can be open and vice versa.
	 * <p>
	 * If a {@code Readable-} or {@code WritableFile} for this {@code File} is
	 * open the invocation of this method will block regarding the specified
	 * timeout.<br>
	 * In addition implementations may block to lock the required IO resources
	 * to read the file.
	 * 
	 * @return a {@link WritableFile} to work with
	 * @throws UncheckedIOException
	 *             if an {@link IOException} occurs while opening the file or
	 *             the file is a directory
	 */
	WritableFile openWritable() throws UncheckedIOException;

	default void copyTo(File destination) {
		Copier.copy(this, destination);
	}

	/**
	 * Moves this file including content to a new location specified by <code>destination</code>.
	 */
	void moveTo(File destination) throws UncheckedIOException;

}

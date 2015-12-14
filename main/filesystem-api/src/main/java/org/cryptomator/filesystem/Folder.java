/*******************************************************************************
 * Copyright (c) 2015 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 ******************************************************************************/
package org.cryptomator.filesystem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.Stream;

/**
 * A {@link Folder} in a {@link FileSystem}.
 * 
 * @author Markus Kreusch
 */
public interface Folder extends Node {

	/**
	 * <p>
	 * Creates a {@link Stream} over all child nodes of this {@code Folder}.
	 * <p>
	 * <b>Note:</b> The {@link Stream} may be lazily populated and thus
	 * {@link IOException IOExceptions} may occurs after this method returned.
	 * In this case implementors should throw a {@link UncheckedIOException}
	 * from any method that produces an {@link IOException}. Thus users should
	 * expect {@link UncheckedIOException UncheckedIOExceptions} when invoking
	 * methods on the returned {@code Stream}.
	 * 
	 * @return the created {@code Stream}
	 * @throws UncheckedIOException
	 *             if an {@link IOException} occurs while initializing the
	 *             stream
	 */
	Stream<? extends Node> children() throws UncheckedIOException;

	/**
	 * <p>
	 * Returns the child {@link Node} in this directory of type {@link File}
	 * with the specified name.
	 * <p>
	 * This operation always returns a {@link File} without checking if the file
	 * exists or is a {@link Folder} instead.
	 */
	File file(String name) throws UncheckedIOException;

	/**
	 * <p>
	 * Returns the child {@link Node} in this directory of type {@link Folder}
	 * with the specified name.
	 * <p>
	 * This operation always returns a {@link Folder} without checking if the
	 * folder exists or is a {@link File} instead.
	 */
	Folder folder(String name) throws UncheckedIOException;

	/**
	 * Copies this directory and its contents to the given destination. If the
	 * target exists it is deleted before performing the copy.
	 */
	void copyTo(Folder target);

	/**
	 * Moves this directory and its contents to the given destination. If the
	 * target exists it is deleted before performing the move.
	 */
	void moveTo(Folder target);

	/**
	 * @return the result of {@link #children()} filtered to contain only
	 *         {@link File Files}
	 */
	default Stream<? extends File> files() throws UncheckedIOException {
		return children() //
				.filter(File.class::isInstance) //
				.map(File.class::cast);
	}

	/**
	 * @return the result of {@link #children()} filtered to contain only
	 *         {@link Folder Folders}
	 */
	default Stream<? extends Folder> folders() throws UncheckedIOException {
		return children() //
				.filter(Folder.class::isInstance) //
				.map(Folder.class::cast);
	}

}

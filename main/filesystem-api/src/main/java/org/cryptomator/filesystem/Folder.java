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
	 * @throws IOException
	 *             if an {@link IOException} occurs while initializing the
	 *             stream
	 */
	Stream<Node> children() throws IOException;

	File file(String name) throws IOException;

	Folder folder(String name) throws IOException;

	void create(FolderCreateMode mode) throws IOException;

	void delete() throws IOException;

	/**
	 * @return the result of {@link #children()} filtered to contain only
	 *         {@link File Files}
	 */
	default Stream<File> files() throws IOException {
		return children() //
				.filter(File.class::isInstance) //
				.map(File.class::cast);
	}

	/**
	 * @return the result of {@link #children()} filtered to contain only
	 *         {@link Folder Folders}
	 */
	default Stream<Folder> folders() throws IOException {
		return children() //
				.filter(Folder.class::isInstance) //
				.map(Folder.class::cast);
	}

}

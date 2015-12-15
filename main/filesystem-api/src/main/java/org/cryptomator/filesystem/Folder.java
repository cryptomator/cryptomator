/*******************************************************************************
 * Copyright (c) 2015 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 ******************************************************************************/
package org.cryptomator.filesystem;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
	 * Creates the directory, if it doesn't exist yet. After successful invocation {@link #exists()} will return <code>true</code>
	 * 
	 * @param mode Depending on this option either the attempt is made to recursively create all parent directories or an exception is thrown if the parent doesn't exist yet.
	 * @throws UncheckedIOException wrapping an {@link FileNotFoundException}, if mode is {@link FolderCreateMode#FAIL_IF_PARENT_IS_MISSING FAIL_IF_PARENT_IS_MISSING} and parent doesn't exist.
	 */
	void create(FolderCreateMode mode) throws UncheckedIOException;

	/**
	 * Copies this directory and its contents to the given destination.
	 */
	default void copyTo(Folder target) throws UncheckedIOException {
		final Folder copy = target.folder(this.name());
		copy.create(FolderCreateMode.INCLUDING_PARENTS);
		folders().forEach(folder -> folder.copyTo(copy));
		files().forEach(srcFile -> {
			final File dstFile = copy.file(srcFile.name());
			try (ReadableFile src = srcFile.openReadable(1, TimeUnit.SECONDS); WritableFile dst = dstFile.openWritable(1, TimeUnit.SECONDS)) {
				src.copyTo(dst);
			} catch (TimeoutException e) {
				throw new UncheckedIOException(new IOException("Failed to lock file in time.", e));
			}
		});
	}

	/**
	 * Deletes the directory including all child elements. Afterwards {@link #exists()} will return <code>false</code>.
	 */
	void delete() throws UncheckedIOException;

	/**
	 * Moves this directory and its contents to the given destination. If the target exists it is deleted before performing the move.
	 * Afterwards {@link #exists()} will return <code>false</code> for this folder and any child nodes.
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

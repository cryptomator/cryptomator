/*******************************************************************************
 * Copyright (c) 2015 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 ******************************************************************************/
package org.cryptomator.filesystem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Optional;

/**
 * Represents a node, namely a {@link File} or {@link Folder}, in a
 * {@link FileSystem}.
 * <p>
 * A node's identity (i.e. {@link #hashCode()} and {@link #equals(Object)}) depends on its parent node and its name (forming the node's path).
 * These properties are meant to be immutable. This means that e.g. moving a node doesn't modify the node's identity but rather transfers properties to the destination node.
 * 
 * @author Markus Kreusch
 * @see Folder
 * @see File
 */
public interface Node {

	String name() throws UncheckedIOException;

	/**
	 * @return Optional parent folder. No parent is present for the root node (see {@link FileSystem#parent()}).
	 */
	Optional<? extends Folder> parent() throws UncheckedIOException;

	/**
	 * @return <code>true</code> if the node exists.
	 */
	boolean exists() throws UncheckedIOException;

	/**
	 * <p>
	 * Deletes the node if it exists.
	 * <p>
	 * Does nothing if the node does not exist.
	 */
	void delete() throws UncheckedIOException;

	/**
	 * <p>
	 * Determines the last modified date of this node.
	 * 
	 * @returns the last modified date of the file
	 */
	Instant lastModified() throws UncheckedIOException;

	/**
	 * <p>
	 * Sets the last modified date of the file.
	 * 
	 * @param lastModified the time to set as creation time
	 */
	void setLastModified(Instant lastModified) throws UncheckedIOException;

	/**
	 * <p>
	 * Determines the creation time of this node.
	 * <p>
	 * Note: Getting the creation time may not be supported by all {@link FileSystem FileSystems}.
	 * 
	 * @returns the creation time of the file or {@link Optional#empty()} if not supported
	 */
	default Optional<Instant> creationTime() throws UncheckedIOException {
		return Optional.empty();
	}

	/**
	 * <p>
	 * Sets the creation time of this node.
	 * <p>
	 * Setting the creation time may not be supported by all {@link FileSystem FileSystems}. If the {@code FileSystem} this {@code Node} belongs to does not support the
	 * setting the creation time the behavior of this method is unspecified.
	 * 
	 * @param creationTime the time to set as creation time
	 */
	default void setCreationTime(Instant creationTime) throws UncheckedIOException {
		throw new UncheckedIOException(new IOException("CreationTime not supported"));
	}

	/**
	 * @return the {@link FileSystem} this Node belongs to
	 */
	default FileSystem fileSystem() {
		return parent() //
				.map(Node::fileSystem) //
				.orElseGet(() -> (FileSystem) this);
	}

	default boolean belongsToSameFilesystem(Node other) {
		return fileSystem() == other.fileSystem();
	}

}

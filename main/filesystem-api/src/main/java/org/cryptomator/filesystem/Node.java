/*******************************************************************************
 * Copyright (c) 2015 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 ******************************************************************************/
package org.cryptomator.filesystem;

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

	boolean exists() throws UncheckedIOException;

	Instant lastModified() throws UncheckedIOException;

}

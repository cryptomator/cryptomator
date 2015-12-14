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
 * 
 * @author Markus Kreusch
 * @see Folder
 * @see File
 */
public interface Node {

	String name() throws UncheckedIOException;

	Optional<? extends Folder> parent() throws UncheckedIOException;

	boolean exists() throws UncheckedIOException;

	Instant lastModified() throws UncheckedIOException;

}

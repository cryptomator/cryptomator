/*******************************************************************************
 * Copyright (c) 2015 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 ******************************************************************************/
package org.cryptomator.filesystem;

import java.io.UncheckedIOException;
import java.time.Instant;

public interface WritableFile extends File, WritableBytes, AutoCloseable {

	void moveTo(WritableFile other) throws UncheckedIOException;

	void setLastModified(Instant instant) throws UncheckedIOException;

	void delete() throws UncheckedIOException;

	void truncate() throws UncheckedIOException;

	@Override
	void close() throws UncheckedIOException;

}

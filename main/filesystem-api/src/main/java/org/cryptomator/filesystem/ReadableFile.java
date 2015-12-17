/*******************************************************************************
 * Copyright (c) 2015 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 ******************************************************************************/
package org.cryptomator.filesystem;

import java.io.UncheckedIOException;

public interface ReadableFile extends ReadableBytes, AutoCloseable {

	void copyTo(WritableFile other) throws UncheckedIOException;

	@Override
	void close() throws UncheckedIOException;

}

/*******************************************************************************
 * Copyright (c) 2015 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 ******************************************************************************/
package org.cryptomator.filesystem;

import java.io.IOException;
import java.time.Instant;

public interface WritableFile extends File, WritableBytes, AutoCloseable {

	WritableFile moveTo(WritableFile other) throws IOException;

	void setLastModified(Instant instant) throws IOException;

	void delete() throws IOException;

	void truncate() throws IOException;

	@Override
	void close() throws IOException;

}

/*******************************************************************************
 * Copyright (c) 2015 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 ******************************************************************************/
package org.cryptomator.filesystem;

import java.io.IOException;

public interface ReadableFile extends File, ReadableBytes, AutoCloseable {

	WritableFile copyTo(WritableFile other) throws IOException;

	@Override
	void close() throws IOException;

}

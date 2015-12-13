/*******************************************************************************
 * Copyright (c) 2015 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 ******************************************************************************/
package org.cryptomator.filesystem;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface WritableBytes {

	void write(ByteBuffer source) throws IOException;

	void write(ByteBuffer source, int position) throws IOException;

}

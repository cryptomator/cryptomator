/*******************************************************************************
 * Copyright (c) 2015 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 ******************************************************************************/
package org.cryptomator.filesystem;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface ReadableBytes {

	void read(ByteBuffer target) throws IOException;

	void read(ByteBuffer target, int position) throws IOException;

}

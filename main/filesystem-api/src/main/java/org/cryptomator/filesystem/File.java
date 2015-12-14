/*******************************************************************************
 * Copyright (c) 2015 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 ******************************************************************************/
package org.cryptomator.filesystem;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface File extends Node {

	ReadableFile openReadable(long timeout, TimeUnit unit) throws IOException, TimeoutException;

	WritableFile openWritable(long timeout, TimeUnit unit) throws IOException, TimeoutException;

}

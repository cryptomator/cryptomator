/*******************************************************************************
 * Copyright (c) 2014 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Markus Kreusch
 ******************************************************************************/
package org.cryptomator.ui.util.command;

import static org.apache.commons.io.IOUtils.copy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

final class AsyncStreamCopier extends Thread {

	private final InputStream input;
	private final OutputStream output;
	
	private IOException exception;
	
	public AsyncStreamCopier(InputStream input, OutputStream output) {
		this.input = input;
		this.output = output;
		start();
	}
	
	@Override
	public void run() {
		try (InputStream inputToBeClosed = input;
				OutputStream outputToBeClosed = output) {
			copy(input, output);
		} catch (IOException e) {
			exception = e;
		}
	}
	
	public void assertOk() throws IOException {
		try {
			join();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		if (exception != null) {
			throw exception;
		}
	}
	
}

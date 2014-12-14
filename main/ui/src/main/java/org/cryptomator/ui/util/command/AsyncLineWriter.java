/*******************************************************************************
 * Copyright (c) 2014 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Markus Kreusch
 ******************************************************************************/
package org.cryptomator.ui.util.command;

import java.io.IOException;
import java.io.OutputStream;

final class AsyncLineWriter extends Thread {

	private final String[] lines;
	private final OutputStream output;
	
	private IOException exception;
	
	public AsyncLineWriter(String[] lines, OutputStream output) {
		this.lines = lines;
		this.output = output;
		start();
	}
	
	@Override
	public void run() {
		try (OutputStream outputToBeClosed = output) {
			for (String line : lines) {
				output.write(line.getBytes());
				output.write("\n".getBytes());
				output.flush();
			}
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

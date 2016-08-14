/*******************************************************************************
 * Copyright (c) 2016 Markus Kreusch and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Markus Kreusch - initial implementation
 *******************************************************************************/
package org.cryptomator.common;

import java.util.stream.Stream;

/**
 * Utility to print stack traces while analyzing issues.
 * 
 * @author Markus Kreusch
 */
public class StackTrace {

	public static void print(String message) {
		Thread thread = Thread.currentThread();
		System.err.println(stackTraceFor(message, thread));
	}

	private static String stackTraceFor(String message, Thread thread) {
		StringBuilder result = new StringBuilder();
		appendMessageAndThreadName(result, message, thread);
		appendStackTrace(thread, result);
		return result.toString();
	}

	private static void appendStackTrace(Thread thread, StringBuilder result) {
		Stream.of(thread.getStackTrace()) //
				.skip(4) //
				.forEach(stackTraceElement -> append(stackTraceElement, result));
	}

	private static void appendMessageAndThreadName(StringBuilder result, String message, Thread thread) {
		result //
				.append('[') //
				.append(thread.getName()) //
				.append("] ") //
				.append(message);
	}

	private static void append(StackTraceElement stackTraceElement, StringBuilder result) {
		String className = stackTraceElement.getClassName();
		String methodName = stackTraceElement.getMethodName();
		String fileName = stackTraceElement.getFileName();
		int lineNumber = stackTraceElement.getLineNumber();
		result.append('\n') //
				.append(className).append(':').append(methodName) //
				.append(" (").append(fileName).append(':').append(lineNumber).append(')');
	}

}

/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem;

import static java.lang.String.format;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;

public class DeadlockSafeFileOpener {

	public static DeadlockSafeFileOpener withReadable(File file) {
		return new DeadlockSafeFileOpener().andReadable(file);
	}

	public static DeadlockSafeFileOpener withWritable(File file) {
		return new DeadlockSafeFileOpener().andWritable(file);
	}

	private final SortedMap<File, Consumer<File>> filesWithOperation = new TreeMap<>();

	private final Map<File, ReadableFile> readableFiles = new HashMap<>();
	private final Map<File, WritableFile> writableFiles = new HashMap<>();

	private DeadlockSafeFileOpener() {
	}

	public DeadlockSafeFileOpener andReadable(File file) {
		if (filesWithOperation.put(file, this::openReadable) != null) {
			throw new IllegalArgumentException(format("File %s already marked for opening", file));
		}
		return this;
	}

	public DeadlockSafeFileOpener andWritable(File file) {
		if (filesWithOperation.put(file, this::openWritable) != null) {
			throw new IllegalArgumentException(format("File %s already marked for opening", file));
		}
		return this;
	}

	private void openReadable(File file) {
		readableFiles.put(file, file.openReadable());
	}

	private void openWritable(File file) {
		writableFiles.put(file, file.openWritable());
	}

	public OpenFiles open() {
		try {
			filesWithOperation.forEach((file, openAction) -> openAction.accept(file));
		} catch (RuntimeException e) {
			OpenFiles.cleanup(readableFiles.values(), writableFiles.values());
			throw e;
		}
		return new OpenFiles(readableFiles, writableFiles);
	}

}

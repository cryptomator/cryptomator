/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem;

import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenFiles implements AutoCloseable {

	private final static Logger LOG = LoggerFactory.getLogger(OpenFiles.class);

	private final Map<File, ReadableFile> readableFiles;
	private final Map<File, WritableFile> writableFiles;

	public OpenFiles(Map<File, ReadableFile> readableFiles, Map<File, WritableFile> writableFiles) {
		this.readableFiles = readableFiles;
		this.writableFiles = writableFiles;
	}

	@Override
	public void close() throws UncheckedIOException {
		OpenFiles.cleanup(readableFiles.values(), writableFiles.values());
	}

	public ReadableFile readable(File file) {
		return readableFiles.computeIfAbsent(file, fileNotOpenForReading -> {
			throw new IllegalArgumentException(String.format("File %s is not open for reading", fileNotOpenForReading));
		});
	}

	public WritableFile writable(File file) {
		return writableFiles.computeIfAbsent(file, fileNotOpenForWriting -> {
			throw new IllegalArgumentException(String.format("File %s is not open for writing", fileNotOpenForWriting));
		});
	}

	static void cleanup(Collection<ReadableFile> readableFiles, Collection<WritableFile> writableFiles) {
		Iterator<? extends AutoCloseable> iterator = Stream.concat(readableFiles.stream(), writableFiles.stream()).iterator();
		UncheckedIOException firstException = null;
		while (iterator.hasNext()) {
			AutoCloseable openFile = iterator.next();
			try {
				openFile.close();
			} catch (UncheckedIOException e) {
				if (firstException == null) {
					firstException = e;
				} else {
					firstException.addSuppressed(e);
				}
			} catch (Exception e) {
				LOG.error("Unexpected exception during close on " + openFile.getClass().getSimpleName(), e);
			}
		}
		if (firstException != null) {
			throw firstException;
		}
	}

}

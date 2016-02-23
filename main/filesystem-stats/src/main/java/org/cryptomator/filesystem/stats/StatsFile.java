/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.stats;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.filesystem.delegating.DelegatingFile;
import org.cryptomator.filesystem.delegating.DelegatingReadableFile;
import org.cryptomator.filesystem.delegating.DelegatingWritableFile;

class StatsFile extends DelegatingFile<StatsFolder> {

	private final Consumer<Long> readCounter;
	private final Consumer<Long> writeCounter;

	public StatsFile(StatsFolder parent, File delegate, Consumer<Long> readCounter, Consumer<Long> writeCounter) {
		super(parent, delegate);
		this.readCounter = readCounter;
		this.writeCounter = writeCounter;
	}

	@Override
	public ReadableFile openReadable() throws UncheckedIOException {
		return new DelegatingReadableFile(delegate.openReadable()) {
			@Override
			public int read(ByteBuffer target) throws UncheckedIOException {
				int num = super.read(target);
				readCounter.accept((long) num);
				return num;
			}
		};
	}

	@Override
	public WritableFile openWritable() throws UncheckedIOException {
		return new DelegatingWritableFile(delegate.openWritable()) {
			@Override
			public int write(ByteBuffer source) throws UncheckedIOException {
				int num = super.write(source);
				writeCounter.accept((long) num);
				return num;
			}
		};
	}

}

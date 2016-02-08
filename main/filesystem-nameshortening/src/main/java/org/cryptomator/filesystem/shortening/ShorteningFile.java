/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.shortening;

import java.io.UncheckedIOException;
import java.util.concurrent.atomic.AtomicReference;

import org.cryptomator.common.LazyInitializer;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.filesystem.delegating.DelegatingFile;

class ShorteningFile extends DelegatingFile<ShorteningFolder> {

	private final AtomicReference<String> longName;
	private final FilenameShortener shortener;

	public ShorteningFile(ShorteningFolder parent, File delegate, String name, FilenameShortener shortener) {
		super(parent, delegate);
		this.longName = new AtomicReference<>(name);
		this.shortener = shortener;
	}

	@Override
	public String name() throws UncheckedIOException {
		return LazyInitializer.initializeLazily(longName, () -> {
			return shortener.inflate(shortenedName());
		});
	}

	private String shortenedName() {
		return delegate.name();
	}

	@Override
	public WritableFile openWritable() throws UncheckedIOException {
		if (shortener.isShortened(shortenedName())) {
			shortener.saveMapping(name(), shortenedName());
		}
		return super.openWritable();
	}

	@Override
	public void moveTo(File destination) {
		super.moveTo(destination);
		if (destination instanceof ShorteningFile) {
			ShorteningFile dest = (ShorteningFile) destination;
			if (shortener.isShortened(dest.shortenedName())) {
				shortener.saveMapping(dest.name(), dest.shortenedName());
			}
		}
	}

}

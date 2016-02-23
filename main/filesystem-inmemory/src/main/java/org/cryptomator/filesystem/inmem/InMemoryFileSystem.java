/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.inmem;

import java.time.Instant;
import java.util.Optional;

import org.cryptomator.filesystem.FileSystem;

public class InMemoryFileSystem extends InMemoryFolder implements FileSystem {

	public InMemoryFileSystem() {
		super(null, "", Instant.now(), Instant.now());
	}

	@Override
	public Optional<InMemoryFolder> parent() {
		return Optional.empty();
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public void delete() {
		// no-op.
	}

	@Override
	public String toString() {
		return "/";
	}

	@Override
	public Optional<Long> quotaUsedBytes() {
		long used = Runtime.getRuntime().totalMemory();
		return Optional.of(used);
	}

	@Override
	public Optional<Long> quotaAvailableBytes() {
		long available = Runtime.getRuntime().freeMemory();
		return Optional.of(available);
	}

}

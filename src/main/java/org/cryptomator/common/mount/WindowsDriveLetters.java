/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.common.mount;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.SystemUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

@Singleton
public final class WindowsDriveLetters {

	private static final Set<Path> A_TO_Z;

	static {
		var sortedSet = new TreeSet<Path>();
		IntStream.rangeClosed('A', 'Z').mapToObj(i -> Path.of((char) i + ":\\")).forEach(sortedSet::add);
		A_TO_Z = Collections.unmodifiableSet(sortedSet);
	}

	@Inject
	public WindowsDriveLetters() {
	}

	public Set<Path> getAll() {
		return A_TO_Z;
	}

	public Set<Path> getOccupied() {
		if (!SystemUtils.IS_OS_WINDOWS) {
			return Set.of();
		} else {
			Iterable<Path> rootDirs = FileSystems.getDefault().getRootDirectories();
			return StreamSupport.stream(rootDirs.spliterator(), false).collect(Collectors.toUnmodifiableSet());
		}
	}

	public Set<Path> getAvailable() {
		return Sets.difference(getAll(), getOccupied());
	}

	/**
	 * Skips A and B and only returns them if all others are occupied.
	 *
	 * @return an Optional containing either the letter of a free drive letter or empty, if none is available
	 */
	public Optional<Path> getFirstDesiredAvailable() {
		var availableDriveLetters = getAvailable();
		var optString = availableDriveLetters.stream().filter(this::notAOrB).findFirst();
		return optString.or(() -> availableDriveLetters.stream().findFirst());
	}

	private boolean notAOrB(Path driveLetter) {
		return !(Path.of("A:\\").equals(driveLetter) || Path.of("B:\\").equals(driveLetter));
	}

}

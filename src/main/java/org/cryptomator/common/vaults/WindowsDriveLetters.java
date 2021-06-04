/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.common.vaults;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.SystemUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

@Singleton
public final class WindowsDriveLetters {

	private static final Set<String> C_TO_Z;

	static {
		try (IntStream stream = IntStream.rangeClosed('C', 'Z')) {
			C_TO_Z = stream.mapToObj(i -> String.valueOf((char) i)).collect(ImmutableSet.toImmutableSet());
		}
	}

	@Inject
	public WindowsDriveLetters() {
	}

	public Set<String> getAllDriveLetters() {
		return C_TO_Z;
	}

	public Set<String> getOccupiedDriveLetters() {
		if (!SystemUtils.IS_OS_WINDOWS) {
			return Set.of();
		} else {
			Iterable<Path> rootDirs = FileSystems.getDefault().getRootDirectories();
			return StreamSupport.stream(rootDirs.spliterator(), false).map(p -> p.toString().substring(0, 1)).collect(Collectors.toSet());
		}
	}

	public Set<String> getAvailableDriveLetters() {
		return Sets.difference(getAllDriveLetters(), getOccupiedDriveLetters());
	}

	public Optional<String> getAvailableDriveLetter() {
		return getAvailableDriveLetters().stream().findFirst();
	}

	public Optional<Path> getAvailableDriveLetterPath() {
		return getAvailableDriveLetter().map(this::toPath);
	}

	public Path toPath(String driveLetter) {
		return Path.of(driveLetter + ":\\");
	}
}

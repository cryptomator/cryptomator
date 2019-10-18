/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.common.vaults;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

@Singleton
public final class WindowsDriveLetters {

	private static final Logger LOG = LoggerFactory.getLogger(WindowsDriveLetters.class);
	private static final Set<String> A_TO_Z;

	static {
		try (IntStream stream = IntStream.rangeClosed('A', 'Z')) {
			A_TO_Z = stream.mapToObj(i -> String.valueOf((char) i)).collect(Collectors.toSet());
		}
	}

	@Inject
	public WindowsDriveLetters() {
	}

	public Set<String> getAllDriveLetters() {
		return A_TO_Z;
	}

	public Set<String> getOccupiedDriveLetters() {
		if (!SystemUtils.IS_OS_WINDOWS) {
			LOG.warn("Attempted to get occupied drive letters on non-Windows machine.");
			return Set.of();
		} else {
			Iterable<Path> rootDirs = FileSystems.getDefault().getRootDirectories();
			return StreamSupport.stream(rootDirs.spliterator(), false).map(Path::toString).collect(Collectors.toSet());
		}
	}

	public Set<String> getAvailableDriveLetters() {
		return Sets.difference(A_TO_Z, getOccupiedDriveLetters());
	}

}

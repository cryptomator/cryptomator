/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.model;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.FxApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

@FxApplicationScoped
public final class WindowsDriveLetters {

	private static final Logger LOG = LoggerFactory.getLogger(WindowsDriveLetters.class);
	private static final Set<Path> D_TO_Z;

	static {
		try (IntStream stream = IntStream.rangeClosed('D', 'Z')) {
			D_TO_Z = stream.mapToObj(i -> Path.of(((char) i)+":\\")).collect(Collectors.toSet());
		}
	}

	@Inject
	public WindowsDriveLetters() {
	}

	public Set<Path> getOccupiedDriveLetters() {
		if (!SystemUtils.IS_OS_WINDOWS) {
			LOG.warn("Attempted to get occupied drive letters on non-Windows machine.");
			return Set.of();
		} else {
			Iterable<Path> rootDirs = FileSystems.getDefault().getRootDirectories();
			return StreamSupport.stream(rootDirs.spliterator(), false).collect(Collectors.toSet());
		}
	}

	public Set<Path> getAvailableDriveLetters() {
		Set<Path> occupiedDriveLetters = getOccupiedDriveLetters();
		Predicate<Path> isOccupiedDriveLetter = occupiedDriveLetters::contains;
		return D_TO_Z.stream().filter(isOccupiedDriveLetter.negate()).collect(Collectors.toSet());
	}

}

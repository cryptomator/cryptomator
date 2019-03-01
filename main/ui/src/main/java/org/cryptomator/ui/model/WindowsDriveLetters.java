/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.model;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.FxApplicationScoped;

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

	private static final Set<Character> D_TO_Z;

	static {
		try (IntStream stream = IntStream.rangeClosed('D', 'Z')) {
			D_TO_Z = stream.mapToObj(i -> (char) i).collect(Collectors.toSet());
		}
	}

	@Inject
	public WindowsDriveLetters() {
	}

	public Set<Character> getOccupiedDriveLetters() {
		if (!SystemUtils.IS_OS_WINDOWS) {
			throw new UnsupportedOperationException("This method is only defined for Windows file systems");
		}
		Iterable<Path> rootDirs = FileSystems.getDefault().getRootDirectories();
		return StreamSupport.stream(rootDirs.spliterator(), false).map(Path::toString).map(CharUtils::toChar).map(Character::toUpperCase).collect(Collectors.toSet());
	}

	public Set<Character> getAvailableDriveLetters() {
		Set<Character> occupiedDriveLetters = getOccupiedDriveLetters();
		Predicate<Character> isOccupiedDriveLetter = occupiedDriveLetters::contains;
		return D_TO_Z.stream().filter(isOccupiedDriveLetter.negate()).collect(Collectors.toSet());
	}

}

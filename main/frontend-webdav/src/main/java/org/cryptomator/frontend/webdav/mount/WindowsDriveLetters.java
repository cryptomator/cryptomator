/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav.mount;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.IntStream.rangeClosed;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.SystemUtils;

import com.google.common.collect.Sets;


@Singleton
public final class WindowsDriveLetters {
	
	private static final Set<Character> A_TO_Z = rangeClosed('A', 'Z').mapToObj(i -> (char) i).collect(toSet());
	
	@Inject
	public WindowsDriveLetters() {
	}
	
	public Set<Character> getOccupiedDriveLetters() {
		if (!SystemUtils.IS_OS_WINDOWS) {
			throw new UnsupportedOperationException("This method is only defined for Windows file systems");
		}
		Iterable<Path> rootDirs = FileSystems.getDefault().getRootDirectories();
		return StreamSupport.stream(rootDirs.spliterator(), false).map(Path::toString).map(CharUtils::toChar).map(Character::toUpperCase).collect(toSet());
	}
	
	public Set<Character> getAvailableDriveLetters() {
		return Sets.difference(A_TO_Z, getOccupiedDriveLetters());
	}

}

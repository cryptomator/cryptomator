package org.cryptomator.ui.util.mount;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.IntStream.rangeClosed;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.Sets;


@Singleton
public final class WindowsDriveLetters {
	
	private static final Set<Character> A_TO_Z = rangeClosed('A', 'Z').mapToObj(i -> (char) i).collect(toSet());
	
	@Inject
	public WindowsDriveLetters() {
	}
	
	public Set<Character> getOccupiedDriveLetters() {
		Iterable<Path> rootDirs = FileSystems.getDefault().getRootDirectories();
		return StreamSupport.stream(rootDirs.spliterator(), false).map(path -> path.toString().toUpperCase().charAt(0)).collect(toSet());
	}
	
	public Set<Character> getAvailableDriveLetters() {
		return Sets.difference(A_TO_Z, getOccupiedDriveLetters());
	}

}

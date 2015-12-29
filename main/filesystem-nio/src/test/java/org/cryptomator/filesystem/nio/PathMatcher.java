package org.cryptomator.filesystem.nio;

import static org.hamcrest.CoreMatchers.is;

import java.nio.file.Files;
import java.nio.file.Path;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

class PathMatcher {

	public static Matcher<Path> isDirectory() {
		return new FeatureMatcher<Path, Boolean>(is(true), "a path for which Files.isDirectory", "Files.isDirectory") {
			@Override
			protected Boolean featureValueOf(Path actual) {
				return Files.isDirectory(actual);
			}
		};
	}

	public static Matcher<Path> isFile() {
		return new FeatureMatcher<Path, Boolean>(is(true), "a path for which Files.isRegularFile", "Files.isRegularFile") {
			@Override
			protected Boolean featureValueOf(Path actual) {
				return Files.isRegularFile(actual);
			}
		};
	}

	public static Matcher<Path> doesNotExist() {
		return new FeatureMatcher<Path, Boolean>(is(false), "a path for which Files.exists", "Files.exists") {
			@Override
			protected Boolean featureValueOf(Path actual) {
				return Files.exists(actual);
			}
		};
	}

	public static Matcher<Path> doesExist() {
		return new FeatureMatcher<Path, Boolean>(is(true), "a path for which Files.exists", "Files.exists") {
			@Override
			protected Boolean featureValueOf(Path actual) {
				return Files.exists(actual);
			}
		};
	}

}

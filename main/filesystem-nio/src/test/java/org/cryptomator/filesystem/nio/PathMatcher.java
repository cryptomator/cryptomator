package org.cryptomator.filesystem.nio;

import static org.hamcrest.CoreMatchers.is;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.hamcrest.Description;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

class PathMatcher {

	public static Matcher<Path> isDirectory() {
		return new FeatureMatcher<Path, Boolean>(is(true), "a path for which Files.isDirectory", "Files.isDirectory") {
			@Override
			protected Boolean featureValueOf(Path actual) {
				return Files.isDirectory(actual);
			}
		};
	}

	public static IsFileMatcher isFile() {
		return IsFileMatcher.INSTANCE;
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

	public static class IsFileMatcher extends FeatureMatcher<Path, Boolean> {

		public static final IsFileMatcher INSTANCE = new IsFileMatcher();

		private IsFileMatcher() {
			super(is(true), "a path for which Files.isRegularFile", "Files.isRegularFile");
		}

		@Override
		protected Boolean featureValueOf(Path actual) {
			return Files.isRegularFile(actual);
		}

		public Matcher<Path> withContent(String value) {
			return new IsFileWithContentMatcher(value.getBytes());
		}

		public Matcher<Path> withContent(byte[] value) {
			return new IsFileWithContentMatcher(value);
		}

		public Matcher<Path> thatIsEmpty() {
			return withContent(new byte[0]);
		}

	}

	public static class IsFileWithContentMatcher extends TypeSafeDiagnosingMatcher<Path> {

		private final byte[] expectedContent;

		public IsFileWithContentMatcher(byte[] content) {
			this.expectedContent = content;
		}

		@Override
		public void describeTo(Description description) {
			description //
					.appendText("a file with content ") //
					.appendText(Arrays.toString(expectedContent));
		}

		@Override
		protected boolean matchesSafely(Path path, Description mismatchDescription) {
			if (!IsFileMatcher.INSTANCE.matches(path)) {
				IsFileMatcher.INSTANCE.describeMismatch(path, mismatchDescription);
				return false;
			}

			byte[] actualContent = getFileContent(path);
			if (!Arrays.equals(actualContent, expectedContent)) {
				mismatchDescription //
						.appendText("a file with content ") //
						.appendText(Arrays.toString(actualContent));
				return false;
			}

			return true;
		}

		private byte[] getFileContent(Path path) {
			try {
				return Files.readAllBytes(path);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

	}

}

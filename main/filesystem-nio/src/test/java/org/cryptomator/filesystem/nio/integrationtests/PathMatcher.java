package org.cryptomator.filesystem.nio.integrationtests;

import static java.util.Arrays.copyOfRange;
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

		public Matcher<Path> withPartialContentAt(int offset, byte[] value) {
			return new IsFileWithContentMatcher(offset, value);
		}

		public Matcher<Path> withContent(byte[] value) {
			return new IsFileWithContentMatcher(value);
		}

		public Matcher<Path> thatIsEmpty() {
			return withContent(new byte[0]);
		}

	}

	public static class IsFileWithContentMatcher extends TypeSafeDiagnosingMatcher<Path> {

		private static final int NO_OFFSET = -1;

		private final byte[] expectedContent;
		private final int offset;

		public IsFileWithContentMatcher(int offset, byte[] content) {
			this.expectedContent = content;
			this.offset = offset;
		}

		public IsFileWithContentMatcher(byte[] content) {
			this.expectedContent = content;
			this.offset = NO_OFFSET;
		}

		@Override
		public void describeTo(Description description) {
			description //
					.appendText("a file with content ") //
					.appendText(Arrays.toString(expectedContent));
			if (offset != NO_OFFSET) {
				description.appendText(" starting at byte ") //
						.appendValue(offset);
			}
		}

		@Override
		protected boolean matchesSafely(Path path, Description mismatchDescription) {
			if (!IsFileMatcher.INSTANCE.matches(path)) {
				IsFileMatcher.INSTANCE.describeMismatch(path, mismatchDescription);
				return false;
			}

			byte[] actualContent = getFileContent(path);
			if (offset != NO_OFFSET) {
				actualContent = copyOfRange(actualContent, offset, offset + expectedContent.length);
			}

			if (!Arrays.equals(actualContent, expectedContent)) {
				mismatchDescription //
						.appendText("a file with content ") //
						.appendText(Arrays.toString(actualContent));
				if (offset != NO_OFFSET) {
					mismatchDescription.appendText(" starting at byte ") //
							.appendValue(offset);
				}
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

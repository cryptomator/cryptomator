package org.cryptomator.filesystem.invariants.matchers;

import static org.hamcrest.CoreMatchers.is;

import java.nio.ByteBuffer;
import java.util.function.Function;

import org.cryptomator.common.test.matcher.PropertyMatcher;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.ReadableFile;
import org.hamcrest.Matcher;

public class NodeMatchers {

	public static Matcher<Folder> folderWithName(String name) {
		return new PropertyMatcher<>(Folder.class, Folder::name, "name", is(name));
	}

	public static Matcher<File> fileWithName(String name) {
		return new PropertyMatcher<>(File.class, File::name, "name", is(name));
	}

	public static Matcher<File> hasContent(byte[] content) {
		return new PropertyMatcher<>(File.class, readFileContentWithLength(content.length), "content", is(content));
	}

	private static Function<File, byte[]> readFileContentWithLength(int expectedLength) {
		return file -> {
			try (ReadableFile readable = file.openReadable()) {
				ByteBuffer buffer = ByteBuffer.allocate(expectedLength + 1);
				readable.read(buffer);
				if (buffer.remaining() == 0) {
					throw new IllegalStateException("File content > expectedLength of " + expectedLength);
				}
				buffer.flip();
				byte[] result = new byte[buffer.limit()];
				buffer.get(result);
				return result;
			}
		};
	}

}

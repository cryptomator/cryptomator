package org.cryptomator.shortening;

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.BaseNCodec;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;

class FilenameShortener {

	private static final String LONG_NAME_FILE_EXT = ".lng";
	private static final ThreadLocal<MessageDigest> SHA1 = new ThreadLocalSha1();
	private static final BaseNCodec BASE32 = new Base32();
	private final Folder metadataRoot;
	private final int threshold;

	public FilenameShortener(Folder metadataRoot, int threshold) {
		this.metadataRoot = metadataRoot;
		this.threshold = threshold;
	}

	public String inflate(String shortName) {
		if (shortName.endsWith(LONG_NAME_FILE_EXT)) {
			return loadMapping(shortName);
		} else {
			return shortName;
		}
	}

	public String deflate(String longName) {
		if (longName.length() < threshold) {
			return longName;
		} else {
			final byte[] hashBytes = SHA1.get().digest(longName.getBytes());
			final String hash = BASE32.encodeAsString(hashBytes);
			return hash + LONG_NAME_FILE_EXT;
		}
	}

	public boolean isShortened(String name) {
		return name.endsWith(LONG_NAME_FILE_EXT);
	}

	public void saveMapping(String longName, String shortName) {
		final File mappingFile = mappingFile(shortName);
		if (!mappingFile.exists()) {
			mappingFile.parent().get().create();
			try (WritableFile writable = mappingFile.openWritable()) {
				writable.write(ByteBuffer.wrap(longName.getBytes(StandardCharsets.UTF_8)));
			}
		}
	}

	private File mappingFile(String deflated) {
		final Folder folder = metadataRoot.folder(deflated.substring(0, 2)).folder(deflated.substring(2, 4));
		return folder.file(deflated);
	}

	private String loadMapping(String shortName) {
		final File mappingFile = mappingFile(shortName);
		if (!mappingFile.exists()) {
			throw new UncheckedIOException(new FileNotFoundException("Mapping file not found " + mappingFile));
		} else {
			try (ReadableFile readable = mappingFile.openReadable()) {
				// TODO buffer might be to small
				final ByteBuffer buf = ByteBuffer.allocate(1024);
				readable.read(buf);
				buf.flip();
				final byte[] bytes = new byte[buf.remaining()];
				buf.get(bytes);
				return new String(bytes, StandardCharsets.UTF_8);
			}
		}
	}

	private static class ThreadLocalSha1 extends ThreadLocal<MessageDigest> {

		@Override
		protected MessageDigest initialValue() {
			try {
				return MessageDigest.getInstance("SHA-1");
			} catch (NoSuchAlgorithmException e) {
				throw new AssertionError("SHA-1 exists in every JVM");
			}
		}

		@Override
		public MessageDigest get() {
			final MessageDigest messageDigest = super.get();
			messageDigest.reset();
			return messageDigest;
		}
	}

}

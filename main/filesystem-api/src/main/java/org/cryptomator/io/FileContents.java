package org.cryptomator.io;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.WritableFile;

public final class FileContents {

	public static final FileContents UTF_8 = FileContents.withCharset(StandardCharsets.UTF_8);

	private final Charset charset;

	private FileContents(Charset charset) {
		this.charset = charset;
	}

	/**
	 * Reads the whole content from the given file.
	 * 
	 * @param file File whose content should be read.
	 * @return The file's content interpreted in this FileContents' charset.
	 */
	public String readContents(File file) {
		try (Reader reader = Channels.newReader(file.openReadable(), charset.newDecoder(), -1)) {
			return IOUtils.toString(reader);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Writes the string into the file encoded with this FileContents' charset.
	 * This methods replaces any previously existing content, i.e. the string will be the sole content.
	 * 
	 * @param file File whose content should be written.
	 * @param content The new content.
	 */
	public void writeContents(File file, String content) {
		try (WritableFile writable = file.openWritable()) {
			writable.truncate();
			writable.write(charset.encode(content));
		}
	}

	public static FileContents withCharset(Charset charset) {
		return new FileContents(charset);
	}

}

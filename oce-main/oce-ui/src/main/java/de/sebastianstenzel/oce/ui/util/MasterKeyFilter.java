package de.sebastianstenzel.oce.ui.util;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;

import de.sebastianstenzel.oce.crypto.aes256.Aes256Cryptor;

public class MasterKeyFilter implements Filter<Path> {

	public static MasterKeyFilter FILTER = new MasterKeyFilter();

	private final String masterKeyExt = Aes256Cryptor.MASTERKEY_FILE_EXT.toLowerCase();

	@Override
	public boolean accept(Path child) throws IOException {
		return child.getFileName().toString().toLowerCase().endsWith(masterKeyExt);
	}

	public static final DirectoryStream<Path> filteredDirectory(Path dir) throws IOException {
		return Files.newDirectoryStream(dir, FILTER);
	}

}

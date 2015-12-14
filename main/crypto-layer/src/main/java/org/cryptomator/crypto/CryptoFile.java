package org.cryptomator.crypto;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;

public class CryptoFile extends CryptoNode implements File {

	private static final String ENCRYPTED_FILE_EXT = ".file";

	public CryptoFile(CryptoFolder parent, String name) {
		super(parent, name);
	}

	@Override
	String encryptedName() {
		return name() + ENCRYPTED_FILE_EXT;
	}

	@Override
	public Instant lastModified() throws UncheckedIOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReadableFile openReadable(long timeout, TimeUnit unit) throws IOException, TimeoutException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WritableFile openWritable(long timeout, TimeUnit unit) throws IOException, TimeoutException {
		// TODO Auto-generated method stub
		return null;
	}

}

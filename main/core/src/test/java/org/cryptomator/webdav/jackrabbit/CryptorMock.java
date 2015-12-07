package org.cryptomator.webdav.jackrabbit;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

import org.cryptomator.crypto.Cryptor;
import org.cryptomator.crypto.exceptions.DecryptFailedException;
import org.cryptomator.crypto.exceptions.EncryptFailedException;
import org.cryptomator.crypto.exceptions.MacAuthenticationFailedException;
import org.cryptomator.crypto.exceptions.UnsupportedKeyLengthException;
import org.cryptomator.crypto.exceptions.UnsupportedVaultException;
import org.cryptomator.crypto.exceptions.WrongPasswordException;

class CryptorMock implements Cryptor {

	private static final int BUFSIZE = 32768;

	@Override
	public void randomizeMasterKey() {
		// noop
	}

	@Override
	public void encryptMasterKey(OutputStream out, CharSequence password) throws IOException {
		// noop
	}

	@Override
	public void decryptMasterKey(InputStream in, CharSequence password) throws DecryptFailedException, WrongPasswordException, UnsupportedKeyLengthException, IOException, UnsupportedVaultException {
		// noop
	}

	@Override
	public String encryptDirectoryPath(String cleartextDirectoryId, String nativePathSep) {
		return cleartextDirectoryId;
	}

	@Override
	public String encryptFilename(String cleartextName) {
		return cleartextName;
	}

	@Override
	public String decryptFilename(String ciphertextName) throws DecryptFailedException {
		return ciphertextName;
	}

	@Override
	public Long decryptedContentLength(SeekableByteChannel encryptedFile) throws IOException, MacAuthenticationFailedException {
		return encryptedFile.size();
	}

	@Override
	public Long decryptFile(SeekableByteChannel encryptedFile, OutputStream plaintextFile, boolean authenticate) throws IOException, DecryptFailedException {
		ByteBuffer buf = ByteBuffer.allocate(BUFSIZE);
		long numReadTotal = 0;
		int numRead = 0;
		while ((numRead = encryptedFile.read(buf)) != -1) {
			numReadTotal += numRead;
			buf.flip();
			byte[] bytes = new byte[numRead];
			buf.get(bytes);
			plaintextFile.write(bytes);
			buf.rewind();
		}
		return numReadTotal;
	}

	@Override
	public Long decryptRange(SeekableByteChannel encryptedFile, OutputStream plaintextFile, long pos, long length, boolean authenticate) throws IOException, DecryptFailedException {
		encryptedFile.position(pos);

		ByteBuffer buf = ByteBuffer.allocate(BUFSIZE);
		long numReadTotal = 0;
		int numRead = 0;
		while ((numRead = encryptedFile.read(buf)) != -1 && numReadTotal < length) {
			int len = (int) Math.min(Math.min(numRead, BUFSIZE), length - numReadTotal); // known to fit into integer
			numReadTotal += len;
			buf.flip();
			byte[] bytes = new byte[len];
			buf.get(bytes);
			plaintextFile.write(bytes);
			buf.rewind();
		}
		return numReadTotal;
	}

	@Override
	public Long encryptFile(InputStream plaintextFile, SeekableByteChannel encryptedFile) throws IOException, EncryptFailedException {
		byte[] buf = new byte[BUFSIZE];
		long numWrittenTotal = 0;
		int numRead = 0;
		while ((numRead = plaintextFile.read(buf)) != -1) {
			numWrittenTotal += numRead;
			encryptedFile.write(ByteBuffer.wrap(buf, 0, numRead));
		}
		return numWrittenTotal;
	}

}

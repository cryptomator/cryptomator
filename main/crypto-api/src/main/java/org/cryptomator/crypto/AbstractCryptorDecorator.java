package org.cryptomator.crypto;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;

import javax.security.auth.DestroyFailedException;

import org.cryptomator.crypto.exceptions.DecryptFailedException;
import org.cryptomator.crypto.exceptions.EncryptFailedException;
import org.cryptomator.crypto.exceptions.MacAuthenticationFailedException;
import org.cryptomator.crypto.exceptions.UnsupportedKeyLengthException;
import org.cryptomator.crypto.exceptions.UnsupportedVaultException;
import org.cryptomator.crypto.exceptions.WrongPasswordException;

public class AbstractCryptorDecorator implements Cryptor {

	protected final Cryptor cryptor;

	public AbstractCryptorDecorator(Cryptor cryptor) {
		this.cryptor = cryptor;
	}

	@Override
	public void encryptMasterKey(OutputStream out, CharSequence password) throws IOException {
		cryptor.encryptMasterKey(out, password);
	}

	@Override
	public void decryptMasterKey(InputStream in, CharSequence password) throws DecryptFailedException, WrongPasswordException, UnsupportedKeyLengthException, IOException, UnsupportedVaultException {
		cryptor.decryptMasterKey(in, password);
	}

	@Override
	public String encryptDirectoryPath(String cleartextDirectoryId, String nativePathSep) {
		return cryptor.encryptDirectoryPath(cleartextDirectoryId, nativePathSep);
	}

	@Override
	public String encryptFilename(String cleartextName) {
		return cryptor.encryptFilename(cleartextName);
	}

	@Override
	public String decryptFilename(String ciphertextName) throws DecryptFailedException {
		return cryptor.decryptFilename(ciphertextName);
	}

	@Override
	public Long decryptedContentLength(SeekableByteChannel encryptedFile) throws IOException, MacAuthenticationFailedException {
		return cryptor.decryptedContentLength(encryptedFile);
	}

	@Override
	public Long decryptFile(SeekableByteChannel encryptedFile, OutputStream plaintextFile, boolean authenticate) throws IOException, DecryptFailedException {
		return cryptor.decryptFile(encryptedFile, plaintextFile, authenticate);
	}

	@Override
	public Long decryptRange(SeekableByteChannel encryptedFile, OutputStream plaintextFile, long pos, long length, boolean authenticate) throws IOException, DecryptFailedException {
		return cryptor.decryptRange(encryptedFile, plaintextFile, pos, length, authenticate);
	}

	@Override
	public Long encryptFile(InputStream plaintextFile, SeekableByteChannel encryptedFile) throws IOException, EncryptFailedException {
		return cryptor.encryptFile(plaintextFile, encryptedFile);
	}

	@Override
	public void destroy() throws DestroyFailedException {
		cryptor.destroy();
	}

	@Override
	public boolean isDestroyed() {
		return cryptor.isDestroyed();
	}

}

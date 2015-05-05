package org.cryptomator.crypto;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Path;

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
	public String encryptDirectoryPath(String cleartextPath, String nativePathSep) {
		return cryptor.encryptDirectoryPath(cleartextPath, nativePathSep);
	}

	@Override
	public String encryptFilename(String cleartextName, CryptorMetadataSupport ioSupport) throws IOException {
		return cryptor.encryptFilename(cleartextName, ioSupport);
	}

	@Override
	public String decryptFilename(String ciphertextName, CryptorMetadataSupport ioSupport) throws IOException, DecryptFailedException {
		return cryptor.decryptFilename(ciphertextName, ioSupport);
	}

	@Override
	public Long decryptedContentLength(SeekableByteChannel encryptedFile) throws IOException, MacAuthenticationFailedException {
		return cryptor.decryptedContentLength(encryptedFile);
	}

	@Override
	public boolean isAuthentic(SeekableByteChannel encryptedFile) throws IOException {
		return cryptor.isAuthentic(encryptedFile);
	}

	@Override
	public Long decryptFile(SeekableByteChannel encryptedFile, OutputStream plaintextFile) throws IOException, DecryptFailedException {
		return cryptor.decryptFile(encryptedFile, plaintextFile);
	}

	@Override
	public Long decryptRange(SeekableByteChannel encryptedFile, OutputStream plaintextFile, long pos, long length) throws IOException, DecryptFailedException {
		return cryptor.decryptRange(encryptedFile, plaintextFile, pos, length);
	}

	@Override
	public Long encryptFile(InputStream plaintextFile, SeekableByteChannel encryptedFile) throws IOException, EncryptFailedException {
		return cryptor.encryptFile(plaintextFile, encryptedFile);
	}

	@Override
	public Filter<Path> getPayloadFilesFilter() {
		return cryptor.getPayloadFilesFilter();
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

/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package de.sebastianstenzel.oce.crypto.aes256;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.sebastianstenzel.oce.crypto.Cryptor;
import de.sebastianstenzel.oce.crypto.MetadataSupport;
import de.sebastianstenzel.oce.crypto.exceptions.DecryptFailedException;
import de.sebastianstenzel.oce.crypto.exceptions.UnsupportedKeyLengthException;
import de.sebastianstenzel.oce.crypto.exceptions.WrongPasswordException;
import de.sebastianstenzel.oce.crypto.io.SeekableByteChannelInputStream;
import de.sebastianstenzel.oce.crypto.io.SeekableByteChannelOutputStream;

public class Aes256Cryptor implements Cryptor, AesCryptographicConfiguration, FileNamingConventions {

	/**
	 * PRNG for cryptographically secure random numbers. Defaults to SHA1-based number generator.
	 * 
	 * @see http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#SecureRandom
	 */
	private static final SecureRandom SECURE_PRNG;

	/**
	 * Factory for deriveing keys. Defaults to PBKDF2/HMAC-SHA1.
	 * 
	 * @see PKCS #5, defined in RFC 2898
	 * @see http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#SecretKeyFactory
	 */
	private static final SecretKeyFactory PBKDF2_FACTORY;

	/**
	 * Defined in static initializer. Defaults to 256, but falls back to maximum value possible, if JCE isn't installed. JCE can be
	 * installed from here: http://www.oracle.com/technetwork/java/javase/downloads/.
	 */
	private static final int AES_KEY_LENGTH;

	/**
	 * Jackson JSON-Mapper.
	 */
	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * The decrypted master key. Its lifecycle starts with {@link #unlockStorage(Path, CharSequence)} or
	 * {@link #initializeStorage(Path, CharSequence)}. Its lifecycle ends with {@link #swipeSensitiveData()}.
	 */
	private final byte[] masterKey = new byte[MASTER_KEY_LENGTH];

	private static final int SIZE_OF_LONG = Long.SIZE / Byte.SIZE;
	private static final int SIZE_OF_INT = Integer.SIZE / Byte.SIZE;

	static {
		try {
			PBKDF2_FACTORY = SecretKeyFactory.getInstance(KEY_FACTORY_ALGORITHM);
			SECURE_PRNG = SecureRandom.getInstance(PRNG_ALGORITHM);
			final int maxKeyLen = Cipher.getMaxAllowedKeyLength(CRYPTO_ALGORITHM);
			AES_KEY_LENGTH = (maxKeyLen >= 256) ? 256 : maxKeyLen;
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Algorithm should exist.", e);
		}
	}

	public void initializeStorage(OutputStream masterkey, CharSequence password) throws IOException {
		try {
			// generate new masterkey:
			randomMasterKey();

			// derive key:
			final byte[] userSalt = randomData(SALT_LENGTH);
			final SecretKey userKey = pbkdf2(password, userSalt, PBKDF2_PW_ITERATIONS, AES_KEY_LENGTH);

			// encrypt:
			final byte[] iv = randomData(AES_BLOCK_LENGTH);
			final Cipher encCipher = this.cipher(MASTERKEY_CIPHER, userKey, iv, Cipher.ENCRYPT_MODE);
			byte[] encryptedUserKey = encCipher.doFinal(userKey.getEncoded());
			byte[] encryptedMasterKey = encCipher.doFinal(this.masterKey);

			// save encrypted masterkey:
			final Keys keys = new Keys();
			final Keys.Key ownerKey = new Keys.Key();
			ownerKey.setIterations(PBKDF2_PW_ITERATIONS);
			ownerKey.setIv(iv);
			ownerKey.setKeyLength(AES_KEY_LENGTH);
			ownerKey.setMasterkey(encryptedMasterKey);
			ownerKey.setSalt(userSalt);
			ownerKey.setPwVerification(encryptedUserKey);
			keys.setOwnerKey(ownerKey);
			objectMapper.writeValue(masterkey, keys);
		} catch (IllegalBlockSizeException | BadPaddingException ex) {
			throw new IllegalStateException("Block size hard coded. Padding irrelevant in ENCRYPT_MODE. IV must exist in CBC mode.", ex);
		}
	}

	public void unlockStorage(InputStream masterkey, CharSequence password) throws DecryptFailedException, WrongPasswordException, UnsupportedKeyLengthException, IOException {
		byte[] decrypted = new byte[0];
		try {
			// load encrypted masterkey:
			final Keys keys = objectMapper.readValue(masterkey, Keys.class);
			;
			final Keys.Key ownerKey = keys.getOwnerKey();

			// check, whether the key length is supported:
			final int maxKeyLen = Cipher.getMaxAllowedKeyLength(CRYPTO_ALGORITHM);
			if (ownerKey.getKeyLength() > maxKeyLen) {
				throw new UnsupportedKeyLengthException(ownerKey.getKeyLength(), maxKeyLen);
			}

			// derive key:
			final SecretKey userKey = pbkdf2(password, ownerKey.getSalt(), ownerKey.getIterations(), ownerKey.getKeyLength());

			// check password:
			final Cipher encCipher = this.cipher(MASTERKEY_CIPHER, userKey, ownerKey.getIv(), Cipher.ENCRYPT_MODE);
			byte[] encryptedUserKey = encCipher.doFinal(userKey.getEncoded());
			if (!Arrays.equals(ownerKey.getPwVerification(), encryptedUserKey)) {
				throw new WrongPasswordException();
			}

			// decrypt:
			final Cipher decCipher = this.cipher(MASTERKEY_CIPHER, userKey, ownerKey.getIv(), Cipher.DECRYPT_MODE);
			decrypted = decCipher.doFinal(ownerKey.getMasterkey());

			// everything ok, move decrypted data to masterkey:
			final ByteBuffer masterKeyBuffer = ByteBuffer.wrap(this.masterKey);
			masterKeyBuffer.put(decrypted);
		} catch (IllegalBlockSizeException | BadPaddingException | BufferOverflowException ex) {
			throw new DecryptFailedException(ex);
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("Algorithm should exist.", ex);
		} finally {
			Arrays.fill(decrypted, (byte) 0);
		}
	}

	/**
	 * Overwrites the {@link #masterKey} with zeros. As masterKey is a final field, this operation is ensured to work on its actual data.
	 * Otherwise developers could accidentally just assign a new object to the variable.
	 */
	@Override
	public void swipeSensitiveData() {
		Arrays.fill(this.masterKey, (byte) 0);
	}

	private Cipher cipher(String cipherTransformation, SecretKey key, byte[] iv, int cipherMode) {
		try {
			final Cipher cipher = Cipher.getInstance(cipherTransformation);
			cipher.init(cipherMode, key, new IvParameterSpec(iv));
			return cipher;
		} catch (InvalidKeyException ex) {
			throw new IllegalArgumentException("Invalid key.", ex);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException ex) {
			throw new IllegalStateException("Algorithm/Padding should exist and accept an IV.", ex);
		}
	}

	private byte[] randomData(int length) {
		final byte[] result = new byte[length];
		SECURE_PRNG.setSeed(SECURE_PRNG.generateSeed(PRNG_SEED_LENGTH));
		SECURE_PRNG.nextBytes(result);
		return result;
	}

	private void randomMasterKey() {
		SECURE_PRNG.setSeed(SECURE_PRNG.generateSeed(PRNG_SEED_LENGTH));
		SECURE_PRNG.nextBytes(this.masterKey);
	}

	private SecretKey pbkdf2(byte[] password, byte[] salt, int iterations, int keyLength) {
		final char[] pw = new char[password.length];
		try {
			byteToChar(password, pw);
			return pbkdf2(CharBuffer.wrap(pw), salt, iterations, keyLength);
		} finally {
			Arrays.fill(pw, (char) 0);
		}
	}

	private SecretKey pbkdf2(CharSequence password, byte[] salt, int iterations, int keyLength) {
		final int pwLen = password.length();
		final char[] pw = new char[pwLen];
		CharBuffer.wrap(password).get(pw, 0, pwLen);
		try {
			final KeySpec specs = new PBEKeySpec(pw, salt, iterations, keyLength);
			final SecretKey pbkdf2Key = PBKDF2_FACTORY.generateSecret(specs);
			final SecretKey aesKey = new SecretKeySpec(pbkdf2Key.getEncoded(), CRYPTO_ALGORITHM);
			return aesKey;
		} catch (InvalidKeySpecException ex) {
			throw new IllegalStateException("Specs are hard-coded.", ex);
		} finally {
			Arrays.fill(pw, (char) 0);
		}
	}

	private void byteToChar(byte[] source, char[] destination) {
		if (source.length != destination.length) {
			throw new IllegalArgumentException("char[] needs to be the same length as byte[]");
		}
		for (int i = 0; i < source.length; i++) {
			destination[i] = (char) (source[i] & 0xFF);
		}
	}

	@Override
	public String encryptPath(String cleartextPath, char encryptedPathSep, char cleartextPathSep, MetadataSupport metadataSupport) {
		try {
			final SecretKey key = this.pbkdf2(masterKey, EMPTY_SALT, PBKDF2_MASTERKEY_ITERATIONS, AES_KEY_LENGTH);
			final String[] cleartextPathComps = StringUtils.split(cleartextPath, cleartextPathSep);
			final List<String> encryptedPathComps = new ArrayList<>(cleartextPathComps.length);
			for (final String cleartext : cleartextPathComps) {
				final String encrypted = encryptPathComponent(cleartext, key);
				encryptedPathComps.add(encrypted);
			}
			return StringUtils.join(encryptedPathComps, encryptedPathSep);
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			throw new IllegalStateException("Unable to encrypt path: " + cleartextPath, e);
		}
	}

	private String encryptPathComponent(final String cleartext, final SecretKey key) throws IllegalBlockSizeException, BadPaddingException {
		if (cleartext.length() > PLAINTEXT_FILENAME_LENGTH_LIMIT) {
			return encryptLongPathComponent(cleartext, key);
		} else {
			return encryptShortPathComponent(cleartext, key);
		}
	}

	private String encryptShortPathComponent(final String cleartext, final SecretKey key) throws IllegalBlockSizeException, BadPaddingException {
		final Cipher cipher = this.cipher(FILE_NAME_CIPHER, key, EMPTY_IV, Cipher.ENCRYPT_MODE);
		final byte[] encryptedBytes = cipher.doFinal(cleartext.getBytes(Charsets.UTF_8));
		return ENCRYPTED_FILENAME_CODEC.encodeAsString(encryptedBytes) + BASIC_FILE_EXT;
	}

	private String encryptLongPathComponent(String cleartext, SecretKey key) {
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public String decryptPath(String encryptedPath, char encryptedPathSep, char cleartextPathSep, MetadataSupport metadataSupport) {
		try {
			final SecretKey key = this.pbkdf2(masterKey, EMPTY_SALT, PBKDF2_MASTERKEY_ITERATIONS, AES_KEY_LENGTH);
			final String[] encryptedPathComps = StringUtils.split(encryptedPath, encryptedPathSep);
			final List<String> cleartextPathComps = new ArrayList<>(encryptedPathComps.length);
			for (final String encrypted : encryptedPathComps) {
				final String cleartext = decryptPathComponent(encrypted, key);
				cleartextPathComps.add(new String(cleartext));
			}
			return StringUtils.join(cleartextPathComps, cleartextPathSep);
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			throw new IllegalStateException("Unable to decrypt path: " + encryptedPath, e);
		}
	}

	private String decryptPathComponent(final String encrypted, final SecretKey key) throws IllegalBlockSizeException, BadPaddingException {
		if (encrypted.endsWith(LONG_NAME_FILE_EXT)) {
			return decryptLongPathComponent(encrypted, key);
		} else if (encrypted.endsWith(BASIC_FILE_EXT)) {
			return decryptShortPathComponent(encrypted, key);
		} else {
			throw new IllegalArgumentException("Unsupported path component: " + encrypted);
		}
	}

	private String decryptShortPathComponent(final String encrypted, final SecretKey key) throws IllegalBlockSizeException, BadPaddingException {
		final String basename = StringUtils.removeEndIgnoreCase(encrypted, BASIC_FILE_EXT);
		final Cipher cipher = this.cipher(FILE_NAME_CIPHER, key, EMPTY_IV, Cipher.DECRYPT_MODE);
		final byte[] encryptedBytes = ENCRYPTED_FILENAME_CODEC.decode(basename);
		final byte[] cleartextBytes = cipher.doFinal(encryptedBytes);
		return new String(cleartextBytes, Charsets.UTF_8);
	}

	private String decryptLongPathComponent(final String encrypted, final SecretKey key) {
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public Long decryptedContentLength(SeekableByteChannel encryptedFile, MetadataSupport metadataSupport) throws IOException {
		final ByteBuffer sizeBuffer = ByteBuffer.allocate(SIZE_OF_LONG);
		final int read = encryptedFile.read(sizeBuffer);
		if (read == SIZE_OF_LONG) {
			return sizeBuffer.getLong(0);
		} else {
			return null;
		}
	}

	@Override
	public Long decryptedFile(SeekableByteChannel encryptedFile, OutputStream plaintextFile) throws IOException {
		// skip content size:
		encryptedFile.position(SIZE_OF_LONG);

		// read iv:
		final ByteBuffer countingIv = ByteBuffer.allocate(AES_BLOCK_LENGTH);
		final int read = encryptedFile.read(countingIv);
		if (read != AES_BLOCK_LENGTH) {
			throw new IOException("Failed to read encrypted file header.");
		}

		// derive secret key and generate cipher:
		final SecretKey key = this.pbkdf2(masterKey, EMPTY_SALT, PBKDF2_MASTERKEY_ITERATIONS, AES_KEY_LENGTH);
		final Cipher cipher = this.cipher(FILE_CONTENT_CIPHER, key, countingIv.array(), Cipher.DECRYPT_MODE);

		// read content
		final InputStream in = new SeekableByteChannelInputStream(encryptedFile);
		final OutputStream cipheredOut = new CipherOutputStream(plaintextFile, cipher);
		return IOUtils.copyLarge(in, cipheredOut);
	}

	@Override
	public Long encryptFile(InputStream plaintextFile, SeekableByteChannel encryptedFile) throws IOException {
		// truncate file
		encryptedFile.truncate(0);

		// use an IV, whose last 4 bytes store an integer used in counter mode and write initial value to file.
		final ByteBuffer countingIv = ByteBuffer.wrap(randomData(AES_BLOCK_LENGTH));
		countingIv.putInt(AES_BLOCK_LENGTH - SIZE_OF_INT, 0);

		// derive secret key and generate cipher:
		final SecretKey key = this.pbkdf2(masterKey, EMPTY_SALT, PBKDF2_MASTERKEY_ITERATIONS, AES_KEY_LENGTH);
		final Cipher cipher = this.cipher(FILE_CONTENT_CIPHER, key, countingIv.array(), Cipher.ENCRYPT_MODE);

		// skip 8 bytes (reserved for file size):
		encryptedFile.position(SIZE_OF_LONG);

		// write iv:
		encryptedFile.write(countingIv);

		// write content:
		final OutputStream out = new SeekableByteChannelOutputStream(encryptedFile);
		final OutputStream cipheredOut = new CipherOutputStream(out, cipher);
		final Long actualSize = IOUtils.copyLarge(plaintextFile, cipheredOut);

		// write filesize
		final ByteBuffer actualSizeBuffer = ByteBuffer.allocate(SIZE_OF_LONG);
		actualSizeBuffer.putLong(actualSize);
		actualSizeBuffer.position(0);
		encryptedFile.position(0);
		encryptedFile.write(actualSizeBuffer);

		return actualSize;
	}

	@Override
	public Filter<Path> getPayloadFilesFilter() {
		return new Filter<Path>() {
			@Override
			public boolean accept(Path entry) throws IOException {
				return ENCRYPTED_FILE_GLOB_MATCHER.matches(entry);
			}
		};
	}
}

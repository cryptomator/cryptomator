/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.crypto.aes256;

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
import java.util.Random;
import java.util.UUID;
import java.util.zip.CRC32;

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
import org.cryptomator.crypto.AbstractCryptor;
import org.cryptomator.crypto.CryptorIOSupport;
import org.cryptomator.crypto.exceptions.DecryptFailedException;
import org.cryptomator.crypto.exceptions.UnsupportedKeyLengthException;
import org.cryptomator.crypto.exceptions.WrongPasswordException;
import org.cryptomator.crypto.io.SeekableByteChannelInputStream;
import org.cryptomator.crypto.io.SeekableByteChannelOutputStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Aes256Cryptor extends AbstractCryptor implements AesCryptographicConfiguration, FileNamingConventions {

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
	 * The decrypted master key. Its lifecycle starts with {@link #randomData(int)} or {@link #encryptMasterKey(Path, CharSequence)}. Its
	 * lifecycle ends with {@link #swipeSensitiveData()}.
	 */
	private final byte[] masterKey = new byte[MASTER_KEY_LENGTH];

	private static final int SIZE_OF_LONG = Long.BYTES;

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

	/**
	 * Creates a new Cryptor with a newly initialized PRNG.
	 */
	public Aes256Cryptor() {
		SECURE_PRNG.setSeed(SECURE_PRNG.generateSeed(PRNG_SEED_LENGTH));
		SECURE_PRNG.nextBytes(this.masterKey);
	}

	/**
	 * Creates a new Cryptor with the given PRNG.<br/>
	 * <strong>DO NOT USE IN PRODUCTION</strong>. This constructor must only be used in in unit tests. Do not change method visibility.
	 * 
	 * @param prng Fast, possibly insecure PRNG.
	 */
	Aes256Cryptor(Random prng) {
		prng.nextBytes(this.masterKey);
	}

	/**
	 * Encrypts the current masterKey with the given password and writes the result to the given output stream.
	 */
	@Override
	public void encryptMasterKey(OutputStream out, CharSequence password) throws IOException {
		try {
			// derive key:
			final byte[] userSalt = randomData(SALT_LENGTH);
			final SecretKey userKey = pbkdf2(password, userSalt, PBKDF2_PW_ITERATIONS, AES_KEY_LENGTH);

			// encrypt:
			final byte[] iv = randomData(AES_BLOCK_LENGTH);
			final Cipher encCipher = this.cipher(MASTERKEY_CIPHER, userKey, iv, Cipher.ENCRYPT_MODE);
			byte[] encryptedUserKey = encCipher.doFinal(userKey.getEncoded());
			byte[] encryptedMasterKey = encCipher.doFinal(this.masterKey);

			// save encrypted masterkey:
			final Key key = new Key();
			key.setIterations(PBKDF2_PW_ITERATIONS);
			key.setIv(iv);
			key.setKeyLength(AES_KEY_LENGTH);
			key.setMasterkey(encryptedMasterKey);
			key.setSalt(userSalt);
			key.setPwVerification(encryptedUserKey);
			objectMapper.writeValue(out, key);
		} catch (IllegalBlockSizeException | BadPaddingException ex) {
			throw new IllegalStateException("Block size hard coded. Padding irrelevant in ENCRYPT_MODE. IV must exist in CBC mode.", ex);
		}
	}

	/**
	 * Reads the encrypted masterkey from the given input stream and decrypts it with the given password.
	 * 
	 * @throws DecryptFailedException If the decryption failed for various reasons (including wrong password).
	 * @throws WrongPasswordException If the provided password was wrong. Note: Sometimes the algorithm itself fails due to a wrong
	 *             password. In this case a DecryptFailedException will be thrown.
	 * @throws UnsupportedKeyLengthException If the masterkey has been encrypted with a higher key length than supported by the system. In
	 *             this case Java JCE needs to be installed.
	 */
	@Override
	public void decryptMasterKey(InputStream in, CharSequence password) throws DecryptFailedException, WrongPasswordException, UnsupportedKeyLengthException, IOException {
		byte[] decrypted = new byte[0];
		try {
			// load encrypted masterkey:
			final Key key = objectMapper.readValue(in, Key.class);

			// check, whether the key length is supported:
			final int maxKeyLen = Cipher.getMaxAllowedKeyLength(CRYPTO_ALGORITHM);
			if (key.getKeyLength() > maxKeyLen) {
				throw new UnsupportedKeyLengthException(key.getKeyLength(), maxKeyLen);
			}

			// derive key:
			final SecretKey userKey = pbkdf2(password, key.getSalt(), key.getIterations(), key.getKeyLength());

			// check password:
			final Cipher encCipher = this.cipher(MASTERKEY_CIPHER, userKey, key.getIv(), Cipher.ENCRYPT_MODE);
			byte[] encryptedUserKey = encCipher.doFinal(userKey.getEncoded());
			if (!Arrays.equals(key.getPwVerification(), encryptedUserKey)) {
				throw new WrongPasswordException();
			}

			// decrypt:
			final Cipher decCipher = this.cipher(MASTERKEY_CIPHER, userKey, key.getIv(), Cipher.DECRYPT_MODE);
			decrypted = decCipher.doFinal(key.getMasterkey());

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
	public void swipeSensitiveDataInternal() {
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

	private long crc32Sum(byte[] source) {
		final CRC32 crc32 = new CRC32();
		crc32.update(source);
		return crc32.getValue();
	}

	@Override
	public String encryptPath(String cleartextPath, char encryptedPathSep, char cleartextPathSep, CryptorIOSupport ioSupport) {
		try {
			final SecretKey key = this.pbkdf2(masterKey, EMPTY_SALT, PBKDF2_MASTERKEY_ITERATIONS, AES_KEY_LENGTH);
			final String[] cleartextPathComps = StringUtils.split(cleartextPath, cleartextPathSep);
			final List<String> encryptedPathComps = new ArrayList<>(cleartextPathComps.length);
			for (final String cleartext : cleartextPathComps) {
				final String encrypted = encryptPathComponent(cleartext, key, ioSupport);
				encryptedPathComps.add(encrypted);
			}
			return StringUtils.join(encryptedPathComps, encryptedPathSep);
		} catch (IllegalBlockSizeException | BadPaddingException | IOException e) {
			throw new IllegalStateException("Unable to encrypt path: " + cleartextPath, e);
		}
	}

	/**
	 * Each path component, i.e. file or directory name separated by path separators, gets encrypted for its own.<br/>
	 * Encryption will blow up the filename length due to aes block sizes and base32 encoding. The result may be too long for some old file
	 * systems.<br/>
	 * This means that we need a workaround for filenames longer than the limit defined in
	 * {@link FileNamingConventions#ENCRYPTED_FILENAME_LENGTH_LIMIT}.<br/>
	 * <br/>
	 * In any case we will create the encrypted filename normally. For those, that are too long, we calculate a checksum. No
	 * cryptographically secure hash is needed here. We just want an uniform distribution for better load balancing. All encrypted filenames
	 * with the same checksum will then share a metadata file, in which a lookup map between encrypted filenames and short unique
	 * alternative names are stored.<br/>
	 * <br/>
	 * These alternative names consist of the checksum, a unique id and a special file extension defined in
	 * {@link FileNamingConventions#LONG_NAME_FILE_EXT}.
	 */
	private String encryptPathComponent(final String cleartext, final SecretKey key, CryptorIOSupport ioSupport) throws IllegalBlockSizeException, BadPaddingException, IOException {
		final Cipher cipher = this.cipher(FILE_NAME_CIPHER, key, EMPTY_IV, Cipher.ENCRYPT_MODE);
		final byte[] cleartextBytes = cleartext.getBytes(Charsets.UTF_8);
		final byte[] encryptedBytes = cipher.doFinal(cleartextBytes);
		final String encrypted = ENCRYPTED_FILENAME_CODEC.encodeAsString(encryptedBytes) + BASIC_FILE_EXT;

		if (encrypted.length() > ENCRYPTED_FILENAME_LENGTH_LIMIT) {
			final String crc32 = String.valueOf(crc32Sum(encrypted.getBytes()));
			final String metadataFilename = crc32 + METADATA_FILE_EXT;
			final LongFilenameMetadata metadata = this.getMetadata(ioSupport, metadataFilename);
			final String alternativeFileName = crc32 + LONG_NAME_PREFIX_SEPARATOR + metadata.getOrCreateUuidForEncryptedFilename(encrypted).toString() + LONG_NAME_FILE_EXT;
			this.storeMetadata(ioSupport, metadataFilename, metadata);
			return alternativeFileName;
		} else {
			return encrypted;
		}
	}

	@Override
	public String decryptPath(String encryptedPath, char encryptedPathSep, char cleartextPathSep, CryptorIOSupport ioSupport) {
		try {
			final SecretKey key = this.pbkdf2(masterKey, EMPTY_SALT, PBKDF2_MASTERKEY_ITERATIONS, AES_KEY_LENGTH);
			final String[] encryptedPathComps = StringUtils.split(encryptedPath, encryptedPathSep);
			final List<String> cleartextPathComps = new ArrayList<>(encryptedPathComps.length);
			for (final String encrypted : encryptedPathComps) {
				final String cleartext = decryptPathComponent(encrypted, key, ioSupport);
				cleartextPathComps.add(new String(cleartext));
			}
			return StringUtils.join(cleartextPathComps, cleartextPathSep);
		} catch (IllegalBlockSizeException | BadPaddingException | IOException e) {
			throw new IllegalStateException("Unable to decrypt path: " + encryptedPath, e);
		}
	}

	/**
	 * @see #encryptPathComponent(String, SecretKey, CryptorIOSupport)
	 */
	private String decryptPathComponent(final String encrypted, final SecretKey key, CryptorIOSupport ioSupport) throws IllegalBlockSizeException, BadPaddingException, IOException {
		final String ciphertext;
		if (encrypted.endsWith(LONG_NAME_FILE_EXT)) {
			final String basename = StringUtils.removeEnd(encrypted, LONG_NAME_FILE_EXT);
			final String crc32 = StringUtils.substringBefore(basename, LONG_NAME_PREFIX_SEPARATOR);
			final String uuid = StringUtils.substringAfter(basename, LONG_NAME_PREFIX_SEPARATOR);
			final String metadataFilename = crc32 + METADATA_FILE_EXT;
			final LongFilenameMetadata metadata = this.getMetadata(ioSupport, metadataFilename);
			ciphertext = metadata.getEncryptedFilenameForUUID(UUID.fromString(uuid));
		} else if (encrypted.endsWith(BASIC_FILE_EXT)) {
			ciphertext = StringUtils.removeEndIgnoreCase(encrypted, BASIC_FILE_EXT);
		} else {
			throw new IllegalArgumentException("Unsupported path component: " + encrypted);
		}

		final Cipher cipher = this.cipher(FILE_NAME_CIPHER, key, EMPTY_IV, Cipher.DECRYPT_MODE);
		final byte[] encryptedBytes = ENCRYPTED_FILENAME_CODEC.decode(ciphertext);
		final byte[] cleartextBytes = cipher.doFinal(encryptedBytes);
		return new String(cleartextBytes, Charsets.UTF_8);
	}

	private LongFilenameMetadata getMetadata(CryptorIOSupport ioSupport, String metadataFile) throws IOException {
		final byte[] fileContent = ioSupport.readPathSpecificMetadata(metadataFile);
		if (fileContent == null) {
			return new LongFilenameMetadata();
		} else {
			return objectMapper.readValue(fileContent, LongFilenameMetadata.class);
		}
	}

	private void storeMetadata(CryptorIOSupport ioSupport, String metadataFile, LongFilenameMetadata metadata) throws JsonProcessingException, IOException {
		ioSupport.writePathSpecificMetadata(metadataFile, objectMapper.writeValueAsBytes(metadata));
	}

	@Override
	public Long decryptedContentLength(SeekableByteChannel encryptedFile) throws IOException {
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
	public Long decryptRange(SeekableByteChannel encryptedFile, OutputStream plaintextFile, long pos, long length) throws IOException {
		// skip content size:
		encryptedFile.position(SIZE_OF_LONG);

		// read iv:
		final ByteBuffer countingIv = ByteBuffer.allocate(AES_BLOCK_LENGTH);
		final int read = encryptedFile.read(countingIv);
		if (read != AES_BLOCK_LENGTH) {
			throw new IOException("Failed to read encrypted file header.");
		}

		// seek relevant position and update iv:
		long firstRelevantBlock = pos / AES_BLOCK_LENGTH; // cut of fraction!
		long numberOfRelevantBlocks = 1 + length / AES_BLOCK_LENGTH;
		long numberOfRelevantBytes = numberOfRelevantBlocks * AES_BLOCK_LENGTH;
		long beginOfFirstRelevantBlock = firstRelevantBlock * AES_BLOCK_LENGTH;
		long offsetInsideFirstRelevantBlock = pos - beginOfFirstRelevantBlock;
		countingIv.putLong(AES_BLOCK_LENGTH - SIZE_OF_LONG, firstRelevantBlock);

		// fast forward stream:
		encryptedFile.position(SIZE_OF_LONG + AES_BLOCK_LENGTH + beginOfFirstRelevantBlock);

		// derive secret key and generate cipher:
		final SecretKey key = this.pbkdf2(masterKey, EMPTY_SALT, PBKDF2_MASTERKEY_ITERATIONS, AES_KEY_LENGTH);
		final Cipher cipher = this.cipher(FILE_CONTENT_CIPHER, key, countingIv.array(), Cipher.DECRYPT_MODE);

		// read content
		final InputStream in = new SeekableByteChannelInputStream(encryptedFile);
		final OutputStream rangedOut = new RangeFilterOutputStream(plaintextFile, offsetInsideFirstRelevantBlock, length);
		final OutputStream cipheredOut = new CipherOutputStream(rangedOut, cipher);
		return IOUtils.copyLarge(in, cipheredOut, 0, numberOfRelevantBytes);
	}

	@Override
	public Long encryptFile(InputStream plaintextFile, SeekableByteChannel encryptedFile) throws IOException {
		// truncate file
		encryptedFile.truncate(0);

		// use an IV, whose last 8 bytes store a long used in counter mode and write initial value to file.
		final ByteBuffer countingIv = ByteBuffer.wrap(randomData(AES_BLOCK_LENGTH));
		countingIv.putLong(AES_BLOCK_LENGTH - SIZE_OF_LONG, 0l);
		countingIv.position(0);

		// derive secret key and generate cipher:
		final SecretKey key = this.pbkdf2(masterKey, EMPTY_SALT, PBKDF2_MASTERKEY_ITERATIONS, AES_KEY_LENGTH);
		final Cipher cipher = this.cipher(FILE_CONTENT_CIPHER, key, countingIv.array(), Cipher.ENCRYPT_MODE);

		// 8 bytes (file size: temporarily -1):
		final ByteBuffer fileSize = ByteBuffer.allocate(SIZE_OF_LONG);
		fileSize.putLong(-1L);
		fileSize.position(0);
		encryptedFile.write(fileSize);

		// 16 bytes (iv):
		encryptedFile.write(countingIv);

		// write content:
		final OutputStream out = new SeekableByteChannelOutputStream(encryptedFile);
		final OutputStream cipheredOut = new CipherOutputStream(out, cipher);
		final Long actualSize = IOUtils.copyLarge(plaintextFile, cipheredOut);

		// write filesize
		fileSize.position(0);
		fileSize.putLong(actualSize);
		fileSize.position(0);
		encryptedFile.position(0);
		encryptedFile.write(fileSize);

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

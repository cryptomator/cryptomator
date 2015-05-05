/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.crypto.aes256;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.crypto.generators.SCrypt;
import org.cryptomator.crypto.Cryptor;
import org.cryptomator.crypto.CryptorMetadataSupport;
import org.cryptomator.crypto.aes256.CounterAwareInputStream.CounterAwareInputLimitReachedException;
import org.cryptomator.crypto.exceptions.CounterOverflowException;
import org.cryptomator.crypto.exceptions.DecryptFailedException;
import org.cryptomator.crypto.exceptions.EncryptFailedException;
import org.cryptomator.crypto.exceptions.MacAuthenticationFailedException;
import org.cryptomator.crypto.exceptions.UnsupportedKeyLengthException;
import org.cryptomator.crypto.exceptions.UnsupportedVaultException;
import org.cryptomator.crypto.exceptions.WrongPasswordException;
import org.cryptomator.crypto.io.SeekableByteChannelInputStream;
import org.cryptomator.crypto.io.SeekableByteChannelOutputStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Aes256Cryptor implements Cryptor, AesCryptographicConfiguration, FileNamingConventions {

	/**
	 * Defined in static initializer. Defaults to 256, but falls back to maximum value possible, if JCE Unlimited Strength Jurisdiction Policy Files isn't installed. Those files can be downloaded
	 * here: http://www.oracle.com/technetwork/java/javase/downloads/.
	 */
	private static final int AES_KEY_LENGTH_IN_BITS;

	/**
	 * PRNG for cryptographically secure random numbers. Defaults to SHA1-based number generator.
	 * 
	 * @see http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#SecureRandom
	 */
	private final SecureRandom securePrng;

	/**
	 * Jackson JSON-Mapper.
	 */
	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * The decrypted master key. Its lifecycle starts with the construction of an Aes256Cryptor instance or {@link #decryptMasterKey(InputStream, CharSequence)}. Its lifecycle ends with
	 * {@link #swipeSensitiveData()}.
	 */
	private SecretKey primaryMasterKey;

	/**
	 * Decrypted secondary key used for hmac operations.
	 */
	private SecretKey hMacMasterKey;

	static {
		try {
			final int maxKeyLength = Cipher.getMaxAllowedKeyLength(AES_KEY_ALGORITHM);
			AES_KEY_LENGTH_IN_BITS = (maxKeyLength >= PREF_MASTER_KEY_LENGTH_IN_BITS) ? PREF_MASTER_KEY_LENGTH_IN_BITS : maxKeyLength;
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Algorithm should exist.", e);
		}
	}

	/**
	 * Creates a new Cryptor with a newly initialized PRNG.
	 */
	public Aes256Cryptor() {
		byte[] bytes = new byte[AES_KEY_LENGTH_IN_BITS / Byte.SIZE];
		try {
			securePrng = SecureRandom.getInstance(PRNG_ALGORITHM);
			securePrng.setSeed(securePrng.generateSeed(PRNG_SEED_LENGTH));
			securePrng.nextBytes(bytes);
			this.primaryMasterKey = new SecretKeySpec(bytes, AES_KEY_ALGORITHM);
			securePrng.nextBytes(bytes);
			this.hMacMasterKey = new SecretKeySpec(bytes, HMAC_KEY_ALGORITHM);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("PRNG algorithm should exist.", e);
		} finally {
			Arrays.fill(bytes, (byte) 0);
		}
	}

	/**
	 * Encrypts the current masterKey with the given password and writes the result to the given output stream.
	 */
	@Override
	public void encryptMasterKey(OutputStream out, CharSequence password) throws IOException {
		try {
			// derive key:
			final byte[] kekSalt = randomData(SCRYPT_SALT_LENGTH);
			final SecretKey kek = scrypt(password, kekSalt, SCRYPT_COST_PARAM, SCRYPT_BLOCK_SIZE, AES_KEY_LENGTH_IN_BITS);

			// encrypt:
			final Cipher encCipher = aesKeyWrapCipher(kek, Cipher.WRAP_MODE);
			byte[] wrappedPrimaryKey = encCipher.wrap(primaryMasterKey);
			byte[] wrappedSecondaryKey = encCipher.wrap(hMacMasterKey);

			// save encrypted masterkey:
			final KeyFile keyfile = new KeyFile();
			keyfile.setVersion(KeyFile.CURRENT_VERSION);
			keyfile.setScryptSalt(kekSalt);
			keyfile.setScryptCostParam(SCRYPT_COST_PARAM);
			keyfile.setScryptBlockSize(SCRYPT_BLOCK_SIZE);
			keyfile.setKeyLength(AES_KEY_LENGTH_IN_BITS);
			keyfile.setPrimaryMasterKey(wrappedPrimaryKey);
			keyfile.setHMacMasterKey(wrappedSecondaryKey);
			objectMapper.writeValue(out, keyfile);
		} catch (InvalidKeyException | IllegalBlockSizeException ex) {
			throw new IllegalStateException("Invalid hard coded configuration.", ex);
		}
	}

	/**
	 * Reads the encrypted masterkey from the given input stream and decrypts it with the given password.
	 * 
	 * @throws DecryptFailedException If the decryption failed for various reasons (including wrong password).
	 * @throws WrongPasswordException If the provided password was wrong. Note: Sometimes the algorithm itself fails due to a wrong password. In this case a DecryptFailedException will be thrown.
	 * @throws UnsupportedKeyLengthException If the masterkey has been encrypted with a higher key length than supported by the system. In this case Java JCE needs to be installed.
	 * @throws UnsupportedVaultException If the masterkey file is too old or too modern.
	 */
	@Override
	public void decryptMasterKey(InputStream in, CharSequence password) throws DecryptFailedException, WrongPasswordException, UnsupportedKeyLengthException, IOException, UnsupportedVaultException {
		try {
			// load encrypted masterkey:
			final KeyFile keyfile = objectMapper.readValue(in, KeyFile.class);

			// check version
			if (keyfile.getVersion() != KeyFile.CURRENT_VERSION) {
				throw new UnsupportedVaultException(keyfile.getVersion(), KeyFile.CURRENT_VERSION);
			}

			// check, whether the key length is supported:
			final int maxKeyLen = Cipher.getMaxAllowedKeyLength(AES_KEY_ALGORITHM);
			if (keyfile.getKeyLength() > maxKeyLen) {
				throw new UnsupportedKeyLengthException(keyfile.getKeyLength(), maxKeyLen);
			}

			// derive key:
			final SecretKey kek = scrypt(password, keyfile.getScryptSalt(), keyfile.getScryptCostParam(), keyfile.getScryptBlockSize(), AES_KEY_LENGTH_IN_BITS);

			// decrypt and check password by catching AEAD exception
			final Cipher decCipher = aesKeyWrapCipher(kek, Cipher.UNWRAP_MODE);
			SecretKey primary = (SecretKey) decCipher.unwrap(keyfile.getPrimaryMasterKey(), AES_KEY_ALGORITHM, Cipher.SECRET_KEY);
			SecretKey secondary = (SecretKey) decCipher.unwrap(keyfile.getHMacMasterKey(), HMAC_KEY_ALGORITHM, Cipher.SECRET_KEY);

			// everything ok, assign decrypted keys:
			this.primaryMasterKey = primary;
			this.hMacMasterKey = secondary;
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("Algorithm should exist.", ex);
		} catch (InvalidKeyException e) {
			throw new WrongPasswordException();
		}
	}

	@Override
	public boolean isDestroyed() {
		return primaryMasterKey.isDestroyed() && hMacMasterKey.isDestroyed();
	}

	@Override
	public void destroy() {
		destroyQuietly(primaryMasterKey);
		destroyQuietly(hMacMasterKey);
	}

	private void destroyQuietly(Destroyable d) {
		try {
			d.destroy();
		} catch (DestroyFailedException e) {
			// ignore
		}
	}

	private Cipher aesKeyWrapCipher(SecretKey key, int cipherMode) {
		try {
			final Cipher cipher = Cipher.getInstance(AES_KEYWRAP_CIPHER);
			cipher.init(cipherMode, key);
			return cipher;
		} catch (InvalidKeyException ex) {
			throw new IllegalArgumentException("Invalid key.", ex);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
			throw new IllegalStateException("Algorithm/Padding should exist and accept GCM specs.", ex);
		}
	}

	private Cipher aesCtrCipher(SecretKey key, byte[] iv, int cipherMode) {
		try {
			final Cipher cipher = Cipher.getInstance(AES_CTR_CIPHER);
			cipher.init(cipherMode, key, new IvParameterSpec(iv));
			return cipher;
		} catch (InvalidKeyException ex) {
			throw new IllegalArgumentException("Invalid key.", ex);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException ex) {
			throw new IllegalStateException("Algorithm/Padding should exist and accept an IV.", ex);
		}
	}

	private Cipher aesCbcCipher(SecretKey key, byte[] iv, int cipherMode) {
		try {
			final Cipher cipher = Cipher.getInstance(AES_CBC_CIPHER);
			cipher.init(cipherMode, key, new IvParameterSpec(iv));
			return cipher;
		} catch (InvalidKeyException ex) {
			throw new IllegalArgumentException("Invalid key.", ex);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException ex) {
			throw new AssertionError("Every implementation of the Java platform is required to support AES/CBC/PKCS5Padding, which accepts an IV", ex);
		}
	}

	private Mac hmacSha256(SecretKey key) {
		try {
			final Mac mac = Mac.getInstance(HMAC_KEY_ALGORITHM);
			mac.init(key);
			return mac;
		} catch (NoSuchAlgorithmException e) {
			throw new AssertionError("Every implementation of the Java platform is required to support HmacSHA256.", e);
		} catch (InvalidKeyException e) {
			throw new IllegalArgumentException("Invalid key", e);
		}
	}

	private MessageDigest sha256() {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new AssertionError("Every implementation of the Java platform is required to support Sha-256");
		}
	}

	private byte[] randomData(int length) {
		final byte[] result = new byte[length];
		securePrng.nextBytes(result);
		return result;
	}

	private SecretKey scrypt(CharSequence password, byte[] salt, int costParam, int blockSize, int keyLengthInBits) {
		// use sb, as password.toString's implementation is unknown
		final StringBuilder sb = new StringBuilder(password);
		final byte[] pw = sb.toString().getBytes();
		try {
			final byte[] key = SCrypt.generate(pw, salt, costParam, blockSize, 1, keyLengthInBits / Byte.SIZE);
			return new SecretKeySpec(key, AES_KEY_ALGORITHM);
		} finally {
			// destroy copied bytes of the plaintext password:
			Arrays.fill(pw, (byte) 0);
			for (int i = 0; i < password.length(); i++) {
				sb.setCharAt(i, (char) 0);
			}
		}
	}

	@Override
	public String encryptDirectoryPath(String cleartextPath, String nativePathSep) {
		final byte[] cleartextBytes = cleartextPath.getBytes(StandardCharsets.UTF_8);
		byte[] encryptedBytes = AesSivCipherUtil.sivEncrypt(primaryMasterKey, hMacMasterKey, cleartextBytes);
		final byte[] hashed = sha256().digest(encryptedBytes);
		final String encryptedThenHashedPath = ENCRYPTED_FILENAME_CODEC.encodeAsString(hashed);
		return encryptedThenHashedPath.substring(0, 2) + nativePathSep + encryptedThenHashedPath.substring(2);
	}

	/**
	 * Each path component, i.e. file or directory name separated by path separators, gets encrypted for its own.<br/>
	 * Encryption will blow up the filename length due to aes block sizes, IVs and base32 encoding. The result may be too long for some old file systems.<br/>
	 * This means that we need a workaround for filenames longer than the limit defined in {@link FileNamingConventions#ENCRYPTED_FILENAME_LENGTH_LIMIT}.<br/>
	 * <br/>
	 * In any case we will create the encrypted filename normally. For those, that are too long, we calculate a checksum. No cryptographically secure hash is needed here. We just want an uniform
	 * distribution for better load balancing. All encrypted filenames with the same checksum will then share a metadata file, in which a lookup map between encrypted filenames and short unique
	 * alternative names are stored.<br/>
	 * <br/>
	 * These alternative names consist of the checksum, a unique id and a special file extension defined in {@link FileNamingConventions#LONG_NAME_FILE_EXT}.
	 */
	@Override
	public String encryptFilename(String cleartextName, CryptorMetadataSupport ioSupport) throws IOException {
		final byte[] cleartextBytes = cleartextName.getBytes(StandardCharsets.UTF_8);

		// encrypt:
		final byte[] encryptedBytes = AesSivCipherUtil.sivEncrypt(primaryMasterKey, hMacMasterKey, cleartextBytes);
		final String ivAndCiphertext = ENCRYPTED_FILENAME_CODEC.encodeAsString(encryptedBytes);

		if (ivAndCiphertext.length() + BASIC_FILE_EXT.length() > ENCRYPTED_FILENAME_LENGTH_LIMIT) {
			final String metadataGroup = ivAndCiphertext.substring(0, LONG_NAME_PREFIX_LENGTH);
			final LongFilenameMetadata metadata = this.getMetadata(ioSupport, metadataGroup);
			final String alternativeFileName = metadataGroup + metadata.getOrCreateUuidForEncryptedFilename(ivAndCiphertext).toString() + LONG_NAME_FILE_EXT;
			this.storeMetadata(ioSupport, metadataGroup, metadata);
			return alternativeFileName;
		} else {
			return ivAndCiphertext + BASIC_FILE_EXT;
		}
	}

	@Override
	public String decryptFilename(String ciphertextName, CryptorMetadataSupport ioSupport) throws DecryptFailedException, IOException {
		final String ciphertext;
		if (ciphertextName.endsWith(LONG_NAME_FILE_EXT)) {
			final String basename = StringUtils.removeEnd(ciphertextName, LONG_NAME_FILE_EXT);
			final String metadataGroup = basename.substring(0, LONG_NAME_PREFIX_LENGTH);
			final String uuid = basename.substring(LONG_NAME_PREFIX_LENGTH);
			final LongFilenameMetadata metadata = this.getMetadata(ioSupport, metadataGroup);
			ciphertext = metadata.getEncryptedFilenameForUUID(UUID.fromString(uuid));
		} else if (ciphertextName.endsWith(BASIC_FILE_EXT)) {
			ciphertext = StringUtils.removeEndIgnoreCase(ciphertextName, BASIC_FILE_EXT);
		} else {
			throw new IllegalArgumentException("Unsupported path component: " + ciphertextName);
		}

		// decrypt:
		final byte[] encryptedBytes = ENCRYPTED_FILENAME_CODEC.decode(ciphertext);
		final byte[] cleartextBytes = AesSivCipherUtil.sivDecrypt(primaryMasterKey, hMacMasterKey, encryptedBytes);

		return new String(cleartextBytes, StandardCharsets.UTF_8);
	}

	private LongFilenameMetadata getMetadata(CryptorMetadataSupport ioSupport, String metadataGroup) throws IOException {
		final byte[] fileContent = ioSupport.readMetadata(metadataGroup);
		if (fileContent == null) {
			return new LongFilenameMetadata();
		} else {
			return objectMapper.readValue(fileContent, LongFilenameMetadata.class);
		}
	}

	private void storeMetadata(CryptorMetadataSupport ioSupport, String metadataGroup, LongFilenameMetadata metadata) throws JsonProcessingException, IOException {
		ioSupport.writeMetadata(metadataGroup, objectMapper.writeValueAsBytes(metadata));
	}

	@Override
	public Long decryptedContentLength(SeekableByteChannel encryptedFile) throws IOException, MacAuthenticationFailedException {
		// read header:
		encryptedFile.position(0);
		final ByteBuffer headerBuf = ByteBuffer.allocate(64);
		final int headerBytesRead = encryptedFile.read(headerBuf);
		if (headerBytesRead != headerBuf.capacity()) {
			return null;
		}

		// read iv:
		final byte[] iv = new byte[AES_BLOCK_LENGTH];
		headerBuf.position(0);
		headerBuf.get(iv);

		// read content length:
		final byte[] encryptedContentLengthBytes = new byte[AES_BLOCK_LENGTH];
		headerBuf.position(16);
		headerBuf.get(encryptedContentLengthBytes);
		final Long fileSize = decryptContentLength(encryptedContentLengthBytes, iv);

		// read stored header mac:
		final byte[] storedHeaderMac = new byte[32];
		headerBuf.position(32);
		headerBuf.get(storedHeaderMac);

		// calculate mac over first 32 bytes of header:
		final Mac headerMac = this.hmacSha256(hMacMasterKey);
		headerBuf.rewind();
		headerBuf.limit(32);
		headerMac.update(headerBuf);

		final boolean macMatches = MessageDigest.isEqual(storedHeaderMac, headerMac.doFinal());
		if (!macMatches) {
			throw new MacAuthenticationFailedException("MAC authentication failed.");
		}

		return fileSize;
	}

	private long decryptContentLength(byte[] encryptedContentLengthBytes, byte[] iv) {
		try {
			final Cipher sizeCipher = aesCbcCipher(primaryMasterKey, iv, Cipher.DECRYPT_MODE);
			final byte[] decryptedFileSize = sizeCipher.doFinal(encryptedContentLengthBytes);
			final ByteBuffer fileSizeBuffer = ByteBuffer.wrap(decryptedFileSize);
			return fileSizeBuffer.getLong();
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			throw new IllegalStateException(e);
		}
	}

	private byte[] encryptContentLength(long contentLength, byte[] iv) {
		try {
			final ByteBuffer fileSizeBuffer = ByteBuffer.allocate(Long.BYTES);
			fileSizeBuffer.putLong(contentLength);
			final Cipher sizeCipher = aesCbcCipher(primaryMasterKey, iv, Cipher.ENCRYPT_MODE);
			return sizeCipher.doFinal(fileSizeBuffer.array());
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			throw new IllegalStateException("Block size must be valid, as padding is requested. BadPaddingException not possible in encrypt mode.", e);
		}
	}

	@Override
	public boolean isAuthentic(SeekableByteChannel encryptedFile) throws IOException {
		// read header:
		encryptedFile.position(0l);
		final ByteBuffer headerBuf = ByteBuffer.allocate(96);
		final int headerBytesRead = encryptedFile.read(headerBuf);
		if (headerBytesRead != headerBuf.capacity()) {
			throw new IOException("Failed to read file header.");
		}

		// read header mac:
		final byte[] storedHeaderMac = new byte[32];
		headerBuf.position(32);
		headerBuf.get(storedHeaderMac);

		// read content mac:
		final byte[] storedContentMac = new byte[32];
		headerBuf.position(64);
		headerBuf.get(storedContentMac);

		// calculate mac over first 32 bytes of header:
		final Mac headerMac = this.hmacSha256(hMacMasterKey);
		headerBuf.position(0);
		headerBuf.limit(32);
		headerMac.update(headerBuf);

		// calculate mac over content:
		encryptedFile.position(96l);
		final Mac contentMac = this.hmacSha256(hMacMasterKey);
		final InputStream in = new SeekableByteChannelInputStream(encryptedFile);
		final InputStream macIn = new MacInputStream(in, contentMac);
		IOUtils.copyLarge(macIn, new NullOutputStream());

		// compare (in constant time):
		final boolean headerMacMatches = MessageDigest.isEqual(storedHeaderMac, headerMac.doFinal());
		final boolean contentMacMatches = MessageDigest.isEqual(storedContentMac, contentMac.doFinal());
		return headerMacMatches && contentMacMatches;
	}

	@Override
	public Long decryptFile(SeekableByteChannel encryptedFile, OutputStream plaintextFile) throws IOException, DecryptFailedException {
		// read header:
		encryptedFile.position(0l);
		final ByteBuffer headerBuf = ByteBuffer.allocate(96);
		final int headerBytesRead = encryptedFile.read(headerBuf);
		if (headerBytesRead != headerBuf.capacity()) {
			throw new IOException("Failed to read file header.");
		}

		// read iv:
		final byte[] iv = new byte[AES_BLOCK_LENGTH];
		headerBuf.position(0);
		headerBuf.get(iv);

		// read content length:
		final byte[] encryptedContentLengthBytes = new byte[AES_BLOCK_LENGTH];
		headerBuf.position(16);
		headerBuf.get(encryptedContentLengthBytes);
		final Long fileSize = decryptContentLength(encryptedContentLengthBytes, iv);

		// read header mac:
		final byte[] headerMac = new byte[32];
		headerBuf.position(32);
		headerBuf.get(headerMac);

		// read content mac:
		final byte[] contentMac = new byte[32];
		headerBuf.position(64);
		headerBuf.get(contentMac);

		// decrypt content
		encryptedFile.position(96l);
		final Mac calculatedContentMac = this.hmacSha256(hMacMasterKey);
		final Cipher cipher = this.aesCtrCipher(primaryMasterKey, iv, Cipher.DECRYPT_MODE);
		final InputStream in = new SeekableByteChannelInputStream(encryptedFile);
		final InputStream macIn = new MacInputStream(in, calculatedContentMac);
		final InputStream cipheredIn = new CipherInputStream(macIn, cipher);
		final long bytesDecrypted = IOUtils.copyLarge(cipheredIn, plaintextFile, 0, fileSize);

		// drain remaining bytes to /dev/null to complete MAC calculation:
		IOUtils.copyLarge(macIn, new NullOutputStream());

		// compare (in constant time):
		final boolean macMatches = MessageDigest.isEqual(contentMac, calculatedContentMac.doFinal());
		if (!macMatches) {
			// This exception will be thrown AFTER we sent the decrypted content to the user.
			// This has two advantages:
			// - we don't need to read files twice
			// - we can still restore files suffering from non-malicious bit rotting
			// Anyway me MUST make sure to warn the user. This will be done by the UI when catching this exception.
			throw new MacAuthenticationFailedException("MAC authentication failed.");
		}

		return bytesDecrypted;
	}

	@Override
	public Long decryptRange(SeekableByteChannel encryptedFile, OutputStream plaintextFile, long pos, long length) throws IOException, DecryptFailedException {
		// read iv:
		encryptedFile.position(0l);
		final ByteBuffer countingIv = ByteBuffer.allocate(AES_BLOCK_LENGTH);
		final int numIvBytesRead = encryptedFile.read(countingIv);

		// check validity of header:
		if (numIvBytesRead != AES_BLOCK_LENGTH) {
			throw new IOException("Failed to read file header.");
		}

		// seek relevant position and update iv:
		long firstRelevantBlock = pos / AES_BLOCK_LENGTH; // cut of fraction!
		long beginOfFirstRelevantBlock = firstRelevantBlock * AES_BLOCK_LENGTH;
		long offsetInsideFirstRelevantBlock = pos - beginOfFirstRelevantBlock;
		countingIv.putInt(AES_BLOCK_LENGTH - Integer.BYTES, (int) firstRelevantBlock); // int-cast is possible, as max file size is 64GiB

		// fast forward stream:
		encryptedFile.position(96l + beginOfFirstRelevantBlock);

		// generate cipher:
		final Cipher cipher = this.aesCtrCipher(primaryMasterKey, countingIv.array(), Cipher.DECRYPT_MODE);

		// read content
		final InputStream in = new SeekableByteChannelInputStream(encryptedFile);
		final InputStream cipheredIn = new CipherInputStream(in, cipher);
		return IOUtils.copyLarge(cipheredIn, plaintextFile, offsetInsideFirstRelevantBlock, length);
	}

	/**
	 * header = {16 byte iv, 16 byte filesize, 32 byte headerMac, 32 byte contentMac}
	 */
	@Override
	public Long encryptFile(InputStream plaintextFile, SeekableByteChannel encryptedFile) throws IOException, EncryptFailedException {
		// truncate file
		encryptedFile.truncate(0l);

		// use an IV, whose last 8 bytes store a long used in counter mode and write initial value to file.
		final ByteBuffer ivBuf = ByteBuffer.wrap(randomData(AES_BLOCK_LENGTH));
		ivBuf.putInt(AES_BLOCK_LENGTH - Integer.BYTES, 0);
		final byte[] iv = ivBuf.array();

		// 96 byte header buffer (16 IV, 16 size, 32 headerMac, 32 contentMac)
		// prefilled with "zero" content length for impatient processes, which want to know the size, before file has been completely written:
		final ByteBuffer headerBuf = ByteBuffer.allocate(96);
		headerBuf.position(16);
		headerBuf.put(encryptContentLength(0l, iv));
		headerBuf.flip();
		headerBuf.limit(96);
		encryptedFile.write(headerBuf);

		// content encryption:
		final Cipher cipher = this.aesCtrCipher(primaryMasterKey, iv, Cipher.ENCRYPT_MODE);
		final Mac contentMac = this.hmacSha256(hMacMasterKey);
		final OutputStream out = new SeekableByteChannelOutputStream(encryptedFile);
		final OutputStream macOut = new MacOutputStream(out, contentMac);
		final OutputStream cipheredOut = new CipherOutputStream(macOut, cipher);
		final OutputStream blockSizeBufferedOut = new BufferedOutputStream(cipheredOut, AES_BLOCK_LENGTH);
		final InputStream lengthLimitingIn = new CounterAwareInputStream(plaintextFile);
		final Long plaintextSize;
		try {
			plaintextSize = IOUtils.copyLarge(lengthLimitingIn, blockSizeBufferedOut);
		} catch (CounterAwareInputLimitReachedException ex) {
			encryptedFile.truncate(0l);
			throw new CounterOverflowException("File size exceeds limit (64Gib). Aborting to prevent counter overflow.");
		}

		// add random length padding to obfuscate file length:
		final long numberOfPlaintextBlocks = (int) Math.ceil(plaintextSize / AES_BLOCK_LENGTH);
		final long minAdditionalBlocks = 4;
		final long maxAdditionalBlocks = Math.min(numberOfPlaintextBlocks >> 3, 1024 * 1024); // 12,5% of original blocks, but not more than 1M blocks (16MiBs)
		final long availableBlocks = (1l << 32) - numberOfPlaintextBlocks; // before reaching limit of 2^32 blocks
		final long additionalBlocks = (long) Math.min(Math.random() * Math.max(minAdditionalBlocks, maxAdditionalBlocks), availableBlocks);
		final byte[] randomPadding = this.randomData(AES_BLOCK_LENGTH);
		for (int i = 0; i < additionalBlocks; i += AES_BLOCK_LENGTH) {
			blockSizeBufferedOut.write(randomPadding);
		}
		blockSizeBufferedOut.flush();

		// create and write header:
		headerBuf.clear();
		headerBuf.put(iv);
		headerBuf.put(encryptContentLength(plaintextSize, iv));
		headerBuf.flip();
		final Mac headerMac = this.hmacSha256(hMacMasterKey);
		headerMac.update(headerBuf);
		headerBuf.limit(96);
		headerBuf.put(headerMac.doFinal());
		headerBuf.put(contentMac.doFinal());
		headerBuf.flip();
		encryptedFile.position(0);
		encryptedFile.write(headerBuf);

		return plaintextSize;
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

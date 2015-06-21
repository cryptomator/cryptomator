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
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.crypto.generators.SCrypt;
import org.cryptomator.crypto.Cryptor;
import org.cryptomator.crypto.exceptions.DecryptFailedException;
import org.cryptomator.crypto.exceptions.EncryptFailedException;
import org.cryptomator.crypto.exceptions.MacAuthenticationFailedException;
import org.cryptomator.crypto.exceptions.UnsupportedKeyLengthException;
import org.cryptomator.crypto.exceptions.UnsupportedVaultException;
import org.cryptomator.crypto.exceptions.WrongPasswordException;
import org.cryptomator.crypto.io.SeekableByteChannelInputStream;
import org.cryptomator.crypto.io.SeekableByteChannelOutputStream;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Aes256Cryptor implements Cryptor, AesCryptographicConfiguration {

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
			final SecretKey kek = scrypt(password, keyfile.getScryptSalt(), keyfile.getScryptCostParam(), keyfile.getScryptBlockSize(), keyfile.getKeyLength());

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
			throw new IllegalStateException("Algorithm/Padding should exist.", ex);
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
	public String encryptDirectoryPath(String cleartextDirectoryId, String nativePathSep) {
		final byte[] cleartextBytes = cleartextDirectoryId.getBytes(StandardCharsets.UTF_8);
		byte[] encryptedBytes = AesSivCipherUtil.sivEncrypt(primaryMasterKey, hMacMasterKey, cleartextBytes);
		final byte[] hashed = sha256().digest(encryptedBytes);
		final String encryptedThenHashedPath = ENCRYPTED_FILENAME_CODEC.encodeAsString(hashed);
		return encryptedThenHashedPath.substring(0, 2) + nativePathSep + encryptedThenHashedPath.substring(2);
	}

	@Override
	public String encryptFilename(String cleartextName) {
		final byte[] cleartextBytes = cleartextName.getBytes(StandardCharsets.UTF_8);
		final byte[] encryptedBytes = AesSivCipherUtil.sivEncrypt(primaryMasterKey, hMacMasterKey, cleartextBytes);
		return ENCRYPTED_FILENAME_CODEC.encodeAsString(encryptedBytes);
	}

	@Override
	public String decryptFilename(String ciphertextName) throws DecryptFailedException {
		final byte[] encryptedBytes = ENCRYPTED_FILENAME_CODEC.decode(ciphertextName);
		final byte[] cleartextBytes = AesSivCipherUtil.sivDecrypt(primaryMasterKey, hMacMasterKey, encryptedBytes);
		return new String(cleartextBytes, StandardCharsets.UTF_8);
	}

	@Override
	public Long decryptedContentLength(SeekableByteChannel encryptedFile) throws IOException, MacAuthenticationFailedException {
		// read header:
		encryptedFile.position(0);
		final ByteBuffer headerBuf = ByteBuffer.allocate(96);
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

		// read stored header mac:
		final byte[] storedHeaderMac = new byte[32];
		headerBuf.position(64);
		headerBuf.get(storedHeaderMac);

		// calculate mac over first 64 bytes of header:
		final Mac headerMac = this.hmacSha256(hMacMasterKey);
		headerBuf.rewind();
		headerBuf.limit(64);
		headerMac.update(headerBuf);

		final boolean macMatches = MessageDigest.isEqual(storedHeaderMac, headerMac.doFinal());
		if (!macMatches) {
			throw new MacAuthenticationFailedException("MAC authentication failed.");
		}

		final byte[] decryptedContentLengthBytes = decryptHeaderData(encryptedContentLengthBytes, iv);
		final ByteBuffer fileSizeBuffer = ByteBuffer.wrap(decryptedContentLengthBytes);
		return fileSizeBuffer.getLong();
	}

	private byte[] decryptHeaderData(byte[] ciphertextBytes, byte[] iv) {
		try {
			final Cipher sizeCipher = aesCbcCipher(primaryMasterKey, iv, Cipher.DECRYPT_MODE);
			return sizeCipher.doFinal(ciphertextBytes);
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			throw new IllegalStateException(e);
		}
	}

	private byte[] encryptHeaderData(byte[] plaintextBytes, byte[] iv) {
		try {
			final Cipher sizeCipher = aesCbcCipher(primaryMasterKey, iv, Cipher.ENCRYPT_MODE);
			return sizeCipher.doFinal(plaintextBytes);
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			throw new IllegalStateException("Block size must be valid, as padding is requested. BadPaddingException not possible in encrypt mode.", e);
		}
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

		// derive nonce used in counter mode from IV by setting last 64bit to 0:
		final ByteBuffer nonceBuf = ByteBuffer.wrap(iv.clone());
		nonceBuf.putLong(AES_BLOCK_LENGTH - Long.BYTES, 0);
		final byte[] nonce = nonceBuf.array();

		// read content length:
		final byte[] encryptedContentLengthBytes = new byte[AES_BLOCK_LENGTH];
		headerBuf.position(16);
		headerBuf.get(encryptedContentLengthBytes);
		final byte[] decryptedContentLengthBytes = decryptHeaderData(encryptedContentLengthBytes, iv);
		final ByteBuffer fileSizeBuffer = ByteBuffer.wrap(decryptedContentLengthBytes);
		final Long fileSize = fileSizeBuffer.getLong();

		// read content key:
		final byte[] encryptedContentKeyBytes = new byte[32];
		headerBuf.position(32);
		headerBuf.get(encryptedContentKeyBytes);
		final byte[] contentKeyBytes = decryptHeaderData(encryptedContentKeyBytes, iv);

		// read header mac:
		final byte[] storedHeaderMac = new byte[32];
		headerBuf.position(64);
		headerBuf.get(storedHeaderMac);

		// calculate mac over first 64 bytes of header:
		final Mac headerMac = this.hmacSha256(hMacMasterKey);
		headerBuf.position(0);
		headerBuf.limit(64);
		headerMac.update(headerBuf);

		// check header integrity:
		if (!MessageDigest.isEqual(storedHeaderMac, headerMac.doFinal())) {
			throw new MacAuthenticationFailedException("Header MAC authentication failed.");
		}

		// content decryption:
		encryptedFile.position(96l);
		final SecretKey contentKey = new SecretKeySpec(contentKeyBytes, AES_KEY_ALGORITHM);
		final Cipher cipher = this.aesCtrCipher(contentKey, nonce, Cipher.DECRYPT_MODE);
		final Mac contentMac = this.hmacSha256(hMacMasterKey);

		// reading ciphered input and MACs interleaved:
		long bytesDecrypted = 0;
		final InputStream in = new SeekableByteChannelInputStream(encryptedFile);
		byte[] buffer = new byte[CONTENT_MAC_BLOCK + 32];
		int n = 0;
		while ((n = IOUtils.read(in, buffer)) > 0) {
			if (n < 32) {
				throw new DecryptFailedException("Invalid file content, missing MAC.");
			}

			// check MAC of current block:
			contentMac.update(buffer, 0, n - 32);
			final byte[] calculatedMac = contentMac.doFinal();
			final byte[] storedMac = new byte[32];
			System.arraycopy(buffer, n - 32, storedMac, 0, 32);
			if (!MessageDigest.isEqual(calculatedMac, storedMac)) {
				throw new MacAuthenticationFailedException("Content MAC authentication failed.");
			}

			// decrypt block:
			final byte[] plaintext = cipher.update(buffer, 0, n - 32);
			final int plaintextLengthWithoutPadding = (int) Math.min(plaintext.length, fileSize - bytesDecrypted); // plaintext.length is known to be a 32 bit int
			plaintextFile.write(plaintext, 0, plaintextLengthWithoutPadding);
			bytesDecrypted += plaintextLengthWithoutPadding;
		}
		destroyQuietly(contentKey);

		return bytesDecrypted;
	}

	@Override
	public Long decryptRange(SeekableByteChannel encryptedFile, OutputStream plaintextFile, long pos, long length) throws IOException, DecryptFailedException {
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

		// read content key:
		final byte[] encryptedContentKeyBytes = new byte[32];
		headerBuf.position(32);
		headerBuf.get(encryptedContentKeyBytes);
		final byte[] contentKeyBytes = decryptHeaderData(encryptedContentKeyBytes, iv);

		// read header mac:
		final byte[] storedHeaderMac = new byte[32];
		headerBuf.position(64);
		headerBuf.get(storedHeaderMac);

		// calculate mac over first 64 bytes of header:
		final Mac headerMac = this.hmacSha256(hMacMasterKey);
		headerBuf.position(0);
		headerBuf.limit(64);
		headerMac.update(headerBuf);

		// check header integrity:
		if (!MessageDigest.isEqual(storedHeaderMac, headerMac.doFinal())) {
			throw new MacAuthenticationFailedException("Header MAC authentication failed.");
		}

		// find first relevant block:
		final long startBlock = pos / CONTENT_MAC_BLOCK; // floor
		final long startByte = startBlock * (CONTENT_MAC_BLOCK + 32) + 96l;
		final long offsetFromFirstBlock = pos - startBlock * CONTENT_MAC_BLOCK;

		// derive nonce used in counter mode from IV by setting last 64bit to 0:
		final ByteBuffer nonceBuf = ByteBuffer.wrap(iv.clone());
		nonceBuf.putLong(AES_BLOCK_LENGTH - Long.BYTES, startBlock * CONTENT_MAC_BLOCK / AES_BLOCK_LENGTH);
		final byte[] nonce = nonceBuf.array();

		// content decryption:
		encryptedFile.position(startByte);
		final SecretKey contentKey = new SecretKeySpec(contentKeyBytes, AES_KEY_ALGORITHM);
		final Cipher cipher = this.aesCtrCipher(contentKey, nonce, Cipher.DECRYPT_MODE);
		final Mac contentMac = this.hmacSha256(hMacMasterKey);

		try {

			// reading ciphered input and MACs interleaved:
			long bytesWritten = 0;
			final InputStream in = new SeekableByteChannelInputStream(encryptedFile);
			byte[] buffer = new byte[CONTENT_MAC_BLOCK + 32];
			int n = 0;
			while ((n = IOUtils.read(in, buffer)) > 0 && bytesWritten < length) {
				if (n < 32) {
					throw new DecryptFailedException("Invalid file content, missing MAC.");
				}

				// check MAC of current block:
				contentMac.update(buffer, 0, n - 32);
				final byte[] calculatedMac = contentMac.doFinal();
				final byte[] storedMac = new byte[32];
				System.arraycopy(buffer, n - 32, storedMac, 0, 32);
				if (!MessageDigest.isEqual(calculatedMac, storedMac)) {
					throw new MacAuthenticationFailedException("Content MAC authentication failed.");
				}

				// decrypt block:
				final byte[] plaintext = cipher.update(buffer, 0, n - 32);
				final int offset = (bytesWritten == 0) ? (int) offsetFromFirstBlock : 0;
				final long pending = length - bytesWritten;
				final int available = plaintext.length - offset;
				final int currentBatch = (int) Math.min(pending, available);

				plaintextFile.write(plaintext, offset, currentBatch);
				bytesWritten += currentBatch;
			}

			return bytesWritten;
		} finally {
			destroyQuietly(contentKey);
		}
	}

	/**
	 * header = {16 byte iv, 16 byte filesize, 32 byte contentKey, 32 byte headerMac}
	 */
	@Override
	public Long encryptFile(InputStream plaintextFile, SeekableByteChannel encryptedFile) throws IOException, EncryptFailedException {
		// truncate file
		encryptedFile.truncate(0l);

		// choose a random IV:
		final byte[] iv = randomData(AES_BLOCK_LENGTH);

		// derive nonce used in counter mode from IV by setting last 64bit to 0:
		final ByteBuffer nonceBuf = ByteBuffer.wrap(iv.clone());
		nonceBuf.putLong(AES_BLOCK_LENGTH - Long.BYTES, 0);
		final byte[] nonce = nonceBuf.array();

		// choose a random content key:
		final byte[] contentKeyBytes = randomData(32);

		// 96 byte header buffer (16 IV, 16 size, 32 content key, 32 headerMac), filled after writing the content
		final ByteBuffer headerBuf = ByteBuffer.allocate(96);
		headerBuf.limit(96);
		encryptedFile.write(headerBuf);

		// add random length padding to obfuscate file length:
		final byte[] randomPadding = this.randomData(AES_BLOCK_LENGTH);
		final LengthObfuscationInputStream in = new LengthObfuscationInputStream(plaintextFile, randomPadding);

		// content encryption:
		final SecretKey contentKey = new SecretKeySpec(contentKeyBytes, AES_KEY_ALGORITHM);
		final Cipher cipher = this.aesCtrCipher(contentKey, nonce, Cipher.ENCRYPT_MODE);
		final Mac contentMac = this.hmacSha256(hMacMasterKey);
		@SuppressWarnings("resource")
		final OutputStream out = new SeekableByteChannelOutputStream(encryptedFile);

		// writing ciphered output and MACs interleaved:
		final byte[] buffer = new byte[CONTENT_MAC_BLOCK];
		int n = 0;
		while ((n = IOUtils.read(in, buffer)) > 0) {
			final byte[] ciphertext = cipher.update(buffer, 0, n);
			out.write(ciphertext);
			contentMac.update(ciphertext);
			final byte[] mac = contentMac.doFinal();
			out.write(mac);
		}
		destroyQuietly(contentKey);

		// create and write header:
		final long plaintextSize = in.getRealInputLength();
		final ByteBuffer fileSizeBuffer = ByteBuffer.allocate(AES_BLOCK_LENGTH);
		fileSizeBuffer.putLong(plaintextSize);
		headerBuf.clear();
		headerBuf.put(iv);
		headerBuf.put(encryptHeaderData(fileSizeBuffer.array(), iv));
		headerBuf.put(encryptHeaderData(contentKeyBytes, iv));
		headerBuf.flip();
		final Mac headerMac = this.hmacSha256(hMacMasterKey);
		headerMac.update(headerBuf);
		headerBuf.limit(96);
		headerBuf.put(headerMac.doFinal());
		headerBuf.flip();
		encryptedFile.position(0);
		encryptedFile.write(headerBuf);

		return plaintextSize;
	}

}

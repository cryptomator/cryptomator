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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.sebastianstenzel.oce.crypto.Cryptor;
import de.sebastianstenzel.oce.crypto.TransactionAwareFileAccess;
import de.sebastianstenzel.oce.crypto.cache.PseudonymRepository;

/**
 * Default cryptor using PBKDF2 to derive an AES user key of up to 256 bit length.
 * This user key is used to decrypt the masterkey, which is a secure random chunk of data.
 * The masterkey in turn is used to decrypt all files in the secure storage location.
 */
public class AesCryptor extends Cryptor {

	private static final Logger LOG = LoggerFactory.getLogger(AesCryptor.class);
	private static final String METADATA_FILENAME = "metadata.json";
	private static final String KEYS_FILENAME = "keys.json";
	private static final char URI_PATH_SEP = '/';
	
	/**
	 * PRNG for cryptographically secure random numbers.
	 * Defaults to SHA1-based number generator.
	 * @see http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#SecureRandom
	 */
	private static final SecureRandom SECURE_PRNG;
	
	/**
	 * Factory for deriveing keys.
	 * Defaults to PBKDF2/HMAC-SHA1.
	 * @see PKCS #5, defined in RFC 2898
	 * @see http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#SecretKeyFactory
	 */
	private static final SecretKeyFactory PBKDF2_FACTORY;
	
	/**
	 * Number of bytes used as seed for the PRNG.
	 */
	private static final int PRNG_SEED_LENGTH = 16;
	
	/**
	 * Number of bytes of the master key.
	 * Should be significantly higher than the {@link #AES_KEY_LENGTH},
	 * as a corrupted masterkey can't be changed without decrypting and re-encrypting all files first.
	 */
	private static final int MASTER_KEY_LENGTH = 512;
	
	/**
	 * Number of bytes used as salt, where needed.
	 */
	private static final int SALT_LENGTH = 8;
	
	/**
	 * Our cryptographic algorithm.
	 * @see http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#AlgorithmParameters
	 */
	private static final String ALGORITHM = "AES";
	
	/**
	 * More detailed specification for {@link #ALGORITHM}.
	 * @see http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#Cipher
	 */
	private static final String CIPHER = "AES/CBC/PKCS5Padding";
	
	/**
	 * AES block size is 128 bit or 16 bytes.
	 */
	private static final int AES_BLOCK_LENGTH = 16;
	
	/**
	 * Defined in static initializer.
	 * Defaults to 256, but falls back to maximum value possible, if JCE isn't installed.
	 * JCE can be installed from here: http://www.oracle.com/technetwork/java/javase/downloads/.
	 */
	private static final int AES_KEY_LENGTH;
	
	/**
	 * Number of iterations for key derived from user pw.
	 * High iteration count for better resistance to bruteforcing.
	 */
	private static final int PBKDF2_PW_ITERATIONS = 1000;
	
	/**
	 * Number of iterations for key derived from masterkey.
	 * Low iteration count for better performance.
	 * No additional security is added by high values.
	 */
	private static final int PBKDF2_MASTERKEY_ITERATIONS = 1;
	
	/**
	 * Jackson JSON-Mapper.
	 */
	private final ObjectMapper objectMapper = new ObjectMapper();
	
	/**
	 * The decrypted master key.
	 * Its lifecycle starts with {@link #unlockStorage(Path, CharSequence)} or {@link #initializeStorage(Path, CharSequence)}.
	 * Its lifecycle ends with {@link #swipeSensitiveData()}.
	 */
	private final byte[] masterKey = new byte[MASTER_KEY_LENGTH];
	
	static {
		final String keyFactoryName = "PBKDF2WithHmacSHA1";
		final String prngName = "SHA1PRNG";
		try {
			PBKDF2_FACTORY = SecretKeyFactory.getInstance(keyFactoryName);
			SECURE_PRNG = SecureRandom.getInstance(prngName);
			final int maxKeyLen = Cipher.getMaxAllowedKeyLength(ALGORITHM);
			AES_KEY_LENGTH = (maxKeyLen >= 256) ? 256 : maxKeyLen;
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Algorithm should exist.", e);
		}
	}
	
	@Override
	public boolean isStorage(Path path) {
		try {
			final Path keysPath = path.resolve(KEYS_FILENAME);
			return Files.isReadable(keysPath);
		} catch(SecurityException ex) {
			return false;
		}
	}
	
	@Override
	public void initializeStorage(Path path, CharSequence password) throws AlreadyInitializedException, IOException {
		final Path keysPath = path.resolve(KEYS_FILENAME);
		if (Files.exists(keysPath)) {
			throw new AlreadyInitializedException(path);
		}
		try {
			// generate new masterkey:
			randomMasterKey();
			
			// derive key:
			final byte[] userSalt = randomData(SALT_LENGTH);
			final SecretKey userKey = pbkdf2(password, userSalt, PBKDF2_PW_ITERATIONS, AES_KEY_LENGTH);
			
			// encrypt:
			final byte[] iv = randomData(AES_BLOCK_LENGTH);
			final Cipher encCipher = this.cipher(userKey, iv, Cipher.ENCRYPT_MODE);
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
			this.saveKeys(keys, keysPath);
		} catch (IllegalBlockSizeException | BadPaddingException ex) {
			throw new IllegalStateException("Block size hard coded. Padding irrelevant in ENCRYPT_MODE. IV must exist in CBC mode.", ex);
		}
	}
	
	@Override
	public void unlockStorage(Path path, CharSequence password) throws InvalidStorageLocationException, DecryptFailedException, WrongPasswordException, UnsupportedKeyLengthException, IOException {
		final Path keysPath = path.resolve("keys.json");
		if (!this.isStorage(path)) {
			throw new InvalidStorageLocationException(path);
		}
		byte[] decrypted = new byte[0];
		try {
			// load encrypted masterkey:
			final Keys keys = this.loadKeys(keysPath);
			final Keys.Key ownerKey = keys.getOwnerKey();
			
			// check, whether the key length is supported:
			final int maxKeyLen = Cipher.getMaxAllowedKeyLength(ALGORITHM);
			if (ownerKey.getKeyLength() > maxKeyLen) {
				throw new UnsupportedKeyLengthException(ownerKey.getKeyLength(), maxKeyLen);
			}
			
			// derive key:
			final SecretKey userKey = pbkdf2(password, ownerKey.getSalt(), ownerKey.getIterations(), ownerKey.getKeyLength());
			
			// check password:
			final Cipher encCipher = this.cipher(userKey, ownerKey.getIv(), Cipher.ENCRYPT_MODE);
			byte[] encryptedUserKey = encCipher.doFinal(userKey.getEncoded());
			if (!Arrays.equals(ownerKey.getPwVerification(), encryptedUserKey)) {
				throw new WrongPasswordException();
			}

			// decrypt:
			final Cipher decCipher = this.cipher(userKey, ownerKey.getIv(), Cipher.DECRYPT_MODE);
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
	
	@Override
	public long encryptFile(String pseudonymizedUri, InputStream in, TransactionAwareFileAccess accessor) throws IOException {
		final Path path = accessor.resolveUri(pseudonymizedUri);
		OutputStream out = null;
		try {
			// unencrypted output stream:
			final byte[] salt = this.randomData(SALT_LENGTH);
			final byte[] iv = this.randomData(AES_BLOCK_LENGTH);
			out = accessor.openFileForWrite(path);
			out.write(salt, 0, salt.length);
			out.write(iv, 0, iv.length);
			
			// turn outputstream into an encrypting output stream:
			final SecretKey key = this.pbkdf2(masterKey, salt, PBKDF2_MASTERKEY_ITERATIONS, AES_KEY_LENGTH);
			final Cipher encCipher = this.cipher(key, iv, Cipher.ENCRYPT_MODE);
			out = new CipherOutputStream(out, encCipher);
			
			// write payload to encrypted out:
			final long decryptedFilesize = IOUtils.copyLarge(in, out);
			
			// save filesize to metadata:
			final String folderUri = FilenameUtils.getPath(pseudonymizedUri);
			final String pseudonym = FilenameUtils.getName(pseudonymizedUri);
			final Metadata metadata = loadOrCreateMetadata(accessor, folderUri);
			metadata.getFilesizes().put(pseudonym, decryptedFilesize);
			saveMetadata(metadata, accessor, folderUri);
			
			return decryptedFilesize;
		} finally {
			in.close();
			if (out != null) {
				out.close();
			}
		}
	}
	
	@Override
	public InputStream decryptFile(String pseudonymizedUri, TransactionAwareFileAccess accessor) throws IOException {
		// plain input stream:
		final Path path = accessor.resolveUri(pseudonymizedUri);
		final InputStream in = accessor.openFileForRead(path);
		final byte[] salt = new byte[SALT_LENGTH];
		final byte[] iv = new byte[AES_BLOCK_LENGTH];
		in.read(salt, 0, salt.length);
		in.read(iv, 0, iv.length);
		
		// deecrypting input stream:
		final SecretKey key = this.pbkdf2(masterKey, salt, PBKDF2_MASTERKEY_ITERATIONS, AES_KEY_LENGTH);
		final Cipher decCipher = this.cipher(key, iv, Cipher.DECRYPT_MODE);
		return new CipherInputStream(in, decCipher);
	}
	
	@Override
	public long getDecryptedContentLength(String pseudonymizedUri, TransactionAwareFileAccess accessor) throws IOException {
		final String folderUri = FilenameUtils.getPath(pseudonymizedUri);
		final String pseudonym = FilenameUtils.getName(pseudonymizedUri);
		final Metadata metadata = loadOrCreateMetadata(accessor, folderUri);
		if (metadata.getFilesizes().containsKey(pseudonym)) {
			return metadata.getFilesizes().get(pseudonym);
		} else {
			return -1;
		}
	}
	
	/**
	 * Overwrites the {@link #masterKey} with zeros.
	 * As masterKey is a final field, this operation is ensured to work on its actual data.
	 * Otherwise developers could accidentally just assign a new object to the variable.
	 */
	@Override
	public void swipeSensitiveData() {
		Arrays.fill(this.masterKey, (byte) 0);
	}
	
	private Cipher cipher(SecretKey key, byte[] iv, int cipherMode) {
		try {
			final Cipher cipher = Cipher.getInstance(CIPHER);
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
			final SecretKey aesKey = new SecretKeySpec(pbkdf2Key.getEncoded(), ALGORITHM);
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
	
	private Keys loadKeys(Path keysFile) throws IOException {
		InputStream in = null;
		try {
			in = Files.newInputStream(keysFile, StandardOpenOption.READ);
			return objectMapper.readValue(in, Keys.class);
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	private void saveKeys(Keys keys, Path keysFile) throws IOException {
		OutputStream out = null;
		try {
			out = Files.newOutputStream(keysFile, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC, StandardOpenOption.CREATE);
			objectMapper.writeValue(out, keys);
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}
	
	/* Pseudonymizing */

	@Override
	public String createPseudonym(String cleartextUri, TransactionAwareFileAccess access) throws IOException {
		final List<String> cleartextUriComps = this.splitUri(cleartextUri);
		final List<String> pseudonymUriComps = PseudonymRepository.pseudonymizedPathComponents(cleartextUriComps);

		// return immediately if path is already known:
		if (pseudonymUriComps.size() == cleartextUriComps.size()) {
			return concatUri(pseudonymUriComps);
		}

		// append further path components otherwise:
		for (int i = pseudonymUriComps.size(); i < cleartextUriComps.size(); i++) {
			final String currentFolder = concatUri(pseudonymUriComps);
			final String cleartext = cleartextUriComps.get(i);
			String pseudonym = readPseudonymFromMetadata(access, currentFolder, cleartext);
			if (pseudonym == null) {
				pseudonym = UUID.randomUUID().toString();
				this.addToMetadata(access, currentFolder, cleartext, pseudonym);
			}
			pseudonymUriComps.add(pseudonym);
		}
		PseudonymRepository.registerPath(cleartextUriComps, pseudonymUriComps);

		return concatUri(pseudonymUriComps);
	}

	@Override
	public String uncoverPseudonym(String pseudonymizedUri, TransactionAwareFileAccess access) throws IOException {
		final List<String> pseudonymUriComps = this.splitUri(pseudonymizedUri);
		final List<String> cleartextUriComps = PseudonymRepository.cleartextPathComponents(pseudonymUriComps);

		// return immediately if path is already known:
		if (cleartextUriComps.size() == pseudonymUriComps.size()) {
			return concatUri(cleartextUriComps);
		}

		// append further path components otherwise:
		for (int i = cleartextUriComps.size(); i < pseudonymUriComps.size(); i++) {
			final String currentFolder = concatUri(pseudonymUriComps.subList(0, i));
			final String pseudonym = pseudonymUriComps.get(i);
			try {
				final String cleartext = this.readCleartextFromMetadata(access, currentFolder, pseudonym);
				if (cleartext == null) {
					return null;
				}
				cleartextUriComps.add(cleartext);
			} catch (IOException ex) {
				LOG.warn("Unresolvable pseudonym: " + currentFolder + "/" + pseudonym);
				return null;
			}
		}
		PseudonymRepository.registerPath(cleartextUriComps, pseudonymUriComps);

		return concatUri(cleartextUriComps);
	}

	@Override
	public void deletePseudonym(String pseudonymizedUri, TransactionAwareFileAccess access) throws IOException {
		// find parent folder:
		final int lastPathSeparator = pseudonymizedUri.lastIndexOf(URI_PATH_SEP);
		final String parentUri;
		if (lastPathSeparator > 0) {
			parentUri = pseudonymizedUri.substring(0, lastPathSeparator);
		} else {
			parentUri = "/";
		}

		// delete from metadata file:
		final String pseudonym = pseudonymizedUri.substring(lastPathSeparator + 1);
		final Metadata metadata = this.loadOrCreateMetadata(access, parentUri);
		metadata.getFilenames().remove(pseudonym);
		metadata.getFilesizes().remove(pseudonym);
		this.saveMetadata(metadata, access, parentUri);

		// delete from cache:
		final List<String> pseudonymUriComps = this.splitUri(pseudonymizedUri);
		PseudonymRepository.unregisterPath(pseudonymUriComps);
	}

	/* Metadata load & save */

	private String readPseudonymFromMetadata(TransactionAwareFileAccess access, String parentFolder, String cleartext) throws IOException {
		final Metadata metadata = loadOrCreateMetadata(access, parentFolder);
		return metadata.getFilenames().getKey(cleartext);
	}

	private String readCleartextFromMetadata(TransactionAwareFileAccess access, String parentFolder, String pseudonym) throws IOException {
		final Metadata metadata = loadOrCreateMetadata(access, parentFolder);
		final byte[] encryptedFilename = metadata.getFilenames().get(pseudonym);
		if (encryptedFilename == null) {
			return null;
		}
		try {
			// decrypt filename:
			final SecretKey key = this.pbkdf2(masterKey, metadata.getSalt(), PBKDF2_MASTERKEY_ITERATIONS, AES_KEY_LENGTH);
			final Cipher decCipher = this.cipher(key, metadata.getIv(), Cipher.DECRYPT_MODE);
			byte[] decryptedFilename = decCipher.doFinal(encryptedFilename);
			return new String(decryptedFilename, Charsets.UTF_8);
		} catch (IllegalBlockSizeException | BadPaddingException ex) {
			LOG.error("Can't decrypt filename " + pseudonym + " in folder " + parentFolder, ex);
			return null;
		}
	}

	private void addToMetadata(TransactionAwareFileAccess access, String parentFolder, String cleartext, String pseudonym) throws IOException {
		final Metadata metadata = loadOrCreateMetadata(access, parentFolder);
		try {
			// encrypt filename:
			final SecretKey key = this.pbkdf2(masterKey, metadata.getSalt(), PBKDF2_MASTERKEY_ITERATIONS, AES_KEY_LENGTH);
			final Cipher encCipher = this.cipher(key, metadata.getIv(), Cipher.ENCRYPT_MODE);
			byte[] encryptedFilename = encCipher.doFinal(cleartext.getBytes(Charsets.UTF_8));
			
			// save metadata
			metadata.getFilenames().put(pseudonym, encryptedFilename);
			saveMetadata(metadata, access, parentFolder);
		} catch (IllegalBlockSizeException | BadPaddingException ex) {
			LOG.error("Can't encrypt filename " + pseudonym + " (" + cleartext + ") in folder " + parentFolder, ex);
		}
	}

	private Metadata loadOrCreateMetadata(TransactionAwareFileAccess access, String parentFolder) throws IOException {
		InputStream in = null;
		try {
			final Path path = access.resolveUri(parentFolder).resolve(METADATA_FILENAME);
			in = access.openFileForRead(path);
			return objectMapper.readValue(in, Metadata.class);
		} catch (IOException ex) {
			final byte[] salt = randomData(SALT_LENGTH);
			final byte[] iv = randomData(AES_BLOCK_LENGTH);
			return new Metadata(iv, salt);
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	private void saveMetadata(Metadata metadata, TransactionAwareFileAccess access, String parentFolder) throws IOException {
		OutputStream out = null;
		try {
			final Path path = access.resolveUri(parentFolder).resolve(METADATA_FILENAME);
			out = access.openFileForWrite(path);
			objectMapper.writeValue(out, metadata);
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	/* utility stuff */

	private String concatUri(final List<String> uriComponents) {
		final StringBuilder sb = new StringBuilder();
		for (final String comp : uriComponents) {
			sb.append(URI_PATH_SEP).append(comp);
		}
		return sb.toString();
	}

	private List<String> splitUri(final String uri) {
		final List<String> result = new ArrayList<>();
		int begin = 0;
		int end = 0;
		do {
			end = uri.indexOf(URI_PATH_SEP, begin);
			end = (end == -1) ? uri.length() : end;
			if (end > begin) {
				result.add(uri.substring(begin, end));
			}
			begin = end + 1;
		} while (end < uri.length());
		return result;
	}

}

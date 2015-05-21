package org.cryptomator.webdav.jackrabbit;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.cryptomator.crypto.Cryptor;
import org.cryptomator.crypto.exceptions.DecryptFailedException;

import com.fasterxml.jackson.databind.ObjectMapper;

class FilenameTranslator implements FileNamingConventions {

	private final Cryptor cryptor;
	private final Path dataRoot;
	private final Path metadataRoot;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public FilenameTranslator(Cryptor cryptor, Path vaultRoot) {
		this.cryptor = cryptor;
		this.dataRoot = vaultRoot.resolve("d");
		this.metadataRoot = vaultRoot.resolve("m");
	}

	/* file and directory name en/decryption */

	public Path getEncryptedDirectoryPath(String directoryId) {
		final String encrypted = cryptor.encryptDirectoryPath(directoryId, FileSystems.getDefault().getSeparator());
		return dataRoot.resolve(encrypted);
	}

	public String getEncryptedFilename(String cleartextFilename) throws IOException {
		return getEncryptedFilename(cleartextFilename, FILE_EXT, LONG_FILE_EXT);
	}

	public String getEncryptedDirName(String cleartextDirName) throws IOException {
		return getEncryptedFilename(cleartextDirName, DIR_EXT, LONG_DIR_EXT);
	}

	/**
	 * Encryption will blow up the filename length due to aes block sizes, IVs and base32 encoding. The result may be too long for some old file systems.<br/>
	 * This means that we need a workaround for filenames longer than the limit defined in {@link FileNamingConventions#ENCRYPTED_FILENAME_LENGTH_LIMIT}.<br/>
	 * <br/>
	 * For filenames longer than this limit we use a metadata file containing the full encrypted paths. For the actual filename a unique alternative is created by concatenating the metadata filename
	 * and a unique id.
	 */
	private String getEncryptedFilename(String cleartextFilename, String basicExt, String longExt) throws IOException {
		final String ivAndCiphertext = cryptor.encryptFilename(cleartextFilename);
		if (ivAndCiphertext.length() + basicExt.length() > ENCRYPTED_FILENAME_LENGTH_LIMIT) {
			final String metadataGroup = ivAndCiphertext.substring(0, LONG_NAME_PREFIX_LENGTH);
			final LongFilenameMetadata metadata = readMetadata(metadataGroup);
			final String longFilename = metadataGroup + metadata.getOrCreateUuidForEncryptedFilename(ivAndCiphertext).toString() + longExt;
			this.writeMetadata(metadataGroup, metadata);
			return longFilename;
		} else {
			return ivAndCiphertext + basicExt;
		}
	}

	public String getCleartextFilename(String encryptedFilename) throws DecryptFailedException, IOException {
		final String ciphertext;
		if (StringUtils.endsWithIgnoreCase(encryptedFilename, LONG_FILE_EXT)) {
			final String basename = StringUtils.removeEndIgnoreCase(encryptedFilename, LONG_FILE_EXT);
			final String metadataGroup = basename.substring(0, LONG_NAME_PREFIX_LENGTH);
			final String uuid = basename.substring(LONG_NAME_PREFIX_LENGTH);
			final LongFilenameMetadata metadata = readMetadata(metadataGroup);
			ciphertext = metadata.getEncryptedFilenameForUUID(UUID.fromString(uuid));
		} else if (StringUtils.endsWithIgnoreCase(encryptedFilename, FILE_EXT)) {
			ciphertext = StringUtils.removeEndIgnoreCase(encryptedFilename, FILE_EXT);
		} else if (StringUtils.endsWithIgnoreCase(encryptedFilename, LONG_DIR_EXT)) {
			final String basename = StringUtils.removeEndIgnoreCase(encryptedFilename, LONG_DIR_EXT);
			final String metadataGroup = basename.substring(0, LONG_NAME_PREFIX_LENGTH);
			final String uuid = basename.substring(LONG_NAME_PREFIX_LENGTH);
			final LongFilenameMetadata metadata = readMetadata(metadataGroup);
			ciphertext = metadata.getEncryptedFilenameForUUID(UUID.fromString(uuid));
		} else if (StringUtils.endsWithIgnoreCase(encryptedFilename, DIR_EXT)) {
			ciphertext = StringUtils.removeEndIgnoreCase(encryptedFilename, DIR_EXT);
		} else {
			throw new IllegalArgumentException("Unsupported path component: " + encryptedFilename);
		}
		return cryptor.decryptFilename(ciphertext);
	}

	/* Long name metadata files */

	private void writeMetadata(String metadataGroup, LongFilenameMetadata metadata) throws IOException {
		final Path metadataDir = metadataRoot.resolve(metadataGroup.substring(0, 2));
		Files.createDirectories(metadataDir);
		final Path metadataFile = metadataDir.resolve(metadataGroup.substring(2));
		try (final FileChannel c = FileChannel.open(metadataFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.DSYNC); final FileLock lock = c.lock()) {
			byte[] bytes = objectMapper.writeValueAsBytes(metadata);
			c.write(ByteBuffer.wrap(bytes));
		}
	}

	private LongFilenameMetadata readMetadata(String metadataGroup) throws IOException {
		final Path metadataDir = metadataRoot.resolve(metadataGroup.substring(0, 2));
		final Path metadataFile = metadataDir.resolve(metadataGroup.substring(2));
		if (!Files.isReadable(metadataFile)) {
			return new LongFilenameMetadata();
		} else {
			try (final FileChannel c = FileChannel.open(metadataFile, StandardOpenOption.READ, StandardOpenOption.DSYNC); final FileLock lock = c.lock(0L, Long.MAX_VALUE, true)) {
				final ByteBuffer buffer = ByteBuffer.allocate((int) c.size());
				c.read(buffer);
				return objectMapper.readValue(buffer.array(), LongFilenameMetadata.class);
			}
		}
	}

}

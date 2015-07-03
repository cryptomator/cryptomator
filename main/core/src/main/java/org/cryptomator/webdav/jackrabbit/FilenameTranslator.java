package org.cryptomator.webdav.jackrabbit;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.cryptomator.crypto.Cryptor;
import org.cryptomator.crypto.exceptions.DecryptFailedException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

class FilenameTranslator implements FileConstants {

	private static final int MAX_CACHED_DIRECTORY_IDS = 5000;
	private static final int MAX_CACHED_METADATA_FILES = 1000;

	private final Cryptor cryptor;
	private final Path dataRoot;
	private final Path metadataRoot;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final Map<Pair<Path, FileTime>, String> directoryIdCache = new LRUMap<>(MAX_CACHED_DIRECTORY_IDS); // <directoryFile, directoryId>
	private final Map<Pair<Path, FileTime>, LongFilenameMetadata> metadataCache = new LRUMap<>(MAX_CACHED_METADATA_FILES); // <metadataFile, metadata>

	public FilenameTranslator(Cryptor cryptor, Path vaultRoot) {
		this.cryptor = cryptor;
		this.dataRoot = vaultRoot.resolve("d");
		this.metadataRoot = vaultRoot.resolve("m");
	}

	/* file and directory name en/decryption */

	public String getDirectoryId(Path directoryFile, boolean createIfNonexisting) throws IOException {
		try {
			final Pair<Path, FileTime> key = ImmutablePair.of(directoryFile, Files.getLastModifiedTime(directoryFile));
			String directoryId = directoryIdCache.get(key);
			if (directoryId == null) {
				directoryId = new String(readAllBytesAtomically(directoryFile), StandardCharsets.UTF_8);
				directoryIdCache.put(key, directoryId);
			}
			return directoryId;
		} catch (FileNotFoundException | NoSuchFileException e) {
			if (createIfNonexisting) {
				final String directoryId = UUID.randomUUID().toString();
				writeAllBytesAtomically(directoryFile, directoryId.getBytes(StandardCharsets.UTF_8));
				final Pair<Path, FileTime> key = ImmutablePair.of(directoryFile, Files.getLastModifiedTime(directoryFile));
				directoryIdCache.put(key, directoryId);
				return directoryId;
			} else {
				return null;
			}
		}
	}

	public Path getEncryptedDirectoryPath(String directoryId) {
		final String encrypted = cryptor.encryptDirectoryPath(directoryId, FileSystems.getDefault().getSeparator());
		return dataRoot.resolve(encrypted);
	}

	public String getEncryptedFilename(String cleartextFilename) throws IOException {
		return getEncryptedFilename(cleartextFilename, FILE_EXT, LONG_FILE_EXT);
	}

	public String getEncryptedDirFileName(String cleartextDirName) throws IOException {
		return getEncryptedFilename(cleartextDirName, DIR_EXT, LONG_DIR_EXT);
	}

	/**
	 * Encryption will blow up the filename length due to aes block sizes, IVs and base32 encoding. The result may be too long for some old file systems.<br/>
	 * This means that we need a workaround for filenames longer than the limit defined in {@link FileConstants#ENCRYPTED_FILENAME_LENGTH_LIMIT}.<br/>
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

	/* Locked I/O */

	private void writeAllBytesAtomically(Path path, byte[] bytes) throws IOException {
		try (final FileChannel c = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.DSYNC);
				final SilentlyFailingFileLock lock = new SilentlyFailingFileLock(c, false)) {
			c.write(ByteBuffer.wrap(bytes));
		}
	}

	private byte[] readAllBytesAtomically(Path path) throws IOException {
		try (final FileChannel c = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.DSYNC); final SilentlyFailingFileLock lock = new SilentlyFailingFileLock(c, true)) {
			final ByteBuffer buffer = ByteBuffer.allocate((int) c.size());
			c.read(buffer);
			return buffer.array();
		}
	}

	/* Long name metadata files */

	private void writeMetadata(String metadataGroup, LongFilenameMetadata metadata) throws IOException {
		final Path metadataDir = metadataRoot.resolve(metadataGroup.substring(0, 2));
		Files.createDirectories(metadataDir);
		final Path metadataFile = metadataDir.resolve(metadataGroup.substring(2));

		// evict previously cached entries:
		try {
			final Pair<Path, FileTime> key = ImmutablePair.of(metadataFile, Files.getLastModifiedTime(metadataFile));
			metadataCache.remove(key);
		} catch (FileNotFoundException | NoSuchFileException e) {
			// didn't exist yet? then we don't need to do anything anyway.
		}

		// write:
		final byte[] metadataContent = objectMapper.writeValueAsBytes(metadata);
		writeAllBytesAtomically(metadataFile, metadataContent);

		// add to cache:
		final Pair<Path, FileTime> key = ImmutablePair.of(metadataFile, Files.getLastModifiedTime(metadataFile));
		metadataCache.put(key, metadata);
	}

	private LongFilenameMetadata readMetadata(String metadataGroup) throws IOException {
		final Path metadataDir = metadataRoot.resolve(metadataGroup.substring(0, 2));
		final Path metadataFile = metadataDir.resolve(metadataGroup.substring(2));
		try {
			// use cached metadata, if possible:
			final Pair<Path, FileTime> key = ImmutablePair.of(metadataFile, Files.getLastModifiedTime(metadataFile));
			LongFilenameMetadata metadata = metadataCache.get(key);
			// else read from filesystem:
			if (metadata == null) {
				final byte[] metadataContent = readAllBytesAtomically(metadataFile);
				metadata = objectMapper.readValue(metadataContent, LongFilenameMetadata.class);
				metadataCache.put(key, metadata);
			}
			return metadata;
		} catch (FileNotFoundException | NoSuchFileException e) {
			// not yet existing:
			return new LongFilenameMetadata();
		}
	}

	private static class LongFilenameMetadata implements Serializable {

		private static final long serialVersionUID = 6214509403824421320L;

		@JsonDeserialize(as = DualHashBidiMap.class)
		private BidiMap<UUID, String> encryptedFilenames = new DualHashBidiMap<>();

		/* Getter/Setter */

		public synchronized String getEncryptedFilenameForUUID(final UUID uuid) {
			return encryptedFilenames.get(uuid);
		}

		public synchronized UUID getOrCreateUuidForEncryptedFilename(String encryptedFilename) {
			UUID uuid = encryptedFilenames.getKey(encryptedFilename);
			if (uuid == null) {
				uuid = UUID.randomUUID();
				encryptedFilenames.put(uuid, encryptedFilename);
			}
			return uuid;
		}

		// used by jackson
		@SuppressWarnings("unused")
		public BidiMap<UUID, String> getEncryptedFilenames() {
			return encryptedFilenames;
		}

		// used by jackson
		@SuppressWarnings("unused")
		public void setEncryptedFilenames(BidiMap<UUID, String> encryptedFilenames) {
			this.encryptedFilenames = encryptedFilenames;
		}

	}

}

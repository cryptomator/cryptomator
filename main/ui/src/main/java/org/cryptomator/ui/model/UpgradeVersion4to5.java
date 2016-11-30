package org.cryptomator.ui.model;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.cryptomator.cryptolib.Cryptors;
import org.cryptomator.cryptolib.api.CryptoLibVersion;
import org.cryptomator.cryptolib.api.CryptoLibVersion.Version;
import org.cryptomator.cryptolib.api.Cryptor;
import org.cryptomator.cryptolib.api.CryptorProvider;
import org.cryptomator.cryptolib.api.FileHeader;
import org.cryptomator.ui.settings.Localization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains the collective knowledge of all creatures who were alive during the development of vault format 3.
 * This class uses no external classes from the crypto or shortening layer by purpose, so we don't need legacy code inside these.
 */
@Singleton
class UpgradeVersion4to5 extends UpgradeStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(UpgradeVersion4to5.class);
	private static final Pattern BASE32_PATTERN = Pattern.compile("^([A-Z2-7]{8})*[A-Z2-7=]{8}");

	@Inject
	public UpgradeVersion4to5(@CryptoLibVersion(Version.ONE) CryptorProvider version1CryptorProvider, Localization localization) {
		super(version1CryptorProvider, localization, 4, 5);
	}

	@Override
	public String getTitle(Vault vault) {
		return localization.getString("upgrade.version4to5.title");
	}

	@Override
	public String getMessage(Vault vault) {
		return localization.getString("upgrade.version4to5.msg");
	}

	@Override
	protected void upgrade(Vault vault, Cryptor cryptor) throws UpgradeFailedException {
		Path dataDir = vault.path().get().resolve("d");
		if (!Files.isDirectory(dataDir)) {
			return; // empty vault. no migration needed.
		}
		try {
			Files.walkFileTree(dataDir, new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (BASE32_PATTERN.matcher(file.getFileName().toString()).find() && attrs.size() > cryptor.fileHeaderCryptor().headerSize()) {
						migrate(file, attrs, cryptor);
					} else {
						LOG.info("Skipping irrelevant file {}.", file);
					}
					return FileVisitResult.CONTINUE;
				}

			});
		} catch (IOException e) {
			LOG.error("Migration failed.", e);
			throw new UpgradeFailedException(localization.getString("upgrade.version4to5.err.io"));
		}
		LOG.info("Migration finished.");
	}

	private void migrate(Path file, BasicFileAttributes attrs, Cryptor cryptor) throws IOException {
		LOG.info("Starting migration of {}...", file);
		try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
			// read header:
			ByteBuffer headerBuf = ByteBuffer.allocate(cryptor.fileHeaderCryptor().headerSize());
			ch.read(headerBuf);
			headerBuf.flip();
			LOG.info("\tHeader read");
			FileHeader header = cryptor.fileHeaderCryptor().decryptHeader(headerBuf);
			long cleartextSize = header.getFilesize();
			if (cleartextSize < 0) {
				LOG.info("\tSkipping already migrated file");
				return;
			} else if (cleartextSize > attrs.size()) {
				LOG.warn("\tSkipping file with invalid file size {}/{}", cleartextSize, attrs.size());
				return;
			}
			int headerSize = cryptor.fileHeaderCryptor().headerSize();
			int ciphertextChunkSize = cryptor.fileContentCryptor().ciphertextChunkSize();
			int cleartextChunkSize = cryptor.fileContentCryptor().cleartextChunkSize();
			long newCiphertextSize = Cryptors.ciphertextSize(cleartextSize, cryptor);
			long newEOF = headerSize + newCiphertextSize;
			long newFullChunks = newCiphertextSize / ciphertextChunkSize; // int-truncation
			long newAdditionalCiphertextBytes = newCiphertextSize % ciphertextChunkSize;
			if (newAdditionalCiphertextBytes == 0) {
				// (new) last block is already correct. just truncate:
				LOG.info("\tMigrating cleartext size {}: Truncating to new ciphertext size: {}", cleartextSize, newEOF);
				ch.truncate(newEOF);
				LOG.info("\tFile truncated");
			} else {
				// last block may contain padding and needs to be re-encrypted:
				long lastChunkIdx = newFullChunks;
				LOG.info("\tMigrating cleartext size {}: Re-encrypting chunk {}. New ciphertext size: {}", cleartextSize, lastChunkIdx, newEOF);
				long beginOfLastChunk = headerSize + lastChunkIdx * ciphertextChunkSize;
				assert beginOfLastChunk < newEOF;
				int lastCleartextChunkLength = (int) (cleartextSize % cleartextChunkSize);
				assert lastCleartextChunkLength < cleartextChunkSize;
				assert lastCleartextChunkLength > 0;
				ch.position(beginOfLastChunk);
				ByteBuffer lastCiphertextChunk = ByteBuffer.allocate(ciphertextChunkSize);
				int read = ch.read(lastCiphertextChunk);
				if (read != -1) {
					lastCiphertextChunk.flip();
					ByteBuffer lastCleartextChunk = cryptor.fileContentCryptor().decryptChunk(lastCiphertextChunk, lastChunkIdx, header, true);
					lastCleartextChunk.position(0).limit(lastCleartextChunkLength);
					assert lastCleartextChunk.remaining() == lastCleartextChunkLength;
					ByteBuffer newLastChunkCiphertext = cryptor.fileContentCryptor().encryptChunk(lastCleartextChunk, lastChunkIdx, header);
					ch.truncate(beginOfLastChunk);
					ch.write(newLastChunkCiphertext);
				} else {
					LOG.error("\tReached EOF at position {}/{}", beginOfLastChunk, newEOF);
					return; // must exit method before changing header!
				}
				LOG.info("\tReencrypted last block");
			}
			header.setFilesize(-1l);
			ByteBuffer newHeaderBuf = cryptor.fileHeaderCryptor().encryptHeader(header);
			ch.position(0);
			ch.write(newHeaderBuf);
			LOG.info("\tUpdated header");
		}
		LOG.info("Finished migration of {}.", file);
	}

}

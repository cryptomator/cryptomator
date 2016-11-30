package org.cryptomator.ui.model;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.BaseNCodec;
import org.apache.commons.lang3.StringUtils;
import org.cryptomator.cryptolib.api.CryptoLibVersion;
import org.cryptomator.cryptolib.api.CryptoLibVersion.Version;
import org.cryptomator.cryptolib.api.Cryptor;
import org.cryptomator.cryptolib.api.CryptorProvider;
import org.cryptomator.cryptolib.common.MessageDigestSupplier;
import org.cryptomator.ui.settings.Localization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains the collective knowledge of all creatures who were alive during the development of vault format 3.
 * This class uses no external classes from the crypto or shortening layer by purpose, so we don't need legacy code inside these.
 */
@Singleton
class UpgradeVersion3to4 extends UpgradeStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(UpgradeVersion3to4.class);
	private static final Pattern BASE32_FOLLOWED_BY_UNDERSCORE_PATTERN = Pattern.compile("^(([A-Z2-7]{8})*[A-Z2-7=]{8})_");
	private static final int FILE_MIN_SIZE = 88; // vault version 3 files have a header of 88 bytes (assuming no chunks at all)
	private static final String LONG_FILENAME_SUFFIX = ".lng";
	private static final String OLD_FOLDER_SUFFIX = "_";
	private static final String NEW_FOLDER_PREFIX = "0";

	private final MessageDigest sha1 = MessageDigestSupplier.SHA1.get();
	private final BaseNCodec base32 = new Base32();

	@Inject
	public UpgradeVersion3to4(@CryptoLibVersion(Version.ONE) CryptorProvider version1CryptorProvider, Localization localization) {
		super(version1CryptorProvider, localization, 3, 4);
	}

	@Override
	public String getTitle(Vault vault) {
		return localization.getString("upgrade.version3to4.title");
	}

	@Override
	public String getMessage(Vault vault) {
		return localization.getString("upgrade.version3to4.msg");
	}

	@Override
	protected void upgrade(Vault vault, Cryptor cryptor) throws UpgradeFailedException {
		Path dataDir = vault.path().get().resolve("d");
		Path metadataDir = vault.path().get().resolve("m");
		if (!Files.isDirectory(dataDir)) {
			return; // empty vault. no migration needed.
		}
		try {
			Files.walkFileTree(dataDir, new FileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					String name = file.getFileName().toString();
					if (name.endsWith(LONG_FILENAME_SUFFIX)) {
						migrateLong(metadataDir, file);
					} else {
						migrate(file, attrs);
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
					throw exc;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}

			});
		} catch (IOException e) {
			LOG.error("Migration failed.", e);
			throw new UpgradeFailedException(localization.getString("upgrade.version3to4.err.io"));
		}
		LOG.info("Migration finished.");
	}

	private void migrate(Path file, BasicFileAttributes attrs) throws IOException {
		String name = file.getFileName().toString();
		long size = attrs.size();
		Matcher m = BASE32_FOLLOWED_BY_UNDERSCORE_PATTERN.matcher(name);
		if (m.find(0) && size < FILE_MIN_SIZE) {
			String base32 = m.group(1);
			String suffix = name.substring(m.end());
			String renamed = NEW_FOLDER_PREFIX + base32 + (suffix.isEmpty() ? "" : " " + suffix);
			renameWithoutOverwriting(file, renamed);
		}
	}

	private void renameWithoutOverwriting(Path path, String newName) throws IOException {
		Path newPath = path.resolveSibling(newName);
		for (int i = 2; Files.exists(newPath); i++) {
			newPath = path.resolveSibling(newName + " " + i);
		}
		Files.move(path, newPath);
		LOG.info("Renaming {} to {}", path, newPath.getFileName());
	}

	private void migrateLong(Path metadataDir, Path path) throws IOException {
		String oldName = path.getFileName().toString();
		Path oldMetadataFile = metadataDir.resolve(oldName.substring(0, 2)).resolve(oldName.substring(2, 4)).resolve(oldName);
		if (Files.isRegularFile(oldMetadataFile)) {
			String oldContent = new String(Files.readAllBytes(oldMetadataFile), UTF_8);
			if (oldContent.endsWith(OLD_FOLDER_SUFFIX)) {
				String newContent = NEW_FOLDER_PREFIX + StringUtils.removeEnd(oldContent, OLD_FOLDER_SUFFIX);
				String newName = base32.encodeAsString(sha1.digest(newContent.getBytes(UTF_8))) + LONG_FILENAME_SUFFIX;
				Path newPath = path.resolveSibling(newName);
				Path newMetadataFile = metadataDir.resolve(newName.substring(0, 2)).resolve(newName.substring(2, 4)).resolve(newName);
				Files.move(path, newPath);
				Files.createDirectories(newMetadataFile.getParent());
				Files.write(newMetadataFile, newContent.getBytes(UTF_8));
				Files.delete(oldMetadataFile);
				LOG.info("Renaming {} to {}\nDeleting {}\nCreating {}", path, newName, oldMetadataFile, newMetadataFile);
			}
		} else {
			LOG.warn("Found uninflatable long file name. Expected: {}", oldMetadataFile);
		}
	}

}

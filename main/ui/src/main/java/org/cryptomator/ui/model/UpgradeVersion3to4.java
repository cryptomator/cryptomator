package org.cryptomator.ui.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.cryptomator.crypto.engine.Cryptor;
import org.cryptomator.filesystem.crypto.Constants;
import org.cryptomator.ui.settings.Localization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
class UpgradeVersion3to4 extends UpgradeStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(UpgradeVersion3to4.class);
	private static final Pattern BASE32_FOLLOWED_BY_UNDERSCORE_PATTERN = Pattern.compile("^(([A-Z2-7]{8})*[A-Z2-7=]{8})_");
	private static final int FILE_MIN_SIZE = 88; // vault version 3 files have a header of 88 bytes (assuming no chunks at all)

	@Inject
	public UpgradeVersion3to4(Provider<Cryptor> cryptorProvider, Localization localization) {
		super(cryptorProvider, localization);
	}

	@Override
	public String getNotification(Vault vault) {
		return localization.getString("upgrade.version3to4.msg");
	}

	@Override
	protected void upgrade(Vault vault, Cryptor cryptor) throws UpgradeFailedException {
		Path dataDir = vault.path().get().resolve("d");
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
					migrate(file, attrs);
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
			String renamed = "0" + base32 + (suffix.isEmpty() ? "" : " " + suffix);
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

	@Override
	public boolean isApplicable(Vault vault) {
		final Path masterkeyFile = vault.path().getValue().resolve(Constants.MASTERKEY_FILENAME);
		try {
			if (Files.isRegularFile(masterkeyFile)) {
				final String keyContents = new String(Files.readAllBytes(masterkeyFile), StandardCharsets.UTF_8);
				return keyContents.contains("\"version\":3") || keyContents.contains("\"version\": 3");
			} else {
				LOG.warn("Not a file: {}", masterkeyFile);
				return false;
			}
		} catch (IOException e) {
			LOG.warn("Could not determine, whether upgrade is applicable.", e);
			return false;
		}
	}

}

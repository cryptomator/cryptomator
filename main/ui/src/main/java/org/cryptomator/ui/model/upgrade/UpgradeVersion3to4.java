/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.model.upgrade;

import com.google.common.io.BaseEncoding;
import org.apache.commons.lang3.StringUtils;
import org.cryptomator.common.FxApplicationScoped;
import org.cryptomator.cryptolib.Cryptors;
import org.cryptomator.cryptolib.api.Cryptor;
import org.cryptomator.cryptolib.common.MessageDigestSupplier;
import org.cryptomator.ui.l10n.Localization;
import org.cryptomator.ui.model.Vault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Contains the collective knowledge of all creatures who were alive during the development of vault format 3.
 * This class uses no external classes from the crypto or shortening layer by purpose, so we don't need legacy code inside these.
 */
@FxApplicationScoped
class UpgradeVersion3to4 extends UpgradeStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(UpgradeVersion3to4.class);
	private static final Pattern LVL1_DIR_PATTERN = Pattern.compile("[A-Z2-7]{2}");
	private static final Pattern LVL2_DIR_PATTERN = Pattern.compile("[A-Z2-7]{30}");
	private static final Pattern BASE32_PATTERN = Pattern.compile("^(([A-Z2-7]{8})*[A-Z2-7=]{8})");
	private static final Pattern BASE32_FOLLOWED_BY_UNDERSCORE_PATTERN = Pattern.compile(BASE32_PATTERN.pattern() + "_");
	private static final int FILE_MIN_SIZE = 88; // vault version 3 files have a header of 88 bytes (assuming no chunks at all)
	private static final String LONG_FILENAME_EXT = ".lng";
	private static final String OLD_FOLDER_SUFFIX = "_";
	private static final String NEW_FOLDER_PREFIX = "0";

	private final MessageDigest sha1 = MessageDigestSupplier.SHA1.get();
	private final BaseEncoding base32 = BaseEncoding.base32();

	@Inject
	public UpgradeVersion3to4(Localization localization) {
		super(Cryptors.version1(UpgradeStrategy.strongSecureRandom()), localization, 3, 4);
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
		Path dataDir = vault.getPath().resolve("d");
		Path metadataDir = vault.getPath().resolve("m");
		if (!Files.isDirectory(dataDir)) {
			return; // empty vault. no migration needed.
		}
		try {
			Files.walkFileTree(dataDir, EnumSet.noneOf(FileVisitOption.class), 3, new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					if (dir.equals(dataDir)) {
						// path/to/vault/d
						return FileVisitResult.CONTINUE;
					} else if (dir.getParent().equals(dataDir) && LVL1_DIR_PATTERN.matcher(dir.getFileName().toString()).matches()) {
						// path/to/vault/d/AB
						return FileVisitResult.CONTINUE;
					} else if (dir.getParent().getParent().equals(dataDir) && LVL2_DIR_PATTERN.matcher(dir.getFileName().toString()).matches()) {
						// path/to/vault/d/AB/CDEFGHIJKLMNOPQRSTUVWXYZ234567
						return FileVisitResult.CONTINUE;
					} else {
						LOG.info("Skipping irrelevant directory {}", dir);
						return FileVisitResult.SKIP_SUBTREE;
					}
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					String name = file.getFileName().toString();
					if (attrs.size() > FILE_MIN_SIZE) {
						LOG.trace("Skipping non-directory file {}.", file);
					} else if (name.endsWith(LONG_FILENAME_EXT)) {
						migrateLong(metadataDir, file);
					} else {
						migrate(file);
					}
					return FileVisitResult.CONTINUE;
				}

			});
		} catch (IOException e) {
			LOG.error("Migration failed.", e);
			throw new UpgradeFailedException(localization.getString("upgrade.version3to4.err.io"));
		}
		LOG.info("Migration finished.");
	}

	private void migrate(Path file) throws IOException {
		String name = file.getFileName().toString();
		Matcher m = BASE32_FOLLOWED_BY_UNDERSCORE_PATTERN.matcher(name);
		if (m.find(0)) {
			String base32 = m.group(1);
			String suffix = name.substring(m.end());
			String renamed = NEW_FOLDER_PREFIX + base32 + suffix;
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
		assert oldName.endsWith(LONG_FILENAME_EXT);
		String oldNameBase = StringUtils.removeEnd(oldName, LONG_FILENAME_EXT);

		Matcher m = BASE32_PATTERN.matcher(oldNameBase);
		if (m.find(0)) {
			String oldNameBase32 = m.group(1);
			String oldNameSuffix = oldNameBase.substring(m.end());
			String oldCanonicalName = oldNameBase32 + LONG_FILENAME_EXT;

			Path oldMetadataFile = metadataDir.resolve(oldCanonicalName.substring(0, 2)).resolve(oldCanonicalName.substring(2, 4)).resolve(oldCanonicalName);
			if (!Files.isReadable(oldMetadataFile)) {
				LOG.warn("Found uninflatable long file name. Expected: {}", oldMetadataFile);
				return;
			}

			String oldLongName = new String(Files.readAllBytes(oldMetadataFile), UTF_8);
			if (oldLongName.endsWith(OLD_FOLDER_SUFFIX)) {
				String newLongName = NEW_FOLDER_PREFIX + StringUtils.removeEnd(oldLongName, OLD_FOLDER_SUFFIX);
				String newCanonicalBase32 = base32.encode(sha1.digest(newLongName.getBytes(UTF_8)));
				String newCanonicalName = newCanonicalBase32 + LONG_FILENAME_EXT;
				Path newMetadataFile = metadataDir.resolve(newCanonicalName.substring(0, 2)).resolve(newCanonicalName.substring(2, 4)).resolve(newCanonicalName);
				String newName = newCanonicalBase32 + oldNameSuffix + LONG_FILENAME_EXT;
				Path newPath = path.resolveSibling(newName);
				Files.move(path, newPath);
				Files.createDirectories(newMetadataFile.getParent());
				Files.write(newMetadataFile, newLongName.getBytes(UTF_8));
				LOG.info("Renaming {} to {}.", path, newName);
				LOG.info("Creating {}.", newMetadataFile);
			}
		}
	}

}

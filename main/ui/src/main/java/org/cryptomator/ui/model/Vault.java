/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui.model;

import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.cryptomator.common.Optionals;
import org.cryptomator.crypto.engine.InvalidPassphraseException;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.crypto.CryptoFileSystemDelegate;
import org.cryptomator.filesystem.crypto.CryptoFileSystemFactory;
import org.cryptomator.filesystem.nio.NioFileSystem;
import org.cryptomator.filesystem.stats.StatsFileSystem;
import org.cryptomator.frontend.CommandFailedException;
import org.cryptomator.frontend.Frontend;
import org.cryptomator.frontend.Frontend.MountParam;
import org.cryptomator.frontend.FrontendCreationFailedException;
import org.cryptomator.frontend.FrontendFactory;
import org.cryptomator.ui.util.DeferredClosable;
import org.cryptomator.ui.util.DeferredCloser;
import org.cryptomator.ui.util.FXThreads;

import com.google.common.collect.ImmutableMap;

import dagger.Lazy;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Vault implements Serializable, CryptoFileSystemDelegate {

	private static final long serialVersionUID = 3754487289683599469L;

	public static final String VAULT_FILE_EXTENSION = ".cryptomator";

	@Deprecated
	public static final String VAULT_MASTERKEY_FILE = "masterkey.cryptomator";

	private final Path path;
	private final Lazy<FrontendFactory> frontendFactory;
	private final DeferredCloser closer;
	private final CryptoFileSystemFactory cryptoFileSystemFactory;
	private final ObjectProperty<Boolean> unlocked = new SimpleObjectProperty<Boolean>(this, "unlocked", Boolean.FALSE);
	private final ObservableList<String> namesOfResourcesWithInvalidMac = FXThreads.observableListOnMainThread(FXCollections.observableArrayList());
	private final Set<String> whitelistedResourcesWithInvalidMac = new HashSet<>();

	private String mountName;
	private Character winDriveLetter;
	private Optional<StatsFileSystem> statsFileSystem = Optional.empty();
	private DeferredClosable<Frontend> filesystemFrontend = DeferredClosable.empty();

	/**
	 * Package private constructor, use {@link VaultFactory}.
	 */
	Vault(Path vaultDirectoryPath, Lazy<FrontendFactory> frontendFactory, CryptoFileSystemFactory cryptoFileSystemFactory, DeferredCloser closer) {
		this.path = vaultDirectoryPath;
		this.frontendFactory = frontendFactory;
		this.closer = closer;
		this.cryptoFileSystemFactory = cryptoFileSystemFactory;

		try {
			setMountName(getName());
		} catch (IllegalArgumentException e) {
			// mount name needs to be set by the user explicitly later
		}
	}

	/*
	 * ******************************************************************************
	 * Commands
	 ********************************************************************************/

	public void create(CharSequence passphrase) throws IOException {
		try {
			FileSystem fs = NioFileSystem.rootedAt(path);
			if (fs.children().count() > 0) {
				throw new FileAlreadyExistsException(null, null, "Vault location not empty.");
			}
			cryptoFileSystemFactory.initializeNew(fs, passphrase);
		} catch (UncheckedIOException e) {
			throw new IOException(e);
		}
	}

	public void changePassphrase(CharSequence oldPassphrase, CharSequence newPassphrase) throws IOException, InvalidPassphraseException {
		try {
			FileSystem fs = NioFileSystem.rootedAt(path);
			cryptoFileSystemFactory.changePassphrase(fs, oldPassphrase, newPassphrase);
		} catch (UncheckedIOException e) {
			throw new IOException(e);
		}
	}

	public synchronized void activateFrontend(CharSequence passphrase) throws FrontendCreationFailedException {
		try {
			FileSystem fs = NioFileSystem.rootedAt(path);
			FileSystem cryptoFs = cryptoFileSystemFactory.unlockExisting(fs, passphrase, this);
			StatsFileSystem statsFs = new StatsFileSystem(cryptoFs);
			statsFileSystem = Optional.of(statsFs);
			String contextPath = StringUtils.prependIfMissing(mountName, "/");
			Frontend frontend = frontendFactory.get().create(statsFs, contextPath);
			filesystemFrontend = closer.closeLater(frontend);
			unlocked.set(true);
		} catch (UncheckedIOException e) {
			throw new FrontendCreationFailedException(e);
		}
	}

	public synchronized void deactivateFrontend() {
		filesystemFrontend.close();
		statsFileSystem = Optional.empty();
		unlocked.set(false);
	}

	private Map<MountParam, Optional<String>> getMountParams() {
		return ImmutableMap.of( //
				MountParam.MOUNT_NAME, Optional.ofNullable(mountName), //
				MountParam.WIN_DRIVE_LETTER, Optional.ofNullable(CharUtils.toString(winDriveLetter)) //
		);
	}

	public Boolean mount() {
		try {
			Optionals.ifPresent(filesystemFrontend.get(), f -> {
				f.mount(getMountParams());
			});
			return true;
		} catch (CommandFailedException e) {
			return false;
		}
	}

	public void reveal() throws CommandFailedException {
		Optionals.ifPresent(filesystemFrontend.get(), Frontend::reveal);
	}

	public void unmount() throws CommandFailedException {
		Optionals.ifPresent(filesystemFrontend.get(), Frontend::unmount);
	}

	/*
	 * ******************************************************************************
	 * Delegate methods
	 ********************************************************************************/

	@Override
	public void authenticationFailed(String cleartextPath) {
		namesOfResourcesWithInvalidMac.add(cleartextPath);
	}

	@Override
	public boolean shouldSkipAuthentication(String cleartextPath) {
		return namesOfResourcesWithInvalidMac.contains(cleartextPath);
	}

	/*
	 * ******************************************************************************
	 * Getter/Setter
	 ********************************************************************************/

	public Path getPath() {
		return path;
	}

	/**
	 * @return Directory name without preceeding path components and file extension
	 */
	public String getName() {
		return StringUtils.removeEnd(path.getFileName().toString(), VAULT_FILE_EXTENSION);
	}

	public boolean isValidVaultDirectory() {
		return Files.isDirectory(path) && path.getFileName().toString().endsWith(VAULT_FILE_EXTENSION);
	}

	public boolean containsMasterKey() throws IOException {
		final Path masterKeyPath = path.resolve(VAULT_MASTERKEY_FILE);
		return Files.isRegularFile(masterKeyPath);
	}

	public ObjectProperty<Boolean> unlockedProperty() {
		return unlocked;
	}

	public boolean isUnlocked() {
		return unlocked.get();
	}

	public ObservableList<String> getNamesOfResourcesWithInvalidMac() {
		return namesOfResourcesWithInvalidMac;
	}

	public Set<String> getWhitelistedResourcesWithInvalidMac() {
		return whitelistedResourcesWithInvalidMac;
	}

	public long pollBytesRead() {
		return statsFileSystem.map(StatsFileSystem::getThenResetBytesRead).orElse(0l);
	}

	public long pollBytesWritten() {
		return statsFileSystem.map(StatsFileSystem::getThenResetBytesWritten).orElse(0l);
	}

	/**
	 * Tries to form a similar string using the regular latin alphabet.
	 * 
	 * @param string
	 * @return a string composed of a-z, A-Z, 0-9, and _.
	 */
	public static String normalize(String string) {
		String normalized = Normalizer.normalize(string, Form.NFD);
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < normalized.length(); i++) {
			char c = normalized.charAt(i);
			if (Character.isWhitespace(c)) {
				if (builder.length() == 0 || builder.charAt(builder.length() - 1) != '_') {
					builder.append('_');
				}
			} else if (c < 127 && Character.isLetterOrDigit(c)) {
				builder.append(c);
			} else if (c < 127) {
				if (builder.length() == 0 || builder.charAt(builder.length() - 1) != '_') {
					builder.append('_');
				}
			}
		}
		return builder.toString();
	}

	public String getMountName() {
		return mountName;
	}

	/**
	 * sets the mount name while normalizing it
	 * 
	 * @param mountName
	 * @throws IllegalArgumentException if the name is empty after normalization
	 */
	public void setMountName(String mountName) throws IllegalArgumentException {
		mountName = normalize(mountName);
		if (StringUtils.isEmpty(mountName)) {
			throw new IllegalArgumentException("mount name is empty");
		}
		this.mountName = mountName;
	}

	public Character getWinDriveLetter() {
		return winDriveLetter;
	}

	public void setWinDriveLetter(Character winDriveLetter) {
		this.winDriveLetter = winDriveLetter;
	}

	/*
	 * ******************************************************************************
	 * Hashcode / Equals
	 ********************************************************************************/

	@Override
	public int hashCode() {
		return path.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Vault) {
			final Vault other = (Vault) obj;
			return this.path.equals(other.path);
		} else {
			return false;
		}
	}

}

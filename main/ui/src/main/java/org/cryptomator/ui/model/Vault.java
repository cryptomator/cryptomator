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
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.LazyInitializer;
import org.cryptomator.common.Optionals;
import org.cryptomator.crypto.engine.InvalidPassphraseException;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.crypto.CryptoFileSystemDelegate;
import org.cryptomator.filesystem.crypto.CryptoFileSystemFactory;
import org.cryptomator.filesystem.nio.NioFileSystem;
import org.cryptomator.filesystem.shortening.ShorteningFileSystemFactory;
import org.cryptomator.filesystem.stats.StatsFileSystem;
import org.cryptomator.frontend.CommandFailedException;
import org.cryptomator.frontend.Frontend;
import org.cryptomator.frontend.Frontend.MountParam;
import org.cryptomator.frontend.FrontendCreationFailedException;
import org.cryptomator.frontend.FrontendFactory;
import org.cryptomator.ui.settings.Settings;
import org.cryptomator.ui.util.DeferredClosable;
import org.cryptomator.ui.util.DeferredCloser;
import org.cryptomator.ui.util.FXThreads;
import org.fxmisc.easybind.EasyBind;

import com.google.common.collect.ImmutableMap;

import javafx.application.Platform;
import javafx.beans.binding.Binding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Vault implements CryptoFileSystemDelegate {

	public static final String VAULT_FILE_EXTENSION = ".cryptomator";

	private final ObjectProperty<Path> path;
	private final DeferredCloser closer;
	private final ShorteningFileSystemFactory shorteningFileSystemFactory;
	private final CryptoFileSystemFactory cryptoFileSystemFactory;
	private final BooleanProperty unlocked = new SimpleBooleanProperty();
	private final ObservableList<String> namesOfResourcesWithInvalidMac = FXThreads.observableListOnMainThread(FXCollections.observableArrayList());
	private final Set<String> whitelistedResourcesWithInvalidMac = new HashSet<>();
	private final AtomicReference<FileSystem> nioFileSystem = new AtomicReference<>();

	private String mountName;
	private Character winDriveLetter;
	private Optional<StatsFileSystem> statsFileSystem = Optional.empty();
	private DeferredClosable<Frontend> filesystemFrontend = DeferredClosable.empty();

	/**
	 * Package private constructor, use {@link VaultFactory}.
	 */
	Vault(Path vaultDirectoryPath, ShorteningFileSystemFactory shorteningFileSystemFactory, CryptoFileSystemFactory cryptoFileSystemFactory, DeferredCloser closer) {
		this.path = new SimpleObjectProperty<Path>(vaultDirectoryPath);
		this.closer = closer;
		this.shorteningFileSystemFactory = shorteningFileSystemFactory;
		this.cryptoFileSystemFactory = cryptoFileSystemFactory;

		try {
			setMountName(name().getValue());
		} catch (IllegalArgumentException e) {
			// mount name needs to be set by the user explicitly later
		}
	}

	private FileSystem getNioFileSystem() {
		return LazyInitializer.initializeLazily(nioFileSystem, () -> NioFileSystem.rootedAt(path.getValue()));
	}

	// ******************************************************************************
	// Commands
	// ********************************************************************************/

	public void create(CharSequence passphrase) throws IOException {
		try {
			FileSystem fs = getNioFileSystem();
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
			cryptoFileSystemFactory.changePassphrase(getNioFileSystem(), oldPassphrase, newPassphrase);
		} catch (UncheckedIOException e) {
			throw new IOException(e);
		}
	}

	public synchronized void activateFrontend(FrontendFactory frontendFactory, Settings settings, CharSequence passphrase) throws FrontendCreationFailedException {
		boolean success = false;
		try {
			FileSystem fs = getNioFileSystem();
			FileSystem shorteningFs = shorteningFileSystemFactory.get(fs);
			FileSystem cryptoFs = cryptoFileSystemFactory.unlockExisting(shorteningFs, passphrase, this);
			StatsFileSystem statsFs = new StatsFileSystem(cryptoFs);
			statsFileSystem = Optional.of(statsFs);
			String contextPath = StringUtils.prependIfMissing(mountName, "/");
			Frontend frontend = frontendFactory.create(statsFs, contextPath);
			filesystemFrontend = closer.closeLater(frontend);
			frontend.mount(getMountParams(settings));
			success = true;
		} catch (UncheckedIOException | CommandFailedException e) {
			throw new FrontendCreationFailedException(e);
		} finally {
			// unlocked is a observable property and should only be changed by the FX application thread
			final boolean finalSuccess = success;
			Platform.runLater(() -> unlocked.set(finalSuccess));
		}
	}

	public synchronized void deactivateFrontend() {
		filesystemFrontend.close();
		statsFileSystem = Optional.empty();
		Platform.runLater(() -> unlocked.set(false));
	}

	private Map<MountParam, Optional<String>> getMountParams(Settings settings) {
		String hostname = SystemUtils.IS_OS_WINDOWS && settings.shouldUseIpv6() ? "0--1.ipv6-literal.net" : "localhost";
		return ImmutableMap.of( //
				MountParam.MOUNT_NAME, Optional.ofNullable(mountName), //
				MountParam.WIN_DRIVE_LETTER, Optional.ofNullable(CharUtils.toString(winDriveLetter)), //
				MountParam.HOSTNAME, Optional.of(hostname) //
		);
	}

	public void reveal() throws CommandFailedException {
		Optionals.ifPresent(filesystemFrontend.get(), Frontend::reveal);
	}

	public void unmount() throws CommandFailedException {
		Optionals.ifPresent(filesystemFrontend.get(), Frontend::unmount);
	}

	public boolean needsUpgrade() {
		return availableUpgrade().isPresent();
	}

	public Optional<UpgradeInstruction> availableUpgrade() {
		return Arrays.stream(UpgradeInstruction.AVAILABLE_INSTRUCTIONS).filter(instruction -> {
			return instruction.isApplicable(this);
		}).findAny();
	}

	// ******************************************************************************
	// Delegate methods
	// ********************************************************************************/

	@Override
	public void authenticationFailed(String cleartextPath) {
		namesOfResourcesWithInvalidMac.add(cleartextPath);
	}

	@Override
	public boolean shouldSkipAuthentication(String cleartextPath) {
		return whitelistedResourcesWithInvalidMac.contains(cleartextPath);
	}

	// ******************************************************************************
	// Getter/Setter
	// *******************************************************************************/

	public String getWebDavUrl() {
		return filesystemFrontend.get().map(Frontend::getWebDavUrl).orElseThrow(IllegalStateException::new);
	}

	void setPath(Path path) {
		this.path.set(path);
		this.nioFileSystem.set(null);
	}

	public ReadOnlyObjectProperty<Path> path() {
		return path;
	}

	public Binding<String> displayablePath() {
		Path homeDir = Paths.get(SystemUtils.USER_HOME);
		return EasyBind.map(path, p -> {
			if (p.startsWith(homeDir)) {
				Path relativePath = homeDir.relativize(p);
				String homePrefix = SystemUtils.IS_OS_WINDOWS ? "~\\" : "~/";
				return homePrefix + relativePath.toString();
			} else {
				return path.getValue().toString();
			}
		});
	}

	/**
	 * @return Directory name without preceeding path components and file extension
	 */
	public Binding<String> name() {
		return EasyBind.map(path, p -> p.getFileName().toString());
	}

	public boolean doesVaultDirectoryExist() {
		return Files.isDirectory(path.getValue());
	}

	public boolean isValidVaultDirectory() {
		try {
			return doesVaultDirectoryExist() && cryptoFileSystemFactory.isValidVaultStructure(getNioFileSystem());
		} catch (UncheckedIOException e) {
			return false;
		}
	}

	public BooleanProperty unlockedProperty() {
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

	// ******************************************************************************
	// Hashcode / Equals
	// *******************************************************************************/

	@Override
	public int hashCode() {
		return path.getValue().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Vault) {
			final Vault other = (Vault) obj;
			return this.path.getValue().equals(other.path.getValue());
		} else {
			return false;
		}
	}

}
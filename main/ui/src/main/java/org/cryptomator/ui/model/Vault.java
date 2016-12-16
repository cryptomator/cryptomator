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
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.LazyInitializer;
import org.cryptomator.cryptofs.CryptoFileSystem;
import org.cryptomator.cryptofs.CryptoFileSystemProperties;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.frontend.webdav.WebDavServer;
import org.cryptomator.ui.model.VaultModule.PerVault;
import org.cryptomator.ui.settings.VaultSettings;
import org.cryptomator.ui.util.DeferredCloser;
import org.fxmisc.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.beans.binding.Binding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;

@PerVault
public class Vault {

	private static final Logger LOG = LoggerFactory.getLogger(Vault.class);
	public static final String VAULT_FILE_EXTENSION = ".cryptomator";

	private final VaultSettings vaultSettings;
	private final WebDavServer server;
	private final DeferredCloser closer;

	private final BooleanProperty unlocked = new SimpleBooleanProperty();
	private final BooleanProperty mounted = new SimpleBooleanProperty();

	private final AtomicReference<CryptoFileSystem> cryptoFileSystem = new AtomicReference<>();

	@Inject
	Vault(VaultSettings vaultSettings, WebDavServer server, DeferredCloser closer) {
		this.vaultSettings = vaultSettings;
		this.server = server;
		this.closer = closer;
		try {
			setMountName(name().getValue());
		} catch (IllegalArgumentException e) {
			// mount name needs to be set by the user explicitly later
		}
	}

	// ******************************************************************************
	// Commands
	// ********************************************************************************/

	private CryptoFileSystem getCryptoFileSystem(CharSequence passphrase) throws IOException {
		return LazyInitializer.initializeLazily(cryptoFileSystem, () -> createCryptoFileSystem(passphrase), IOException.class);
	}

	private CryptoFileSystem createCryptoFileSystem(CharSequence passphrase) throws IOException {
		CryptoFileSystemProperties fsProps = CryptoFileSystemProperties.cryptoFileSystemProperties().withPassphrase(passphrase).build();
		CryptoFileSystem fs = CryptoFileSystemProvider.newFileSystem(getPath(), fsProps);
		closer.closeLater(fs);
		return fs;
	}

	public void create(CharSequence passphrase) throws IOException {
		getCryptoFileSystem(passphrase);
		// TODO and now?
	}

	public void changePassphrase(CharSequence oldPassphrase, CharSequence newPassphrase) throws IOException, InvalidPassphraseException {
		// TODO implement
	}

	public synchronized void activateFrontend(CharSequence passphrase) {
		boolean launchSuccess = false;
		boolean mountSuccess = false;
		try {
			FileSystem fs = getCryptoFileSystem(passphrase);
			if (!server.isRunning()) {
				server.start();
			}
			server.startWebDavServlet(fs.getPath("/"), vaultSettings.getId());
		} catch (IOException e) {
			LOG.error("Unable to provide frontend", e);
		} finally {
			// unlocked is a observable property and should only be changed by the FX application thread
			Platform.runLater(() -> {
				unlocked.set(launchSuccess);
				mounted.set(mountSuccess);
			});
		}
	}

	public synchronized void deactivateFrontend() throws Exception {
		CryptoFileSystem fs = cryptoFileSystem.getAndSet(null);
		if (fs != null) {
			fs.close();
		}
		// TODO overheadhunter remove servlet from server
		Platform.runLater(() -> {
			mounted.set(false);
			unlocked.set(false);
		});
	}

	public synchronized void reveal() {
		// TODO implement
	}

	// ******************************************************************************
	// Getter/Setter
	// *******************************************************************************/

	public VaultSettings getVaultSettings() {
		return vaultSettings;
	}

	public synchronized String getWebDavUrl() {
		// TODO implement
		return "http://localhost/not/implemented";
	}

	public Path getPath() {
		return vaultSettings.pathProperty().getValue();
	}

	public Binding<String> displayablePath() {
		Path homeDir = Paths.get(SystemUtils.USER_HOME);
		return EasyBind.map(vaultSettings.pathProperty(), p -> {
			if (p.startsWith(homeDir)) {
				Path relativePath = homeDir.relativize(p);
				String homePrefix = SystemUtils.IS_OS_WINDOWS ? "~\\" : "~/";
				return homePrefix + relativePath.toString();
			} else {
				return p.toString();
			}
		});
	}

	/**
	 * @return Directory name without preceeding path components and file extension
	 */
	public Binding<String> name() {
		return EasyBind.map(vaultSettings.pathProperty(), p -> p.getFileName().toString());
	}

	public boolean doesVaultDirectoryExist() {
		return Files.isDirectory(getPath());
	}

	public boolean isValidVaultDirectory() {
		try {
			return doesVaultDirectoryExist(); // TODO: && cryptoFileSystemFactory.isValidVaultStructure(getNioFileSystem());
		} catch (UncheckedIOException e) {
			return false;
		}
	}

	public BooleanProperty unlockedProperty() {
		return unlocked;
	}

	public BooleanProperty mountedProperty() {
		return mounted;
	}

	public boolean isUnlocked() {
		return unlocked.get();
	}

	public boolean isMounted() {
		return mounted.get();
	}

	public ObservableList<String> getNamesOfResourcesWithInvalidMac() {
		// TODO overheadhunter implement.
		return null;
	}

	public Set<String> getWhitelistedResourcesWithInvalidMac() {
		// TODO overheadhunter implement.
		return null;
	}

	public long pollBytesRead() {
		// TODO overheadhunter implement.
		return 0l;
	}

	public long pollBytesWritten() {
		// TODO overheadhunter implement.
		return 0l;
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
		return vaultSettings.getMountName();
	}

	/**
	 * sets the mount name while normalizing it
	 * 
	 * @param mountName
	 * @throws IllegalArgumentException if the name is empty after normalization
	 */
	public void setMountName(String mountName) throws IllegalArgumentException {
		String normalized = normalize(mountName);
		if (StringUtils.isEmpty(normalized)) {
			throw new IllegalArgumentException("mount name is empty");
		} else {
			vaultSettings.setMountName(normalized);
		}
	}

	public Character getWinDriveLetter() {
		if (vaultSettings.getWinDriveLetter() == null) {
			return null;
		} else {
			return vaultSettings.getWinDriveLetter().charAt(0);
		}
	}

	public void setWinDriveLetter(Character winDriveLetter) {
		if (winDriveLetter == null) {
			vaultSettings.setWinDriveLetter(null);
		} else {
			vaultSettings.setWinDriveLetter(String.valueOf(winDriveLetter));
		}
	}

	public String getId() {
		return vaultSettings.getId();
	}

	// ******************************************************************************
	// Hashcode / Equals
	// *******************************************************************************/

	@Override
	public int hashCode() {
		return Objects.hash(vaultSettings);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Vault && obj.getClass().equals(this.getClass())) {
			final Vault other = (Vault) obj;
			return Objects.equals(this.vaultSettings, other.vaultSettings);
		} else {
			return false;
		}
	}

}
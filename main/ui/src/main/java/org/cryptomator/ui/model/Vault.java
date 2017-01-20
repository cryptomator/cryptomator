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
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.LazyInitializer;
import org.cryptomator.cryptofs.CryptoFileSystem;
import org.cryptomator.cryptofs.CryptoFileSystemProperties;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptolib.api.CryptoException;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.frontend.webdav.WebDavServer;
import org.cryptomator.frontend.webdav.mount.Mounter.CommandFailedException;
import org.cryptomator.frontend.webdav.mount.Mounter.Mount;
import org.cryptomator.frontend.webdav.servlet.WebDavServletController;
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

@PerVault
public class Vault {

	private static final Logger LOG = LoggerFactory.getLogger(Vault.class);
	private static final String MASTERKEY_FILENAME = "masterkey.cryptomator";

	private final VaultSettings vaultSettings;
	private final WebDavServer server;
	private final DeferredCloser closer;
	private final BooleanProperty unlocked = new SimpleBooleanProperty();
	private final BooleanProperty mounted = new SimpleBooleanProperty();
	private final AtomicReference<CryptoFileSystem> cryptoFileSystem = new AtomicReference<>();

	private WebDavServletController servlet;
	private Mount mount;

	@Inject
	Vault(VaultSettings vaultSettings, WebDavServer server, DeferredCloser closer) {
		this.vaultSettings = vaultSettings;
		this.server = server;
		this.closer = closer;
	}

	// ******************************************************************************
	// Commands
	// ********************************************************************************/

	private CryptoFileSystem getCryptoFileSystem(CharSequence passphrase) throws IOException {
		return LazyInitializer.initializeLazily(cryptoFileSystem, () -> createCryptoFileSystem(passphrase), IOException.class);
	}

	private CryptoFileSystem createCryptoFileSystem(CharSequence passphrase) throws IOException, CryptoException {
		CryptoFileSystemProperties fsProps = CryptoFileSystemProperties.cryptoFileSystemProperties() //
				.withPassphrase(passphrase) //
				.withMasterkeyFilename(MASTERKEY_FILENAME) //
				.build();
		CryptoFileSystem fs = CryptoFileSystemProvider.newFileSystem(getPath(), fsProps);
		closer.closeLater(fs);
		return fs;
	}

	public void create(CharSequence passphrase) throws IOException {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(getPath())) {
			for (Path file : stream) {
				if (!file.getFileName().toString().startsWith(".")) {
					throw new DirectoryNotEmptyException(getPath().toString());
				}
			}
		}
		if (!isValidVaultDirectory()) {
			getCryptoFileSystem(passphrase); // implicitly creates a non-existing vault
		} else {
			throw new FileAlreadyExistsException(getPath().toString());
		}
	}

	public void changePassphrase(CharSequence oldPassphrase, CharSequence newPassphrase) throws IOException, InvalidPassphraseException {
		CryptoFileSystemProvider.changePassphrase(getPath(), MASTERKEY_FILENAME, oldPassphrase, newPassphrase);
	}

	public synchronized void activateFrontend(CharSequence passphrase) {
		boolean unlockSuccess = false;
		boolean mountSuccess = false;
		try {
			FileSystem fs = getCryptoFileSystem(passphrase);
			if (!server.isRunning()) {
				server.start();
			}
			servlet = server.createWebDavServlet(fs.getPath("/"), vaultSettings.getId() + "/" + vaultSettings.getMountName());
			servlet.start();
			unlockSuccess = true;

			mount = servlet.mount(Collections.emptyMap());
			mountSuccess = true;
		} catch (IOException e) {
			LOG.error("Unable to provide filesystem", e);
		} catch (CommandFailedException e) {
			LOG.error("Unable to mount filesystem", e);
		} finally {
			// unlocked is a observable property and should only be changed by the FX application thread
			final boolean finalUnlockSuccess = unlockSuccess;
			final boolean finalMountSuccess = mountSuccess;
			Platform.runLater(() -> {
				unlocked.set(finalUnlockSuccess);
				mounted.set(finalMountSuccess);
			});
		}
	}

	public synchronized void deactivateFrontend() throws Exception {
		if (mount != null) {
			mount.unmount();
		}
		if (servlet != null) {
			servlet.stop();
		}
		CryptoFileSystem fs = cryptoFileSystem.getAndSet(null);
		if (fs != null) {
			fs.close();
		}
		Platform.runLater(() -> {
			mounted.set(false);
			unlocked.set(false);
		});
	}

	public void reveal() throws CommandFailedException {
		if (mount != null) {
			mount.reveal();
		}
	}

	// ******************************************************************************
	// Getter/Setter
	// *******************************************************************************/

	public VaultSettings getVaultSettings() {
		return vaultSettings;
	}

	public String getWebDavUrl() {
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
		return EasyBind.map(vaultSettings.pathProperty(), Path::getFileName).map(Path::toString);
	}

	public boolean doesVaultDirectoryExist() {
		return Files.isDirectory(getPath());
	}

	public boolean isValidVaultDirectory() {
		return CryptoFileSystemProvider.containsVault(getPath(), MASTERKEY_FILENAME);
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

	public long pollBytesRead() {
		CryptoFileSystem fs = cryptoFileSystem.get();
		if (fs != null) {
			return fs.getStats().pollBytesRead();
		} else {
			return 0l;
		}
	}

	public long pollBytesWritten() {
		CryptoFileSystem fs = cryptoFileSystem.get();
		if (fs != null) {
			return fs.getStats().pollBytesWritten();
		} else {
			return 0l;
		}
	}

	public String getMountName() {
		return vaultSettings.getMountName();
	}

	public void setMountName(String mountName) throws IllegalArgumentException {
		if (StringUtils.isBlank(mountName)) {
			throw new IllegalArgumentException("mount name is empty");
		} else {
			vaultSettings.setMountName(mountName);
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
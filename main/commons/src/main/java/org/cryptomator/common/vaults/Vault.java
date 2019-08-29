/*******************************************************************************
 * Copyright (c) 2016, 2017 Sebastian Stenzel and others.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.common.vaults;

import com.google.common.base.Strings;
import javafx.beans.Observable;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.LazyInitializer;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.cryptofs.CryptoFileSystem;
import org.cryptomator.cryptofs.CryptoFileSystemProperties;
import org.cryptomator.cryptofs.CryptoFileSystemProperties.FileSystemFlags;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptolib.api.CryptoException;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.fxmisc.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

@PerVault
public class Vault {

	public static final Predicate<Vault> NOT_LOCKED = hasState(VaultState.LOCKED).negate();
	private static final Logger LOG = LoggerFactory.getLogger(Vault.class);
	private static final String MASTERKEY_FILENAME = "masterkey.cryptomator";
	private static final Path HOME_DIR = Paths.get(SystemUtils.USER_HOME);

	private final VaultSettings vaultSettings;
	private final Provider<Volume> volumeProvider;
	private final StringBinding defaultMountFlags;
	private final AtomicReference<CryptoFileSystem> cryptoFileSystem;
	private final ObjectProperty<VaultState> state;
	private final VaultStats stats;
	private final ObjectProperty<Path> accessPoint = new SimpleObjectProperty<>(Path.of(""));
	private final StringBinding displayableName;
	private final StringBinding displayablePath;
	private final BooleanBinding locked;
	private final BooleanBinding processing;
	private final BooleanBinding unlocked;

	private Volume volume;

	@Inject
	Vault(VaultSettings vaultSettings, Provider<Volume> volumeProvider, @DefaultMountFlags StringBinding defaultMountFlags, AtomicReference<CryptoFileSystem> cryptoFileSystem, ObjectProperty<VaultState> state, VaultStats stats) {
		this.vaultSettings = vaultSettings;
		this.volumeProvider = volumeProvider;
		this.defaultMountFlags = defaultMountFlags;
		this.cryptoFileSystem = cryptoFileSystem;
		this.state = state;
		this.stats = stats;
		this.displayableName = Bindings.createStringBinding(this::getDisplayableName, vaultSettings.path());
		this.displayablePath = Bindings.createStringBinding(this::getDisplayablePath, vaultSettings.path());
		this.locked = Bindings.createBooleanBinding(this::isLocked, state);
		this.processing = Bindings.createBooleanBinding(this::isProcessing, state);
		this.unlocked = Bindings.createBooleanBinding(this::isUnlocked, state);
		this.state.addListener(this::setAccessPoint);
	}

	// ******************************************************************************
	// Commands
	// ********************************************************************************/

	private CryptoFileSystem getCryptoFileSystem(CharSequence passphrase) throws NoSuchFileException, IOException, InvalidPassphraseException, CryptoException {
		return LazyInitializer.initializeLazily(cryptoFileSystem, () -> unlockCryptoFileSystem(passphrase), IOException.class);
	}

	private CryptoFileSystem unlockCryptoFileSystem(CharSequence passphrase) throws NoSuchFileException, IOException, InvalidPassphraseException, CryptoException {
		List<FileSystemFlags> flags = new ArrayList<>();
		if (vaultSettings.usesReadOnlyMode().get()) {
			flags.add(FileSystemFlags.READONLY);
		}
		CryptoFileSystemProperties fsProps = CryptoFileSystemProperties.cryptoFileSystemProperties() //
				.withPassphrase(passphrase) //
				.withFlags(flags) //
				.withMasterkeyFilename(MASTERKEY_FILENAME) //
				.build();
		return CryptoFileSystemProvider.newFileSystem(getPath(), fsProps);
	}

	public void create(CharSequence passphrase) throws IOException {
		if (!isValidVaultDirectory()) {
			CryptoFileSystemProvider.initialize(getPath(), MASTERKEY_FILENAME, passphrase);
		} else {
			throw new FileAlreadyExistsException(getPath().toString());
		}
	}

	public void changePassphrase(CharSequence oldPassphrase, CharSequence newPassphrase) throws IOException, InvalidPassphraseException {
		CryptoFileSystemProvider.changePassphrase(getPath(), MASTERKEY_FILENAME, oldPassphrase, newPassphrase);
	}

	public synchronized void unlock(CharSequence passphrase) throws CryptoException, IOException, Volume.VolumeException {
		if (vaultSettings.usesIndividualMountPath().get() && Strings.isNullOrEmpty(vaultSettings.individualMountPath().get())) {
			throw new NotDirectoryException("");
		}
		CryptoFileSystem fs = getCryptoFileSystem(passphrase);
		volume = volumeProvider.get();
		volume.mount(fs, getEffectiveMountFlags());
	}

	public synchronized void lock(boolean forced) throws Volume.VolumeException {
		if (forced && volume.supportsForcedUnmount()) {
			volume.unmountForced();
		} else {
			volume.unmount();
		}
		CryptoFileSystem fs = cryptoFileSystem.getAndSet(null);
		if (fs != null) {
			try {
				fs.close();
			} catch (IOException e) {
				LOG.error("Error closing file system.", e);
			}
		}
	}

	/**
	 * Ejects any mounted drives and locks this vault. no-op if this vault is currently locked.
	 */
	public void prepareForShutdown() {
		try {
			lock(false);
		} catch (Volume.VolumeException e) {
			if (volume.supportsForcedUnmount()) {
				try {
					lock(true);
				} catch (Volume.VolumeException e1) {
					LOG.warn("Failed to force lock vault.", e1);
				}
			} else {
				LOG.warn("Failed to gracefully lock vault.", e);
			}
		}
	}

	public void reveal() throws Volume.VolumeException {
		volume.reveal();
	}

	public static Predicate<Vault> hasState(VaultState state) {
		return vault -> {
			return vault.getState() == state;
		};
	}

	// ******************************************************************************
	// Observable Properties
	// *******************************************************************************

	public ObjectProperty<VaultState> stateProperty() {
		return state;
	}

	public VaultState getState() {
		return state.get();
	}

	public void setState(VaultState value) {
		state.setValue(value);
	}

	public BooleanBinding lockedProperty() {
		return locked;
	}

	public boolean isLocked() {
		return state.get() == VaultState.LOCKED;
	}

	public BooleanBinding processingProperty() {
		return processing;
	}

	public boolean isProcessing() {
		return state.get() == VaultState.PROCESSING;
	}

	public BooleanBinding unlockedProperty() {
		return unlocked;
	}

	public boolean isUnlocked() {
		return state.get() == VaultState.UNLOCKED;
	}

	public StringBinding displayableNameProperty() {
		return displayableName;
	}

	public String getDisplayableName() {
		Path p = vaultSettings.path().get();
		return p.getFileName().toString();
	}

	public ObjectProperty<Path> accessPointProperty() {
		return accessPoint;
	}

	public Path getAccessPoint() {
		return accessPoint.get();
	}

	private void setAccessPoint(Observable obs) {
		if (this.getState() == VaultState.UNLOCKED) {
			accessPoint.setValue(volume.getMountPointSafe().get());
		} else {
			accessPoint.setValue(Path.of(""));
		}
	}

	public StringBinding displayablePathProperty() {
		return displayablePath;
	}

	public String getDisplayablePath() {
		Path p = vaultSettings.path().get();
		if (p.startsWith(HOME_DIR)) {
			Path relativePath = HOME_DIR.relativize(p);
			String homePrefix = SystemUtils.IS_OS_WINDOWS ? "~\\" : "~/";
			return homePrefix + relativePath.toString();
		} else {
			return p.toString();
		}
	}

	// ******************************************************************************
	// Getter/Setter
	// *******************************************************************************/

	public VaultStats getStats() {
		return stats;
	}

	public Observable[] observables() {
		return new Observable[]{state};
	}

	public VaultSettings getVaultSettings() {
		return vaultSettings;
	}

	public Path getPath() {
		return vaultSettings.path().getValue();
	}

	/**
	 * @deprecated use displayablePathProperty() instead
	 */
	@Deprecated(forRemoval = true, since = "1.5.0")
	public Binding<String> displayablePath() {
		Path homeDir = Paths.get(SystemUtils.USER_HOME);
		return EasyBind.map(vaultSettings.path(), p -> {
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
	 * @deprecated use nameProperty() instead
	 */
	@Deprecated(forRemoval = true, since = "1.5.0")
	public Binding<String> name() {
		return EasyBind.map(vaultSettings.path(), Path::getFileName).map(Path::toString);
	}

	public boolean doesVaultDirectoryExist() {
		return Files.isDirectory(getPath());
	}

	public boolean isValidVaultDirectory() {
		return CryptoFileSystemProvider.containsVault(getPath(), MASTERKEY_FILENAME);
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

	public String getCustomMountPath() {
		return vaultSettings.individualMountPath().getValueSafe();
	}

	public void setCustomMountPath(String mountPath) {
		vaultSettings.individualMountPath().set(mountPath);
	}

	public String getMountName() {
		return vaultSettings.mountName().get();
	}

	public void setMountName(String mountName) throws IllegalArgumentException {
		if (StringUtils.isBlank(mountName)) {
			throw new IllegalArgumentException("mount name is empty");
		} else {
			vaultSettings.mountName().set(VaultSettings.normalizeMountName(mountName));
		}
	}

	public boolean isHavingCustomMountFlags() {
		return !Strings.isNullOrEmpty(vaultSettings.mountFlags().get());
	}

	public StringBinding defaultMountFlagsProperty() {
		return defaultMountFlags;
	}

	public String getDefaultMountFlags() {
		return defaultMountFlags.get();
	}

	public String getEffectiveMountFlags() {
		String mountFlags = vaultSettings.mountFlags().get();
		if (Strings.isNullOrEmpty(mountFlags)) {
			return getDefaultMountFlags();
		} else {
			return mountFlags;
		}
	}

	public void setCustomMountFlags(String mountFlags) {
		vaultSettings.mountFlags().set(mountFlags);
	}

	public Character getWinDriveLetter() {
		if (vaultSettings.winDriveLetter().get() == null) {
			return null;
		} else {
			return vaultSettings.winDriveLetter().get().charAt(0);
		}
	}

	public void setWinDriveLetter(Path root) {
		if (root == null) {
			vaultSettings.winDriveLetter().set(null);
		} else {
			char winDriveLetter = root.toString().charAt(0);
			vaultSettings.winDriveLetter().set(String.valueOf(winDriveLetter));
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

	public boolean supportsForcedUnmount() {
		return volume.supportsForcedUnmount();
	}
}
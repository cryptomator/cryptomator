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
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.mountpoint.InvalidMountPointException;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.vaults.Volume.VolumeException;
import org.cryptomator.cryptofs.CryptoFileSystem;
import org.cryptomator.cryptofs.CryptoFileSystemProperties;
import org.cryptomator.cryptofs.CryptoFileSystemProperties.FileSystemFlags;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptofs.common.Constants;
import org.cryptomator.cryptofs.common.FileSystemCapabilityChecker;
import org.cryptomator.cryptolib.api.CryptoException;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.cryptomator.common.Constants.MASTERKEY_FILENAME;

@PerVault
public class Vault {

	private static final Logger LOG = LoggerFactory.getLogger(Vault.class);
	private static final Path HOME_DIR = Paths.get(SystemUtils.USER_HOME);

	private final VaultSettings vaultSettings;
	private final Provider<Volume> volumeProvider;
	private final StringBinding defaultMountFlags;
	private final AtomicReference<CryptoFileSystem> cryptoFileSystem;
	private final ObjectProperty<VaultState> state;
	private final ObjectProperty<Exception> lastKnownException;
	private final VaultStats stats;
	private final StringBinding displayName;
	private final StringBinding displayablePath;
	private final BooleanBinding locked;
	private final BooleanBinding processing;
	private final BooleanBinding unlocked;
	private final BooleanBinding missing;
	private final BooleanBinding needsMigration;
	private final BooleanBinding unknownError;
	private final StringBinding accessPoint;
	private final BooleanBinding accessPointPresent;
	private final BooleanProperty showingStats;

	private volatile Volume volume;

	@Inject
	Vault(VaultSettings vaultSettings, Provider<Volume> volumeProvider, @DefaultMountFlags StringBinding defaultMountFlags, AtomicReference<CryptoFileSystem> cryptoFileSystem, ObjectProperty<VaultState> state, @Named("lastKnownException") ObjectProperty<Exception> lastKnownException, VaultStats stats) {
		this.vaultSettings = vaultSettings;
		this.volumeProvider = volumeProvider;
		this.defaultMountFlags = defaultMountFlags;
		this.cryptoFileSystem = cryptoFileSystem;
		this.state = state;
		this.lastKnownException = lastKnownException;
		this.stats = stats;
		this.displayName = Bindings.createStringBinding(this::getDisplayName, vaultSettings.displayName());
		this.displayablePath = Bindings.createStringBinding(this::getDisplayablePath, vaultSettings.path());
		this.locked = Bindings.createBooleanBinding(this::isLocked, state);
		this.processing = Bindings.createBooleanBinding(this::isProcessing, state);
		this.unlocked = Bindings.createBooleanBinding(this::isUnlocked, state);
		this.missing = Bindings.createBooleanBinding(this::isMissing, state);
		this.needsMigration = Bindings.createBooleanBinding(this::isNeedsMigration, state);
		this.unknownError = Bindings.createBooleanBinding(this::isUnknownError, state);
		this.accessPoint = Bindings.createStringBinding(this::getAccessPoint, state);
		this.accessPointPresent = this.accessPoint.isNotEmpty();
		this.showingStats = new SimpleBooleanProperty(false);
	}

	// ******************************************************************************
	// Commands
	// ********************************************************************************/

	private CryptoFileSystem createCryptoFileSystem(CharSequence passphrase) throws NoSuchFileException, IOException, InvalidPassphraseException, CryptoException {
		Set<FileSystemFlags> flags = EnumSet.noneOf(FileSystemFlags.class);
		if (vaultSettings.usesReadOnlyMode().get()) {
			flags.add(FileSystemFlags.READONLY);
		}
		if (vaultSettings.filenameLengthLimit().get() == -1) {
			LOG.debug("Determining file name length limitations...");
			int limit = new FileSystemCapabilityChecker().determineSupportedFileNameLength(getPath());
			vaultSettings.filenameLengthLimit().set(limit);
			LOG.info("Storing file name length limit of {}", limit);
		}
		assert vaultSettings.filenameLengthLimit().get() > 0;
		CryptoFileSystemProperties fsProps = CryptoFileSystemProperties.cryptoFileSystemProperties() //
				.withPassphrase(passphrase) //
				.withFlags(flags) //
				.withMasterkeyFilename(MASTERKEY_FILENAME) //
				.withMaxPathLength(vaultSettings.filenameLengthLimit().get() + Constants.MAX_ADDITIONAL_PATH_LENGTH) //
				.withMaxNameLength(vaultSettings.filenameLengthLimit().get()) //
				.build();
		return CryptoFileSystemProvider.newFileSystem(getPath(), fsProps);
	}

	public synchronized void unlock(CharSequence passphrase) throws CryptoException, IOException, VolumeException, InvalidMountPointException {
		if (cryptoFileSystem.get() == null) {
			CryptoFileSystem fs = createCryptoFileSystem(passphrase);
			cryptoFileSystem.set(fs);
			volume = volumeProvider.get();
			volume.mount(fs, getEffectiveMountFlags());
		} else {
			throw new IllegalStateException("Already unlocked.");
		}
	}

	public synchronized void lock(boolean forced) throws VolumeException {
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

	public void reveal() throws VolumeException {
		volume.reveal();
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

	public ObjectProperty<Exception> lastKnownExceptionProperty() {
		return lastKnownException;
	}

	public Exception getLastKnownException() {
		return lastKnownException.get();
	}

	public void setLastKnownException(Exception e) {
		lastKnownException.setValue(e);
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

	public BooleanBinding missingProperty() {
		return missing;
	}

	public boolean isMissing() {
		return state.get() == VaultState.MISSING;
	}

	public BooleanBinding needsMigrationProperty() {
		return needsMigration;
	}

	public boolean isNeedsMigration() {
		return state.get() == VaultState.NEEDS_MIGRATION;
	}

	public BooleanBinding unknownErrorProperty() {
		return unknownError;
	}

	public boolean isUnknownError() {
		return state.get() == VaultState.ERROR;
	}

	public StringBinding displayNameProperty() {
		return displayName;
	}

	public String getDisplayName() {
		return vaultSettings.displayName().get();
	}

	public StringBinding accessPointProperty() {
		return accessPoint;
	}

	public String getAccessPoint() {
		if (state.get() == VaultState.UNLOCKED) {
			assert volume != null;
			return volume.getMountPoint().orElse(Path.of("")).toString();
		} else {
			return "";
		}
	}

	public BooleanBinding accessPointPresentProperty() {
		return accessPointPresent;
	}

	public boolean isAccessPointPresent() {
		return accessPointPresent.get();
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

	public BooleanProperty showingStatsProperty() {
		return showingStats;
	}

	public boolean isShowingStats() {
		return accessPointPresent.get();
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

	public String getId() {
		return vaultSettings.getId();
	}

	public Optional<Volume> getVolume() {
		return Optional.ofNullable(this.volume);
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
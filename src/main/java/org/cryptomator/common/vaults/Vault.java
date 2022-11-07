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
import org.cryptomator.common.Constants;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.cryptofs.CryptoFileSystem;
import org.cryptomator.cryptofs.CryptoFileSystemProperties;
import org.cryptomator.cryptofs.CryptoFileSystemProperties.FileSystemFlags;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptofs.common.FileSystemCapabilityChecker;
import org.cryptomator.cryptolib.api.CryptoException;
import org.cryptomator.cryptolib.api.MasterkeyLoader;
import org.cryptomator.cryptolib.api.MasterkeyLoadingFailedException;
import org.cryptomator.integrations.mount.Mount;
import org.cryptomator.integrations.mount.MountBuilder;
import org.cryptomator.integrations.mount.MountCapability;
import org.cryptomator.integrations.mount.MountFailedException;
import org.cryptomator.integrations.mount.MountService;
import org.cryptomator.integrations.mount.Mountpoint;
import org.cryptomator.integrations.mount.UnmountFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@PerVault
public class Vault {

	private static final Logger LOG = LoggerFactory.getLogger(Vault.class);
	private static final Path HOME_DIR = Paths.get(SystemUtils.USER_HOME);
	private static final int UNLIMITED_FILENAME_LENGTH = Integer.MAX_VALUE;

	private final Settings settings;
	private final VaultSettings vaultSettings;
	private final AtomicReference<CryptoFileSystem> cryptoFileSystem;
	private final VaultState state;
	private final ObjectProperty<Exception> lastKnownException;
	private final ObservableValue<MountService> mountService;
	private final ObservableValue<String> defaultMountFlags;
	private final VaultConfigCache configCache;
	private final VaultStats stats;
	private final StringBinding displayablePath;
	private final BooleanBinding locked;
	private final BooleanBinding processing;
	private final BooleanBinding unlocked;
	private final BooleanBinding missing;
	private final BooleanBinding needsMigration;
	private final BooleanBinding unknownError;
	private final StringBinding accessPoint;
	private final BooleanProperty showingStats;

	private AtomicReference<MountHandle> mount = new AtomicReference<>(null);

	@Inject
	Vault(Settings settings, VaultSettings vaultSettings, VaultConfigCache configCache, AtomicReference<CryptoFileSystem> cryptoFileSystem, VaultState state, @Named("lastKnownException") ObjectProperty<Exception> lastKnownException, ObservableValue<MountService> mountService, VaultStats stats) {
		this.settings = settings;
		this.vaultSettings = vaultSettings;
		this.configCache = configCache;
		this.cryptoFileSystem = cryptoFileSystem;
		this.state = state;
		this.lastKnownException = lastKnownException;
		this.mountService = mountService;
		this.defaultMountFlags = Bindings.createStringBinding(() -> mountService.getValue().getDefaultMountFlags(vaultSettings.mountName().get()), vaultSettings.mountName(), mountService).orElse(""); //TODO: logic correct?
		this.stats = stats;
		this.displayablePath = Bindings.createStringBinding(this::getDisplayablePath, vaultSettings.path());
		this.locked = Bindings.createBooleanBinding(this::isLocked, state);
		this.processing = Bindings.createBooleanBinding(this::isProcessing, state);
		this.unlocked = Bindings.createBooleanBinding(this::isUnlocked, state);
		this.missing = Bindings.createBooleanBinding(this::isMissing, state);
		this.needsMigration = Bindings.createBooleanBinding(this::isNeedsMigration, state);
		this.unknownError = Bindings.createBooleanBinding(this::isUnknownError, state);
		this.accessPoint = Bindings.createStringBinding(this::getAccessPoint, state);
		this.showingStats = new SimpleBooleanProperty(false);
	}

	// ******************************************************************************
	// Commands
	// ********************************************************************************/

	private CryptoFileSystem createCryptoFileSystem(MasterkeyLoader keyLoader) throws IOException, MasterkeyLoadingFailedException {
		Set<FileSystemFlags> flags = EnumSet.noneOf(FileSystemFlags.class);
		if (vaultSettings.usesReadOnlyMode().get()) {
			flags.add(FileSystemFlags.READONLY);
		} else if (vaultSettings.maxCleartextFilenameLength().get() == -1) {
			LOG.debug("Determining cleartext filename length limitations...");
			var checker = new FileSystemCapabilityChecker();
			int shorteningThreshold = configCache.get().allegedShorteningThreshold();
			int ciphertextLimit = checker.determineSupportedCiphertextFileNameLength(getPath());
			if (ciphertextLimit < shorteningThreshold) {
				int cleartextLimit = checker.determineSupportedCleartextFileNameLength(getPath());
				vaultSettings.maxCleartextFilenameLength().set(cleartextLimit);
			} else {
				vaultSettings.maxCleartextFilenameLength().setValue(UNLIMITED_FILENAME_LENGTH);
			}
		}

		if (vaultSettings.maxCleartextFilenameLength().get() < UNLIMITED_FILENAME_LENGTH) {
			LOG.warn("Limiting cleartext filename length on this device to {}.", vaultSettings.maxCleartextFilenameLength().get());
		}

		CryptoFileSystemProperties fsProps = CryptoFileSystemProperties.cryptoFileSystemProperties() //
				.withKeyLoader(keyLoader) //
				.withFlags(flags) //
				.withMaxCleartextNameLength(vaultSettings.maxCleartextFilenameLength().get()) //
				.withVaultConfigFilename(Constants.VAULTCONFIG_FILENAME) //
				.build();
		return CryptoFileSystemProvider.newFileSystem(getPath(), fsProps);
	}

	private void destroyCryptoFileSystem() {
		LOG.trace("Trying to close associated CryptoFS...");
		CryptoFileSystem fs = cryptoFileSystem.getAndSet(null);
		if (fs != null) {
			try {
				fs.close();
			} catch (IOException e) {
				LOG.error("Error closing file system.", e);
			}
		}
	}

	private MountBuilder prepareMount(Path cryptoRoot) {
		var mountServiceImpl = mountService.getValue();
		var builder = mountServiceImpl.forFileSystem(cryptoRoot);

		for (var capabiltiy : mountServiceImpl.capabilities()) {
			switch (capabiltiy) {
				case LOOPBACK_PORT -> builder.setLoopbackPort(settings.port().get()); //TODO: move port from settings to vaultsettings?
				case LOOPBACK_HOST_NAME -> builder.setLoopbackHostName("cryptomator-vault"); //TODO: Read from system property
				case READ_ONLY -> builder.setReadOnly(vaultSettings.usesReadOnlyMode().get());
				case MOUNT_FLAGS -> builder.setMountFlags(mountServiceImpl.getDefaultMountFlags(vaultSettings.mountName().get())); //TODO: currently not adjustable
				case VOLUME_ID -> builder.setVolumeId(vaultSettings.mountName().get());
			}
		}

		if (mountServiceImpl.hasCapability(MountCapability.MOUNT_TO_EXISTING_DIR) //
				|| mountServiceImpl.hasCapability(MountCapability.MOUNT_WITHIN_EXISTING_PARENT) //
				|| mountServiceImpl.hasCapability(MountCapability.MOUNT_AS_DRIVE_LETTER)) {
			builder.setMountpoint(vaultSettings.getMountPoint());
		}

		return builder;

	}

	public synchronized void unlock(MasterkeyLoader keyLoader) throws CryptoException, IOException, MountFailedException {
		if (cryptoFileSystem.get() != null) {
			throw new IllegalStateException("Already unlocked.");
		}
		CryptoFileSystem fs = createCryptoFileSystem(keyLoader);
		boolean success = false;
		try {
			cryptoFileSystem.set(fs);
			var rootPath = fs.getRootDirectories().iterator().next();
			var supportsForcedUnmount = mountService.getValue().hasCapability(MountCapability.UNMOUNT_FORCED);
			var mountHandle = new MountHandle(prepareMount(rootPath).mount(), supportsForcedUnmount);
			success = mount.compareAndSet(null, mountHandle);
		} finally {
			if (!success) {
				destroyCryptoFileSystem();
			}
		}
	}


	public synchronized void lock(boolean forced) throws UnmountFailedException, IOException {
		var mountHandle = mount.get();
		if (mountHandle == null) {
			//TODO: noop or InvalidStateException?
			return;
		}

		if (forced && mountHandle.supportsUnmountForced) {
			mountHandle.mount.unmountForced();
		} else {
			mountHandle.mount.unmount();
		}

		try {
			mountHandle.mount.close();
		} finally {
			destroyCryptoFileSystem();
		}

		LOG.info("Locked vault '{}'", getDisplayName());
	}

	// ******************************************************************************
	// Observable Properties
	// *******************************************************************************

	public VaultState stateProperty() {
		return state;
	}

	public VaultState.Value getState() {
		return state.getValue();
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
		return state.get() == VaultState.Value.LOCKED;
	}

	public BooleanBinding processingProperty() {
		return processing;
	}

	public boolean isProcessing() {
		return state.get() == VaultState.Value.PROCESSING;
	}

	public BooleanBinding unlockedProperty() {
		return unlocked;
	}

	public boolean isUnlocked() {
		return state.get() == VaultState.Value.UNLOCKED;
	}

	public BooleanBinding missingProperty() {
		return missing;
	}

	public boolean isMissing() {
		return state.get() == VaultState.Value.MISSING;
	}

	public BooleanBinding needsMigrationProperty() {
		return needsMigration;
	}

	public boolean isNeedsMigration() {
		return state.get() == VaultState.Value.NEEDS_MIGRATION;
	}

	public BooleanBinding unknownErrorProperty() {
		return unknownError;
	}

	public boolean isUnknownError() {
		return state.get() == VaultState.Value.ERROR;
	}

	public ReadOnlyStringProperty displayNameProperty() {
		return vaultSettings.displayName();
	}

	public String getDisplayName() {
		return vaultSettings.displayName().get();
	}

	public StringBinding accessPointProperty() {
		return accessPoint;
	}

	public String getAccessPoint() {
		var mountPoint = mount.get().mount.getMountpoint();
		if (mountPoint instanceof Mountpoint.WithPath m) {
			return m.path().toString();
		} else {
			return mountPoint.uri().toString();
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

	public BooleanProperty showingStatsProperty() {
		return showingStats;
	}

	public boolean isShowingStats() {
		return mount.get() != null;
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

	public ObservableValue<String> defaultMountFlagsProperty() {
		return defaultMountFlags;
	}

	public String getDefaultMountFlags() {
		return defaultMountFlags.getValue();
	}

	public String getEffectiveMountFlags() {
		String mountFlags = vaultSettings.mountFlags().get();
		if (Strings.isNullOrEmpty(mountFlags)) {
			return ""; //TODO: should the provider provide dem defaults??
		} else {
			return mountFlags;
		}
	}

	public VaultConfigCache getVaultConfigCache() {
		return configCache;
	}

	public void setCustomMountFlags(String mountFlags) {
		vaultSettings.mountFlags().set(mountFlags);
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
		if (obj instanceof Vault other && obj.getClass().equals(this.getClass())) {
			return Objects.equals(this.vaultSettings, other.vaultSettings);
		} else {
			return false;
		}
	}


	/* TODO: reactivate/ needed at all?
	public boolean supportsForcedUnmount() {
		return volume.supportsForcedUnmount();
	}

	 */

	private record MountHandle(Mount mount, boolean supportsUnmountForced) {

	}
}
/*******************************************************************************
 * Copyright (c) 2016, 2017 Sebastian Stenzel and others.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.common.vaults;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.Constants;
import org.cryptomator.common.ObservableUtil;
import org.cryptomator.common.mount.ActualMountService;
import org.cryptomator.common.mount.Mounter;
import org.cryptomator.common.mount.WindowsDriveLetters;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.cryptofs.CryptoFileSystem;
import org.cryptomator.cryptofs.CryptoFileSystemProperties;
import org.cryptomator.cryptofs.CryptoFileSystemProperties.FileSystemFlags;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptofs.common.FileSystemCapabilityChecker;
import org.cryptomator.cryptolib.api.CryptoException;
import org.cryptomator.cryptolib.api.MasterkeyLoader;
import org.cryptomator.cryptolib.api.MasterkeyLoadingFailedException;
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
import javafx.beans.binding.ObjectBinding;
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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@PerVault
public class Vault {

	private static final Logger LOG = LoggerFactory.getLogger(Vault.class);
	private static final Path HOME_DIR = Paths.get(SystemUtils.USER_HOME);
	private static final int UNLIMITED_FILENAME_LENGTH = Integer.MAX_VALUE;

	private final VaultSettings vaultSettings;
	private final AtomicReference<CryptoFileSystem> cryptoFileSystem;
	private final VaultState state;
	private final ObjectProperty<Exception> lastKnownException;
	private final VaultConfigCache configCache;
	private final VaultStats stats;
	private final StringBinding displayablePath;
	private final BooleanBinding locked;
	private final BooleanBinding processing;
	private final BooleanBinding unlocked;
	private final BooleanBinding missing;
	private final BooleanBinding needsMigration;
	private final BooleanBinding unknownError;
	private final ObjectBinding<Mountpoint> mountPoint;
	private final Mounter mounter;
	private final BooleanProperty showingStats;
	private final ObservableValue<ActualMountService> actualMountService;
	private final List<MountService> mountProviders;
	private final ObservableValue<MountService> selectedMountService;
	private final AtomicReference<MountService> firstUsedProblematicFuseMountService;

	private final AtomicReference<Mounter.MountHandle> mountHandle = new AtomicReference<>(null);

	@Inject
	Vault(VaultSettings vaultSettings, VaultConfigCache configCache, AtomicReference<CryptoFileSystem> cryptoFileSystem, List<MountService> mountProviders, VaultState state, @Named("lastKnownException") ObjectProperty<Exception> lastKnownException, VaultStats stats, WindowsDriveLetters windowsDriveLetters, Mounter mounter, @Named("vaultMountService") ObservableValue<ActualMountService> actualMountService, @Named("FUPFMS") AtomicReference<MountService> firstUsedProblematicFuseMountService) {
		this.vaultSettings = vaultSettings;
		this.configCache = configCache;
		this.cryptoFileSystem = cryptoFileSystem;
		this.state = state;
		this.lastKnownException = lastKnownException;
		this.stats = stats;
		this.displayablePath = Bindings.createStringBinding(this::getDisplayablePath, vaultSettings.path);
		this.locked = Bindings.createBooleanBinding(this::isLocked, state);
		this.processing = Bindings.createBooleanBinding(this::isProcessing, state);
		this.unlocked = Bindings.createBooleanBinding(this::isUnlocked, state);
		this.missing = Bindings.createBooleanBinding(this::isMissing, state);
		this.needsMigration = Bindings.createBooleanBinding(this::isNeedsMigration, state);
		this.unknownError = Bindings.createBooleanBinding(this::isUnknownError, state);
		this.mountPoint = Bindings.createObjectBinding(this::getMountPoint, state);
		this.mounter = mounter;
		this.showingStats = new SimpleBooleanProperty(false);
		this.actualMountService = actualMountService;
		this.mountProviders = mountProviders;
		var fallbackProvider = mountProviders.stream().findFirst().orElse(null);
		this.selectedMountService = ObservableUtil.mapWithDefault(vaultSettings.mountService, serviceName -> mountProviders.stream().filter(s -> s.getClass().getName().equals(serviceName)).findFirst().orElse(fallbackProvider), fallbackProvider);
		this.firstUsedProblematicFuseMountService = firstUsedProblematicFuseMountService;
	}

	// ******************************************************************************
	// Commands
	// ********************************************************************************/

	private CryptoFileSystem createCryptoFileSystem(MasterkeyLoader keyLoader) throws IOException, MasterkeyLoadingFailedException {
		Set<FileSystemFlags> flags = EnumSet.noneOf(FileSystemFlags.class);
		if (vaultSettings.usesReadOnlyMode.get()) {
			flags.add(FileSystemFlags.READONLY);
		} else if (vaultSettings.maxCleartextFilenameLength.get() == -1) {
			LOG.debug("Determining cleartext filename length limitations...");
			var checker = new FileSystemCapabilityChecker();
			int shorteningThreshold = configCache.get().allegedShorteningThreshold();
			int ciphertextLimit = checker.determineSupportedCiphertextFileNameLength(getPath());
			if (ciphertextLimit < shorteningThreshold) {
				int cleartextLimit = checker.determineSupportedCleartextFileNameLength(getPath());
				vaultSettings.maxCleartextFilenameLength.set(cleartextLimit);
			} else {
				vaultSettings.maxCleartextFilenameLength.setValue(UNLIMITED_FILENAME_LENGTH);
			}
		}

		if (vaultSettings.maxCleartextFilenameLength.get() < UNLIMITED_FILENAME_LENGTH) {
			LOG.warn("Limiting cleartext filename length on this device to {}.", vaultSettings.maxCleartextFilenameLength.get());
		}

		CryptoFileSystemProperties fsProps = CryptoFileSystemProperties.cryptoFileSystemProperties() //
				.withKeyLoader(keyLoader) //
				.withFlags(flags) //
				.withMaxCleartextNameLength(vaultSettings.maxCleartextFilenameLength.get()) //
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

	public synchronized void unlock(MasterkeyLoader keyLoader) throws CryptoException, IOException, MountFailedException {
		if (cryptoFileSystem.get() != null) {
			throw new IllegalStateException("Already unlocked.");
		}
		var fallbackProvider = mountProviders.stream().findFirst().orElse(null);
		var selMountServ = ObservableUtil.mapWithDefault(vaultSettings.mountService, serviceName -> mountProviders.stream().filter(s -> s.getClass().getName().equals(serviceName)).findFirst().orElse(fallbackProvider), fallbackProvider);
		var fuseRestartRequired = selMountServ.map(s -> //
				firstUsedProblematicFuseMountService.get() != null //
						&& VaultModule.isProblematicFuseService(s) //
						&& !firstUsedProblematicFuseMountService.get().equals(s)).getValue();
		if(fuseRestartRequired){
			throw new MountFailedException("fuseRestartRequired");
		}

		CryptoFileSystem fs = createCryptoFileSystem(keyLoader);
		boolean success = false;
		try {
			cryptoFileSystem.set(fs);
			var rootPath = fs.getRootDirectories().iterator().next();
			var mountHandle = mounter.mount(vaultSettings, rootPath, actualMountService);
			success = this.mountHandle.compareAndSet(null, mountHandle);
		} finally {
			if (!success) {
				destroyCryptoFileSystem();
			}
		}
	}

	public synchronized void lock(boolean forced) throws UnmountFailedException, IOException {
		var mountHandle = this.mountHandle.get();
		if (mountHandle == null) {
			//TODO: noop or InvalidStateException?
			return;
		}

		if (forced && mountHandle.supportsUnmountForced()) {
			mountHandle.mountObj().unmountForced();
		} else {
			mountHandle.mountObj().unmount();
		}

		try {
			mountHandle.mountObj().close();
			mountHandle.specialCleanup().run();
		} finally {
			destroyCryptoFileSystem();
		}

		this.mountHandle.set(null);
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
		return vaultSettings.displayName;
	}

	public String getDisplayName() {
		return vaultSettings.displayName.get();
	}

	public ObjectBinding<Mountpoint> mountPointProperty() {
		return mountPoint;
	}

	public Mountpoint getMountPoint() {
		var handle = mountHandle.get();
		return handle == null ? null : handle.mountObj().getMountpoint();
	}

	public StringBinding displayablePathProperty() {
		return displayablePath;
	}

	public String getDisplayablePath() {
		Path p = vaultSettings.path.get();
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
		return mountHandle.get() != null;
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
		return vaultSettings.path.get();
	}

	/**
	 * Gets from the cleartext path its ciphertext counterpart.
	 *
	 * @return Local os path to the ciphertext resource
	 * @throws IOException if an I/O error occurs
	 * @throws IllegalStateException if the vault is not unlocked
	 */
	public Path getCiphertextPath(Path cleartextPath) throws IOException {
		if (!state.getValue().equals(VaultState.Value.UNLOCKED)) {
			throw new IllegalStateException("Vault is not unlocked");
		}
		var fs = cryptoFileSystem.get();
		var osPathSeparator = cleartextPath.getFileSystem().getSeparator();
		var cryptoFsPathSeparator = fs.getSeparator();

		if (getMountPoint() instanceof Mountpoint.WithPath mp) {
			var absoluteCryptoFsPath = cryptoFsPathSeparator + mp.path().relativize(cleartextPath).toString();
			if (!cryptoFsPathSeparator.equals(osPathSeparator)) {
				absoluteCryptoFsPath = absoluteCryptoFsPath.replace(osPathSeparator, cryptoFsPathSeparator);
			}
			var cryptoPath = fs.getPath(absoluteCryptoFsPath);
			return fs.getCiphertextPath(cryptoPath);
		} else {
			throw new UnsupportedOperationException("URI mount points not supported.");
		}
	}

	public VaultConfigCache getVaultConfigCache() {
		return configCache;
	}

	public String getId() {
		return vaultSettings.id;
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

	public boolean supportsForcedUnmount() {
		var mh = mountHandle.get();
		if (mh == null) {
			throw new IllegalStateException("Vault is not mounted");
		}
		return mountHandle.get().supportsUnmountForced();
	}

}
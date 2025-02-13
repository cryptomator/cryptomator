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
import org.cryptomator.common.mount.Mounter;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.cryptofs.CryptoFileSystem;
import org.cryptomator.cryptofs.CryptoFileSystemProperties;
import org.cryptomator.cryptofs.CryptoFileSystemProperties.FileSystemFlags;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptofs.common.FileSystemCapabilityChecker;
import org.cryptomator.cryptofs.event.FilesystemEvent;
import org.cryptomator.cryptolib.api.CryptoException;
import org.cryptomator.cryptolib.api.MasterkeyLoader;
import org.cryptomator.cryptolib.api.MasterkeyLoadingFailedException;
import org.cryptomator.integrations.mount.MountFailedException;
import org.cryptomator.integrations.mount.Mountpoint;
import org.cryptomator.integrations.mount.UnmountFailedException;
import org.cryptomator.integrations.quickaccess.QuickAccessService;
import org.cryptomator.integrations.quickaccess.QuickAccessServiceException;
import org.cryptomator.event.Event;
import org.cryptomator.event.VaultEvent;
import org.jetbrains.annotations.NotNull;
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
import javafx.collections.ObservableList;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ReadOnlyFileSystemException;
import java.util.EnumSet;
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
	private final AtomicReference<QuickAccessService.QuickAccessEntry> quickAccessEntry;
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
	private final Settings settings;
	private final ObservableList<Event> eventQueue;
	private final BooleanProperty showingStats;

	private final AtomicReference<Mounter.MountHandle> mountHandle = new AtomicReference<>(null);

	@Inject
	Vault(VaultSettings vaultSettings, //
		  VaultConfigCache configCache, //
		  AtomicReference<CryptoFileSystem> cryptoFileSystem, //
		  VaultState state, //
		  @Named("lastKnownException") ObjectProperty<Exception> lastKnownException, //
		  VaultStats stats, //
		  Mounter mounter, Settings settings, //
		  ObservableList<Event> eventQueue
		   ) {
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
		this.settings = settings;
		this.eventQueue = eventQueue;
		this.showingStats = new SimpleBooleanProperty(false);
		this.quickAccessEntry = new AtomicReference<>(null);
	}

	// ******************************************************************************
	// Commands
	// ********************************************************************************/

	private CryptoFileSystem createCryptoFileSystem(MasterkeyLoader keyLoader) throws IOException, MasterkeyLoadingFailedException {
		Set<FileSystemFlags> flags = EnumSet.noneOf(FileSystemFlags.class);
		var createReadOnly = vaultSettings.usesReadOnlyMode.get();
		try {
			FileSystemCapabilityChecker.assertWriteAccess(getPath());
		} catch (FileSystemCapabilityChecker.MissingCapabilityException e) {
			if (!createReadOnly) {
				throw new ReadOnlyFileSystemException();
			}
		}
		if (createReadOnly) {
			flags.add(FileSystemFlags.READONLY);
		} else if (vaultSettings.maxCleartextFilenameLength.get() == -1) {
			LOG.debug("Determining cleartext filename length limitations...");
			int shorteningThreshold = configCache.get().allegedShorteningThreshold();
			int ciphertextLimit = FileSystemCapabilityChecker.determineSupportedCiphertextFileNameLength(getPath());
			if (ciphertextLimit < shorteningThreshold) {
				int cleartextLimit = FileSystemCapabilityChecker.determineSupportedCleartextFileNameLength(getPath());
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
				.withFilesystemEventConsumer(this::consumeVaultEvent)
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
		CryptoFileSystem fs = createCryptoFileSystem(keyLoader);
		boolean success = false;
		try {
			cryptoFileSystem.set(fs);
			var rootPath = fs.getRootDirectories().iterator().next();
			var mountHandle = mounter.mount(vaultSettings, rootPath);
			success = this.mountHandle.compareAndSet(null, mountHandle);
			if (settings.useQuickAccess.getValue()) {
				addToQuickAccess();
			}
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
			removeFromQuickAccess();
			destroyCryptoFileSystem();
		}

		this.mountHandle.set(null);
		LOG.info("Locked vault '{}'", getDisplayName());
	}

	private synchronized void addToQuickAccess() {
		if (quickAccessEntry.get() != null) {
			//we don't throw an exception since we don't wanna block unlocking
			LOG.warn("Vault already added to quick access area. Will be removed on next lock operation.");
			return;
		}

		QuickAccessService.get() //
				.filter(s -> s.getClass().getName().equals(settings.quickAccessService.getValue())) //
				.findFirst() //
				.ifPresentOrElse( //
						this::addToQuickAccessInternal, //
						() -> LOG.warn("Unable to add Vault to quick access area: Desired implementation not available.") //
				);
	}

	private void addToQuickAccessInternal(@NotNull QuickAccessService s) {
		if (getMountPoint() instanceof Mountpoint.WithPath mp) {
			try {
				var entry = s.add(mp.path(), getDisplayName());
				quickAccessEntry.set(entry);
			} catch (QuickAccessServiceException e) {
				LOG.error("Adding vault to quick access area failed", e);
			}
		} else {
			LOG.warn("Unable to add vault to quick access area: Vault is not mounted to local system path.");
		}
	}

	private synchronized void removeFromQuickAccess() {
		if (quickAccessEntry.get() == null) {
			LOG.debug("Removing vault from quick access area: Entry not found, nothing to do.");
			return;
		}
		removeFromQuickAccessInternal();
	}

	private void removeFromQuickAccessInternal() {
		try {
			quickAccessEntry.get().remove();
			quickAccessEntry.set(null);
		} catch (QuickAccessServiceException e) {
			LOG.error("Removing vault from quick access area failed", e);
		}
	}

	private void consumeVaultEvent(FilesystemEvent e) {
		eventQueue.addLast(new VaultEvent(vaultSettings.id, vaultSettings.path.get().toString(), e));
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
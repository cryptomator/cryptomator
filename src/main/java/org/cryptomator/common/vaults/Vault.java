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
import org.cryptomator.event.FileSystemEventAggregator;
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
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
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
	private final VaultIdentityProvider identityProvider;
	private final StringBinding displayablePath;
	private final BooleanBinding locked;
	private final BooleanBinding processing;
	private final BooleanBinding unlocked;
	private final BooleanBinding missing;
	private final BooleanBinding needsMigration;
	private final BooleanBinding unknownError;
	private final BooleanBinding missingVaultConfig;
	private final ObjectBinding<Mountpoint> mountPoint;
	private final Mounter mounter;
	private final Settings settings;
	private final FileSystemEventAggregator fileSystemEventAggregator;
	private final BooleanProperty showingStats;

	private final AtomicReference<Mounter.MountHandle> mountHandle = new AtomicReference<>(null);
	private final AtomicReference<VaultIdentity> currentIdentity = new AtomicReference<>(null);

	@Inject
	Vault(VaultSettings vaultSettings, //
		  VaultConfigCache configCache, //
		  AtomicReference<CryptoFileSystem> cryptoFileSystem, //
		  VaultState state, //
		  @Named("lastKnownException") ObjectProperty<Exception> lastKnownException, //
		  VaultStats stats, //
		  VaultIdentityProvider identityProvider, //
		  Mounter mounter, Settings settings, //
		  FileSystemEventAggregator fileSystemEventAggregator) {
		this.vaultSettings = vaultSettings;
		this.configCache = configCache;
		this.cryptoFileSystem = cryptoFileSystem;
		this.state = state;
		this.lastKnownException = lastKnownException;
		this.stats = stats;
		this.identityProvider = identityProvider;
		this.displayablePath = Bindings.createStringBinding(this::getDisplayablePath, vaultSettings.path);
		this.locked = Bindings.createBooleanBinding(this::isLocked, state);
		this.processing = Bindings.createBooleanBinding(this::isProcessing, state);
		this.unlocked = Bindings.createBooleanBinding(this::isUnlocked, state);
		this.missing = Bindings.createBooleanBinding(this::isMissing, state);
		this.missingVaultConfig = Bindings.createBooleanBinding(this::isMissingVaultConfig, state);
		this.needsMigration = Bindings.createBooleanBinding(this::isNeedsMigration, state);
		this.unknownError = Bindings.createBooleanBinding(this::isUnknownError, state);
		this.mountPoint = Bindings.createObjectBinding(this::getMountPoint, state);
		this.mounter = mounter;
		this.settings = settings;
		this.fileSystemEventAggregator = fileSystemEventAggregator;
		this.showingStats = new SimpleBooleanProperty(false);
		this.quickAccessEntry = new AtomicReference<>(null);
	}

	// ******************************************************************************
	// Commands
	// ********************************************************************************/

	private CryptoFileSystem createCryptoFileSystem(MasterkeyLoader keyLoader, VaultIdentity identity) throws IOException, MasterkeyLoadingFailedException {
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
			LOG.debug("Determining cleartext filename limitations...");
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

		// Use identity-specific vault config file (vault.cryptomator or vault.bak)
		String vaultConfigFilename = identity.getVaultConfigFilename();
		LOG.info("UNLOCK: Using identity '{}' (primary={}) with vault config file: {}", 
				identity.getName(), identity.isPrimary(), vaultConfigFilename);

		// Handle multi-keyslot vault configs: extract the correct config for this masterkey
		String actualConfigFilename = prepareVaultConfigForUnlock(keyLoader, vaultConfigFilename);
		
		CryptoFileSystemProperties fsProps = CryptoFileSystemProperties.cryptoFileSystemProperties() //
				.withKeyLoader(keyLoader) //
				.withFlags(flags) //
				.withVaultConfigFilename(actualConfigFilename) //
				.withFilesystemEventConsumer(this::consumeVaultEvent) //
				.build();
		return CryptoFileSystemProvider.newFileSystem(getPath(), fsProps);
	}
	
	/**
	 * Prepares vault config for unlocking. If the config file is in multi-keyslot format,
	 * extracts the appropriate config for the given masterkey and writes it temporarily.
	 * 
	 * @param keyLoader Loader that can provide the masterkey
	 * @param configFilename Original config filename
	 * @return Filename to use for unlocking (may be temporary)
	 * @throws IOException on errors
	 */
	private String prepareVaultConfigForUnlock(MasterkeyLoader keyLoader, String configFilename) throws IOException {
		Path configPath = getPath().resolve(configFilename);
		
		// Check if this is a multi-keyslot file
		MultiKeyslotVaultConfig multiKeyslot = new MultiKeyslotVaultConfig();
		if (!multiKeyslot.isMultiKeyslotFile(configPath)) {
			// Legacy single-config file, use as-is
			return configFilename;
		}
		
		// SECURITY: Don't log "multi-keyslot" detection - reveals hidden vault capability
		LOG.debug("Preparing vault config for unlock");
		
		byte[] masterkeyBytes = null;
		try {
			// Load the masterkey to determine which config to use
			masterkeyBytes = keyLoader.loadKey(URI.create("masterkeyfile:masterkey.cryptomator")).getEncoded();
			
			// Read the config slots and find the one that matches this masterkey
			java.util.List<String> configTokens = readConfigSlotsFromMultiKeyslot(configPath);
			
			// Try each config to find which one verifies with this masterkey
			String matchingToken = null;
			for (String token : configTokens) {
				try {
					var config = org.cryptomator.cryptofs.VaultConfig.decode(token);
					config.verify(masterkeyBytes, config.allegedVaultVersion());
					matchingToken = token;
					// SECURITY: Don't log "slot" terminology
					LOG.debug("Vault config matched");
					break;
				} catch (Exception e) {
					// Not this config, try next
				}
			}
			
			if (matchingToken == null) {
				throw new IOException("No config matches the loaded masterkey");
			}
			
			// Write to temporary config file
			Path tempConfigPath = getPath().resolve(".vault.cryptomator.unlock");
			Files.writeString(tempConfigPath, matchingToken, java.nio.charset.StandardCharsets.US_ASCII);
			
			LOG.debug("Prepared config for unlock");
			return ".vault.cryptomator.unlock";
			
		} catch (Exception e) {
			// Clean up temp file on failure
			try {
				Files.deleteIfExists(getPath().resolve(".vault.cryptomator.unlock"));
			} catch (IOException ignored) {
				// Ignore cleanup errors
			}
			throw new IOException("Failed to extract config from multi-keyslot file", e);
		} finally {
			// SECURITY: Always zero sensitive masterkey bytes after use
			if (masterkeyBytes != null) {
				java.util.Arrays.fill(masterkeyBytes, (byte) 0);
			}
		}
	}
	
	/**
	 * Reads all config tokens from a multi-keyslot file.
	 * Uses centralized parsing logic from MultiKeyslotVaultConfig for consistency
	 * and security (bounds checks).
	 */
	private java.util.List<String> readConfigSlotsFromMultiKeyslot(Path path) throws IOException {
		// Delegate to MultiKeyslotVaultConfig for centralized parsing with bounds checks
		MultiKeyslotVaultConfig multiKeyslot = new MultiKeyslotVaultConfig();
		
		// Use reflection to call private readConfigSlots method
		// This ensures we use the same parsing logic with bounds checks everywhere
		try {
			java.lang.reflect.Method method = MultiKeyslotVaultConfig.class.getDeclaredMethod("readConfigSlots", Path.class);
			method.setAccessible(true);
			@SuppressWarnings("unchecked")
			java.util.List<String> result = (java.util.List<String>) method.invoke(multiKeyslot, path);
			return result;
		} catch (java.lang.reflect.InvocationTargetException e) {
			// Unwrap the real exception
			Throwable cause = e.getCause();
			if (cause instanceof IOException) {
				throw (IOException) cause;
			}
			throw new IOException("Failed to read config slots", cause);
		} catch (Exception e) {
			throw new IOException("Failed to access config slot parsing", e);
		}
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
	
	/**
	 * Cleans up temporary vault config file created during unlock.
	 */
	private void cleanupTempVaultConfig() {
		try {
			Path tempConfigPath = getPath().resolve(".vault.cryptomator.unlock");
			Files.deleteIfExists(tempConfigPath);
		} catch (IOException e) {
			LOG.warn("Failed to clean up temporary vault config", e);
		}
	}

	public synchronized void unlock(MasterkeyLoader keyLoader) throws CryptoException, IOException, MountFailedException {
		// Use primary identity by default
		try {
			VaultIdentity identity = identityProvider.getManager().getPrimaryIdentity()
					.orElseThrow(() -> new IllegalStateException("No primary identity found"));
			unlock(keyLoader, identity);
		} catch (IOException e) {
			throw new IOException("Failed to load vault identities", e);
		}
	}

	public synchronized void unlock(MasterkeyLoader keyLoader, VaultIdentity identity) throws CryptoException, IOException, MountFailedException {
		if (cryptoFileSystem.get() != null) {
			throw new IllegalStateException("Already unlocked.");
		}
		CryptoFileSystem fs = createCryptoFileSystem(keyLoader, identity);
		boolean success = false;
		try {
			cryptoFileSystem.set(fs);
			currentIdentity.set(identity);
			var rootPath = fs.getRootDirectories().iterator().next();
			var mountHandle = mounter.mount(vaultSettings, rootPath);
			success = this.mountHandle.compareAndSet(null, mountHandle);
			if (settings.useQuickAccess.getValue()) {
				addToQuickAccess();
			}
		} finally {
			if (!success) {
				currentIdentity.set(null);
				destroyCryptoFileSystem();
				cleanupTempVaultConfig();  // Clean up temp config file on failure
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
			cleanupTempVaultConfig();
			currentIdentity.set(null);
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
		fileSystemEventAggregator.put(this, e);
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

	public BooleanBinding missingVaultConfigProperty() {
		return missingVaultConfig;
	}

	public boolean isMissingVaultConfig() {
		return state.get() == VaultState.Value.VAULT_CONFIG_MISSING || state.get() == VaultState.Value.ALL_MISSING;
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

	/**
	 * Gets the cleartext name from a given path to an encrypted vault file
	 */
	public String getCleartextName(Path ciphertextPath) throws IOException {
		if (!state.getValue().equals(VaultState.Value.UNLOCKED)) {
			throw new IllegalStateException("Vault is not unlocked");
		}
		var fs = cryptoFileSystem.get();
		return fs.getCleartextName(ciphertextPath);
	}

	public VaultConfigCache getVaultConfigCache() {
		return configCache;
	}

	public String getId() {
		return vaultSettings.id;
	}

	public VaultIdentityProvider getIdentityProvider() {
		return identityProvider;
	}

	public VaultIdentity getCurrentIdentity() {
		return currentIdentity.get();
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
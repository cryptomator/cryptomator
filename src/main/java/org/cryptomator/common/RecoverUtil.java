package org.cryptomator.common;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import static org.cryptomator.common.Constants.DEFAULT_KEY_ID;
import static org.cryptomator.common.Constants.MASTERKEY_FILENAME;
import static org.cryptomator.common.Constants.VAULTCONFIG_FILENAME;
import static org.cryptomator.common.vaults.VaultState.Value.MASTERKEY_MISSING;
import static org.cryptomator.common.vaults.VaultState.Value.VAULT_CONFIG_MISSING;
import static org.cryptomator.cryptofs.common.Constants.DATA_DIR_NAME;
import static org.cryptomator.cryptolib.api.CryptorProvider.Scheme.SIV_CTRMAC;
import static org.cryptomator.cryptolib.api.CryptorProvider.Scheme.SIV_GCM;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultComponent;
import org.cryptomator.common.vaults.VaultConfigCache;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.cryptofs.CryptoFileSystemProperties;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptolib.api.CryptoException;
import org.cryptomator.cryptolib.api.Cryptor;
import org.cryptomator.cryptolib.api.CryptorProvider;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.api.MasterkeyLoader;
import org.cryptomator.cryptolib.common.MasterkeyFileAccess;
import org.cryptomator.integrations.mount.MountService;
import org.cryptomator.ui.changepassword.NewPasswordController;
import org.cryptomator.ui.dialogs.Dialogs;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.cryptomator.ui.recoverykey.RecoveryKeyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecoverUtil {

	private static final Logger LOG = LoggerFactory.getLogger(RecoverUtil.class);

	public static CryptorProvider.Scheme detectCipherCombo(byte[] masterkey, Path pathToVault) {
		try (Stream<Path> paths = Files.walk(pathToVault.resolve(DATA_DIR_NAME))) {
			return paths.filter(path -> path.toString().endsWith(".c9r")).findFirst().map(c9rFile -> determineScheme(c9rFile, masterkey)).orElseThrow(() -> new IllegalStateException("No .c9r file found."));
		} catch (IOException e) {
			throw new IllegalStateException("Failed to detect cipher combo.", e);
		}
	}

	private static CryptorProvider.Scheme determineScheme(Path c9rFile, byte[] masterkey) {
		try {
			ByteBuffer header = ByteBuffer.wrap(Files.readAllBytes(c9rFile));
			return tryDecrypt(header, new Masterkey(masterkey), SIV_GCM) ? SIV_GCM : tryDecrypt(header, new Masterkey(masterkey), SIV_CTRMAC) ? SIV_CTRMAC : null;
		} catch (IOException e) {
			return null;
		}
	}

	private static boolean tryDecrypt(ByteBuffer header, Masterkey masterkey, CryptorProvider.Scheme scheme) {
		try (Cryptor cryptor = CryptorProvider.forScheme(scheme).provide(masterkey, SecureRandom.getInstanceStrong())) {
			cryptor.fileHeaderCryptor().decryptHeader(header.duplicate());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean restoreBackupIfAvailable(Path configPath, VaultState.Value vaultState) {
		try (Stream<Path> files = Files.list(configPath.getParent())) {
			return files.filter(file -> matchesBackupFile(file.getFileName().toString(), vaultState)).findFirst().map(backupFile -> copyBackupFile(backupFile, configPath)).orElse(false);
		} catch (IOException e) {
			return false;
		}
	}

	private static boolean matchesBackupFile(String fileName, VaultState.Value vaultState) {
		return switch (vaultState) {
			case VAULT_CONFIG_MISSING -> fileName.startsWith("vault.cryptomator") && fileName.endsWith(".bkup");
			case MASTERKEY_MISSING -> fileName.startsWith("masterkey.cryptomator") && fileName.endsWith(".bkup");
			default -> false;
		};
	}

	private static boolean copyBackupFile(Path backupFile, Path configPath) {
		try {
			Files.copy(backupFile, configPath, StandardCopyOption.REPLACE_EXISTING);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public static Path createRecoveryDirectory(Path vaultPath) throws IOException {
		Path recoveryPath = vaultPath.resolve("r");
		Files.createDirectory(recoveryPath);
		return recoveryPath;
	}

	public static void createNewMasterkeyFile(RecoveryKeyFactory recoveryKeyFactory, Path recoveryPath, String recoveryKey, CharSequence newPassword) throws IOException {
		recoveryKeyFactory.newMasterkeyFileWithPassphrase(recoveryPath, recoveryKey, newPassword);
	}

	public static Masterkey loadMasterkey(org.cryptomator.cryptolib.common.MasterkeyFileAccess masterkeyFileAccess, Path masterkeyFilePath, CharSequence password) throws IOException {
		return masterkeyFileAccess.load(masterkeyFilePath, password);
	}

	public static void initializeCryptoFileSystem(Path recoveryPath, Masterkey masterkey, IntegerProperty shorteningThreshold, CryptorProvider.Scheme combo) throws IOException, CryptoException {
		MasterkeyLoader loader = ignored -> masterkey.copy();
		CryptoFileSystemProperties fsProps = CryptoFileSystemProperties.cryptoFileSystemProperties().withCipherCombo(combo).withKeyLoader(loader).withShorteningThreshold(shorteningThreshold.get()).build();
		CryptoFileSystemProvider.initialize(recoveryPath, fsProps, DEFAULT_KEY_ID);
	}

	public static Optional<CryptorProvider.Scheme> getCipherCombo(Path vaultPath, Masterkey masterkey) {
		try {
			return Optional.of(RecoverUtil.detectCipherCombo(masterkey.getEncoded(), vaultPath));
		} catch (Exception e) {
			LOG.warn("Failed to detect cipher combo", e);
			return Optional.empty();
		}
	}

	public static Optional<CryptorProvider.Scheme> validateRecoveryKeyAndGetCombo(RecoveryKeyFactory recoveryKeyFactory, Vault vault, StringProperty recoveryKey, MasterkeyFileAccess masterkeyFileAccess) {

		Path tempRecoveryPath = null;
		CharSequence tmpPass = "asdasdasd";

		try {
			tempRecoveryPath = createRecoveryDirectory(vault.getPath());
			createNewMasterkeyFile(recoveryKeyFactory, tempRecoveryPath, recoveryKey.get(), tmpPass);
			Path masterkeyFilePath = tempRecoveryPath.resolve(MASTERKEY_FILENAME);

			try (Masterkey masterkey = loadMasterkey(masterkeyFileAccess, masterkeyFilePath, tmpPass)) {
				return getCipherCombo(vault.getPath(), masterkey);
			}
		} catch (IOException | CryptoException e) {
			LOG.warn("Recovery key validation failed", e);
			return Optional.empty();
		} finally {
			if (tempRecoveryPath != null) {
				deleteRecoveryDirectory(tempRecoveryPath);
			}
		}
	}


	public static void moveRecoveredFiles(Path recoveryPath, Path vaultPath) throws IOException {
		Files.move(recoveryPath.resolve(MASTERKEY_FILENAME), vaultPath.resolve(MASTERKEY_FILENAME), StandardCopyOption.REPLACE_EXISTING);
		Files.move(recoveryPath.resolve(VAULTCONFIG_FILENAME), vaultPath.resolve(VAULTCONFIG_FILENAME));
	}

	public static void deleteRecoveryDirectory(Path recoveryPath) {
		try (var paths = Files.walk(recoveryPath)) {
			paths.sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.delete(p);
				} catch (IOException e) {
					LOG.info("Unable to delete {}. Please delete it manually.", p);
				}
			});
		} catch (IOException e) {
			LOG.error("Failed to clean up recovery directory", e);
		}
	}

	public static void addVaultToList(VaultListManager vaultListManager, Path vaultPath) throws IOException {
		if (!vaultListManager.containsVault(vaultPath)) {
			vaultListManager.add(vaultPath);
		}
	}

	public static Task<Void> createResetPasswordTask(ResourceBundle resourceBundle,Stage owner, RecoveryKeyFactory recoveryKeyFactory, Vault vault, StringProperty recoveryKey, NewPasswordController newPasswordController, Stage window, FxApplicationWindows appWindows, Dialogs dialogs) {

		Task<Void> task = new ResetPasswordTask(recoveryKeyFactory, vault, recoveryKey, newPasswordController);

		task.setOnScheduled(_ -> {
			LOG.debug("Using recovery key to reset password for {}.", vault.getDisplayablePath());
		});

		task.setOnSucceeded(_ -> {
			LOG.info("Used recovery key to reset password for {}.", vault.getDisplayablePath());
			if (vault.getState().equals(MASTERKEY_MISSING)) {
				dialogs.prepareRecoverPasswordSuccess(window, owner, resourceBundle).setTitleKey("recoveryKey.recoverMasterkey.title").setMessageKey("recoveryKey.recover.resetMasterkeyFileSuccess.message").build().showAndWait();
				window.close();
			} else {
				dialogs.prepareRecoverPasswordSuccess(window, owner, resourceBundle).build().showAndWait();
				window.close();
			}
		});

		task.setOnFailed(_ -> {
			LOG.error("Resetting password failed.", task.getException());
			appWindows.showErrorWindow(task.getException(), window, null);
		});

		return task;
	}


	public static class ResetPasswordTask extends Task<Void> {

		private static final Logger LOG = LoggerFactory.getLogger(ResetPasswordTask.class);
		private final RecoveryKeyFactory recoveryKeyFactory;
		private final Vault vault;
		private final StringProperty recoveryKey;
		private final NewPasswordController newPasswordController;

		public ResetPasswordTask(RecoveryKeyFactory recoveryKeyFactory, Vault vault, StringProperty recoveryKey, NewPasswordController newPasswordController) {
			this.recoveryKeyFactory = recoveryKeyFactory;
			this.vault = vault;
			this.recoveryKey = recoveryKey;
			this.newPasswordController = newPasswordController;

			setOnFailed(event -> LOG.error("Failed to reset password", getException()));
		}

		@Override
		protected Void call() throws IOException, IllegalArgumentException {
			recoveryKeyFactory.newMasterkeyFileWithPassphrase(vault.getPath(), recoveryKey.get(), newPasswordController.passwordField.getCharacters());
			return null;
		}
	}

	public static Optional<Vault> checkAndPrepareVaultFromDirectory(DirectoryChooser directoryChooser, Stage window, Dialogs dialogs, VaultComponent.Factory vaultComponentFactory, List<MountService> mountServices) {

		File selectedDirectory;
		do {
			selectedDirectory = directoryChooser.showDialog(window);
			if (selectedDirectory == null) {
				return Optional.empty();
			}
			boolean hasSubfolderD = new File(selectedDirectory, "d").isDirectory();

			if (!hasSubfolderD) {
				dialogs.prepareNoDDirectorySelectedDialog(window).build().showAndWait();
				selectedDirectory = null;
			}
		} while (selectedDirectory == null);

		Path selectedPath = selectedDirectory.toPath();
		VaultSettings vaultSettings = VaultSettings.withRandomId();
		vaultSettings.path.set(selectedPath);
		if (selectedPath.getFileName() != null) {
			vaultSettings.displayName.set(selectedPath.getFileName().toString());
		} else {
			vaultSettings.displayName.set("defaultVaultName");
		}

		var wrapper = new VaultConfigCache(vaultSettings);
		Vault vault = vaultComponentFactory.create(vaultSettings, wrapper, VAULT_CONFIG_MISSING, null).vault();

		// Spezialbehandlung für Windows + Dropbox + WinFsp
		var nameOfWinfspLocalMounter = "org.cryptomator.frontend.fuse.mount.WinFspMountProvider";
		if (SystemUtils.IS_OS_WINDOWS && vaultSettings.path.get().toString().contains("Dropbox") && mountServices.stream().anyMatch(s -> s.getClass().getName().equals(nameOfWinfspLocalMounter))) {
			vaultSettings.mountService.setValue(nameOfWinfspLocalMounter);
		}

		return Optional.of(vault);
	}

	public enum Type {
		RESTORE_VAULT_CONFIG,
		RESTORE_MASTERKEY,
		RESET_PASSWORD,
		SHOW_KEY,
		CONVERT_VAULT;
	}

}

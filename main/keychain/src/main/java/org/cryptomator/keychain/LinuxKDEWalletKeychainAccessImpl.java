package org.cryptomator.keychain;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.kde.KWallet;
import org.kde.Static;
import org.purejava.KDEWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinuxKDEWalletKeychainAccessImpl implements KeychainAccessStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(LinuxKDEWalletKeychainAccessImpl.class);

	private final String FOLDER_NAME = "Cryptomator";
	private final String APP_NAME = "Cryptomator";
	private DBusConnection connection;
	private KDEWallet wallet;
	private int handle = -1;

	public LinuxKDEWalletKeychainAccessImpl() throws KeychainAccessException {
		try {
			connection = DBusConnection.getConnection(DBusConnection.DBusBusType.SESSION);
		} catch (DBusException e) {
			LOG.error("Connecting to D-Bus failed:", e);
			throw new KeychainAccessException(e);
		}
	}

	@Override
	public boolean isSupported() {
		try {
			wallet = new KDEWallet(connection);
			return wallet.isEnabled();
		} catch (Exception e) {
			LOG.error("A KDEWallet could not be created:", e);
			return false;
		}
	}

	@Override
	public void storePassphrase(String key, CharSequence passphrase) throws KeychainAccessException {
		try {
			if (walletIsOpen() &&
					!(wallet.hasEntry(handle, FOLDER_NAME, key, APP_NAME)
							&& wallet.entryType(handle, FOLDER_NAME, key, APP_NAME) == 1)
					&& wallet.writePassword(handle, FOLDER_NAME, key, passphrase.toString(), APP_NAME) == 0) {
				LOG.debug("Passphrase successfully stored.");
			} else {
				LOG.debug("Passphrase was not stored.");
			}
		} catch (Exception e) {
			LOG.error("Storing the passphrase failed:", e);
			throw new KeychainAccessException(e);
		}
	}

	@Override
	public char[] loadPassphrase(String key) throws KeychainAccessException {
		String password = "";
		try {
			if (walletIsOpen()) {
				password = wallet.readPassword(handle, FOLDER_NAME, key, APP_NAME);
				LOG.debug("loadPassphrase: wallet is open.");
			} else {
				LOG.debug("loadPassphrase: wallet is closed.");
			}
			return (password.equals("")) ? null : password.toCharArray();
		} catch (Exception e) {
			LOG.error("Loading the passphrase failed:", e);
			throw new KeychainAccessException(e);
		}
	}

	@Override
	public void deletePassphrase(String key) throws KeychainAccessException {
		try {
			if (walletIsOpen()
					&& wallet.hasEntry(handle, FOLDER_NAME, key, APP_NAME)
					&& wallet.entryType(handle, FOLDER_NAME, key, APP_NAME) == 1
					&& wallet.removeEntry(handle, FOLDER_NAME, key, APP_NAME) == 0) {
				LOG.debug("Passphrase successfully deleted.");
			} else {
				LOG.debug("Passphrase was not deleted.");
			}
		} catch (Exception e) {
			LOG.error("Deleting the passphrase failed:", e);
			throw new KeychainAccessException(e);
		}
	}

	@Override
	public void changePassphrase(String key, CharSequence passphrase) throws KeychainAccessException {
		try {
			if (walletIsOpen()
					&& wallet.hasEntry(handle, FOLDER_NAME, key, APP_NAME)
					&& wallet.entryType(handle, FOLDER_NAME, key, APP_NAME) == 1
					&& wallet.writePassword(handle, FOLDER_NAME, key, passphrase.toString(), APP_NAME) == 0) {
				LOG.debug("Passphrase successfully changed.");
			} else {
				LOG.debug("Passphrase could not be changed.");
			}
		} catch (Exception e) {
			LOG.error("Changing the passphrase failed:", e);
			throw new KeychainAccessException(e);
		}
	}

	private boolean walletIsOpen() throws KeychainAccessException {
		try {
			if (wallet.isOpen(Static.DEFAULT_WALLET)) {
				// This is needed due to KeechainManager loading the passphase directly
				if (handle == -1) handle = wallet.open(Static.DEFAULT_WALLET, 0, APP_NAME);
				return true;
			}
			wallet.openAsync(Static.DEFAULT_WALLET, 0, APP_NAME, false);
			wallet.getSignalHandler().await(KWallet.walletAsyncOpened.class, Static.ObjectPaths.KWALLETD5, () -> null);
			handle = wallet.getSignalHandler().getLastHandledSignal(KWallet.walletAsyncOpened.class, Static.ObjectPaths.KWALLETD5).handle;
			LOG.debug("Wallet successfully initialized.");
			return handle != -1;
		} catch (Exception e) {
			LOG.error("Asynchronous opening the wallet failed:", e);
			throw new KeychainAccessException(e);
		}
	}
}

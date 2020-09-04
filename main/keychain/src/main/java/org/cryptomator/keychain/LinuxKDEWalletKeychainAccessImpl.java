package org.cryptomator.keychain;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.kde.KWallet;
import org.kde.Static;
import org.purejava.KDEWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinuxKDEWalletKeychainAccessImpl implements KeychainAccessStrategy {

    private final Logger log = LoggerFactory.getLogger(LinuxKDEWalletKeychainAccessImpl.class);

    private final String FOLDER_NAME = "Cryptomator";
    private final String APP_NAME = "Cryptomator";
    private DBusConnection connection;
    private KDEWallet wallet;
    private int handle = -1;

    public LinuxKDEWalletKeychainAccessImpl() {
        try {
            connection = DBusConnection.getConnection(DBusConnection.DBusBusType.SESSION);
        } catch (DBusException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isSupported() {
        try {
            wallet = new KDEWallet(connection);
            return wallet.isEnabled();
        } catch (Exception e) {
            e.printStackTrace();
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
                log.debug("Passphrase successfully stored.");
            } else {
                log.debug("Passphrase was not stored.");
            }
        } catch (Exception e) {
            log.error(e.toString(), e.getCause());
            throw new KeychainAccessException(e);
        }
    }

    @Override
    public char[] loadPassphrase(String key) throws KeychainAccessException {
        String password = "";
        try {
            if (walletIsOpen()) {
                password = wallet.readPassword(handle, FOLDER_NAME, key, APP_NAME);
                log.debug("loadPassphrase: wallet is open.");
            } else {
                log.debug("loadPassphrase: wallet is closed.");
            }
            return (password.equals("")) ? null : password.toCharArray();
        } catch (Exception e) {
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
                log.debug("Passphrase successfully deleted.");
            } else {
                log.debug("Passphrase was not deleted.");
            }
        } catch (Exception e) {
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
                log.debug("Passphrase successfully changed.");
            } else {
                log.debug("Passphrase could not be changed.");
            }
        } catch (Exception e) {
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
            log.debug("Wallet successfully initialized.");
            return handle != -1;
        } catch (Exception e) {
            throw new KeychainAccessException(e);
        }
    }
}

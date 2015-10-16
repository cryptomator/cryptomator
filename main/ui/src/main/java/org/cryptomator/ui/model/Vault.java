package org.cryptomator.ui.model;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.security.auth.DestroyFailedException;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.cryptomator.crypto.Cryptor;
import org.cryptomator.ui.util.DeferredClosable;
import org.cryptomator.ui.util.DeferredCloser;
import org.cryptomator.ui.util.FXThreads;
import org.cryptomator.ui.util.mount.CommandFailedException;
import org.cryptomator.ui.util.mount.WebDavMount;
import org.cryptomator.ui.util.mount.WebDavMounter;
import org.cryptomator.ui.util.mount.WebDavMounter.MountParam;
import org.cryptomator.webdav.WebDavServer;
import org.cryptomator.webdav.WebDavServer.ServletLifeCycleAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Vault implements Serializable {

	private static final long serialVersionUID = 3754487289683599469L;
	private static final Logger LOG = LoggerFactory.getLogger(Vault.class);

	public static final String VAULT_FILE_EXTENSION = ".cryptomator";
	public static final String VAULT_MASTERKEY_FILE = "masterkey.cryptomator";
	public static final String VAULT_MASTERKEY_BACKUP_FILE = "masterkey.cryptomator.bkup";

	private final Path path;
	private final WebDavServer server;
	private final Cryptor cryptor;
	private final WebDavMounter mounter;
	private final DeferredCloser closer;
	private final ObjectProperty<Boolean> unlocked = new SimpleObjectProperty<Boolean>(this, "unlocked", Boolean.FALSE);
	private final ObservableList<String> namesOfResourcesWithInvalidMac = FXThreads.observableListOnMainThread(FXCollections.observableArrayList());
	private final Set<String> whitelistedResourcesWithInvalidMac = new HashSet<>();

	private String mountName;
	private Character winDriveLetter;
	private DeferredClosable<ServletLifeCycleAdapter> webDavServlet = DeferredClosable.empty();
	private DeferredClosable<WebDavMount> webDavMount = DeferredClosable.empty();

	/**
	 * Package private constructor, use {@link VaultFactory}.
	 */
	Vault(final Path vaultDirectoryPath, final WebDavServer server, final Cryptor cryptor, final WebDavMounter mounter, final DeferredCloser closer) {
		this.path = vaultDirectoryPath;
		this.server = server;
		this.cryptor = cryptor;
		this.mounter = mounter;
		this.closer = closer;

		try {
			setMountName(getName());
		} catch (IllegalArgumentException e) {
			// mount name needs to be set by the user explicitly later
		}
	}

	public boolean isValidVaultDirectory() {
		return Files.isDirectory(path) && path.getFileName().toString().endsWith(VAULT_FILE_EXTENSION);
	}

	public boolean containsMasterKey() throws IOException {
		final Path masterKeyPath = path.resolve(VAULT_MASTERKEY_FILE);
		return Files.isRegularFile(masterKeyPath);
	}

	public synchronized boolean startServer() {
		namesOfResourcesWithInvalidMac.clear();
		whitelistedResourcesWithInvalidMac.clear();
		Optional<ServletLifeCycleAdapter> o = webDavServlet.get();
		if (o.isPresent() && o.get().isRunning()) {
			return false;
		}
		ServletLifeCycleAdapter servlet = server.createServlet(path, cryptor, namesOfResourcesWithInvalidMac, whitelistedResourcesWithInvalidMac, mountName);
		if (servlet.start()) {
			webDavServlet = closer.closeLater(servlet);
			return true;
		}
		return false;
	}

	public void stopServer() {
		try {
			unmount();
		} catch (CommandFailedException e) {
			LOG.warn("Unmounting failed. Locking anyway...", e);
		}
		webDavServlet.close();
		try {
			cryptor.destroy();
		} catch (DestroyFailedException e) {
			LOG.error("Destruction of cryptor throw an exception.", e);
		}
		whitelistedResourcesWithInvalidMac.clear();
		namesOfResourcesWithInvalidMac.clear();
	}

	private Map<MountParam, Optional<String>> getMountParams() {
		return ImmutableMap.of( //
				MountParam.MOUNT_NAME, Optional.ofNullable(mountName), //
				MountParam.WIN_DRIVE_LETTER, Optional.ofNullable(CharUtils.toString(winDriveLetter)) //
		);
	}

	public Boolean mount() {
		final ServletLifeCycleAdapter servlet = webDavServlet.get().orElse(null);
		if (servlet == null || !servlet.isRunning()) {
			return false;
		}
		try {
			webDavMount = closer.closeLater(mounter.mount(servlet.getServletUri(), getMountParams()));
			return true;
		} catch (CommandFailedException e) {
			LOG.warn("mount failed", e);
			return false;
		}
	}

	public void reveal() throws CommandFailedException {
		final WebDavMount mnt = webDavMount.get().orElse(null);
		if (mnt != null) {
			mnt.reveal();
		}
	}

	public void unmount() throws CommandFailedException {
		final WebDavMount mnt = webDavMount.get().orElse(null);
		if (mnt != null) {
			mnt.unmount();
		}
		webDavMount = DeferredClosable.empty();
	}

	/* Getter/Setter */

	public Path getPath() {
		return path;
	}

	/**
	 * @return Directory name without preceeding path components and file extension
	 */
	public String getName() {
		return StringUtils.removeEnd(path.getFileName().toString(), VAULT_FILE_EXTENSION);
	}

	public Cryptor getCryptor() {
		return cryptor;
	}

	public ObjectProperty<Boolean> unlockedProperty() {
		return unlocked;
	}

	public boolean isUnlocked() {
		return unlocked.get();
	}

	public void setUnlocked(boolean unlocked) {
		this.unlocked.set(unlocked);
	}

	public ObservableList<String> getNamesOfResourcesWithInvalidMac() {
		return namesOfResourcesWithInvalidMac;
	}

	public Set<String> getWhitelistedResourcesWithInvalidMac() {
		return whitelistedResourcesWithInvalidMac;
	}

	/**
	 * Tries to form a similar string using the regular latin alphabet.
	 * 
	 * @param string
	 * @return a string composed of a-z, A-Z, 0-9, and _.
	 */
	public static String normalize(String string) {
		String normalized = Normalizer.normalize(string, Form.NFD);
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < normalized.length(); i++) {
			char c = normalized.charAt(i);
			if (Character.isWhitespace(c)) {
				if (builder.length() == 0 || builder.charAt(builder.length() - 1) != '_') {
					builder.append('_');
				}
			} else if (c < 127 && Character.isLetterOrDigit(c)) {
				builder.append(c);
			} else if (c < 127) {
				if (builder.length() == 0 || builder.charAt(builder.length() - 1) != '_') {
					builder.append('_');
				}
			}
		}
		return builder.toString();
	}

	public String getMountName() {
		return mountName;
	}

	/**
	 * sets the mount name while normalizing it
	 * 
	 * @param mountName
	 * @throws IllegalArgumentException if the name is empty after normalization
	 */
	public void setMountName(String mountName) throws IllegalArgumentException {
		mountName = normalize(mountName);
		if (StringUtils.isEmpty(mountName)) {
			throw new IllegalArgumentException("mount name is empty");
		}
		this.mountName = mountName;
	}

	public Character getWinDriveLetter() {
		return winDriveLetter;
	}

	public void setWinDriveLetter(Character winDriveLetter) {
		this.winDriveLetter = winDriveLetter;
	}

	/* hashcode/equals */

	@Override
	public int hashCode() {
		return path.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Vault) {
			final Vault other = (Vault) obj;
			return this.path.equals(other.path);
		} else {
			return false;
		}
	}

}

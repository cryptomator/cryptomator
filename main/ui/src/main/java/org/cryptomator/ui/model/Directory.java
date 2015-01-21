package org.cryptomator.ui.model;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.apache.commons.lang3.StringUtils;
import org.cryptomator.crypto.Cryptor;
import org.cryptomator.crypto.SamplingDecorator;
import org.cryptomator.crypto.aes256.Aes256Cryptor;
import org.cryptomator.ui.MainApplication;
import org.cryptomator.ui.util.MasterKeyFilter;
import org.cryptomator.ui.util.mount.CommandFailedException;
import org.cryptomator.ui.util.mount.WebDavMount;
import org.cryptomator.ui.util.mount.WebDavMounter;
import org.cryptomator.webdav.WebDavServer;
import org.cryptomator.webdav.WebDavServer.ServletLifeCycleAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = DirectorySerializer.class)
@JsonDeserialize(using = DirectoryDeserializer.class)
public class Directory implements Serializable {

	private static final long serialVersionUID = 3754487289683599469L;
	private static final Logger LOG = LoggerFactory.getLogger(Directory.class);
	private final Cryptor cryptor = SamplingDecorator.decorate(new Aes256Cryptor());
	private final ObjectProperty<Boolean> unlocked = new SimpleObjectProperty<Boolean>(this, "unlocked", Boolean.FALSE);
	private final Runnable shutdownTask = new ShutdownTask();
	private final Path path;
	private boolean verifyFileIntegrity;
	private ServletLifeCycleAdapter webDavServlet;
	private WebDavMount webDavMount;

	public Directory(final Path path) {
		if (!Files.isDirectory(path)) {
			throw new IllegalArgumentException("Not a directory: " + path);
		}
		this.path = path;

	}

	public boolean containsMasterKey() throws IOException {
		return MasterKeyFilter.filteredDirectory(path).iterator().hasNext();
	}

	public synchronized boolean startServer() {
		if (webDavServlet != null && webDavServlet.isRunning()) {
			return false;
		}
		webDavServlet = WebDavServer.getInstance().createServlet(path, verifyFileIntegrity, cryptor);
		if (webDavServlet.start()) {
			MainApplication.addShutdownTask(shutdownTask);
			return true;
		} else {
			return false;
		}
	}

	public void stopServer() {
		if (webDavServlet != null && webDavServlet.isRunning()) {
			MainApplication.removeShutdownTask(shutdownTask);
			this.unmount();
			webDavServlet.stop();
			cryptor.swipeSensitiveData();
		}
	}

	public boolean mount() {
		if (webDavServlet == null || !webDavServlet.isRunning()) {
			return false;
		}
		try {
			webDavMount = WebDavMounter.mount(webDavServlet.getServletUri());
			return true;
		} catch (CommandFailedException e) {
			LOG.warn("mount failed", e);
			return false;
		}
	}

	public boolean unmount() {
		try {
			if (webDavMount != null) {
				webDavMount.unmount();
				webDavMount = null;
			}
			return true;
		} catch (CommandFailedException e) {
			LOG.warn("unmount failed", e);
			return false;
		}
	}

	/* Getter/Setter */

	public Path getPath() {
		return path;
	}

	public boolean shouldVerifyFileIntegrity() {
		return verifyFileIntegrity;
	}

	public void setVerifyFileIntegrity(boolean verifyFileIntegrity) {
		this.verifyFileIntegrity = verifyFileIntegrity;
	}

	/**
	 * @return Directory name without preceeding path components
	 */
	public String getName() {
		String name = path.getFileName().toString();
		if (StringUtils.endsWithIgnoreCase(name, Aes256Cryptor.FOLDER_EXTENSION)) {
			name = name.substring(0, name.length() - Aes256Cryptor.FOLDER_EXTENSION.length());
		}
		return name;
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

	/* hashcode/equals */

	@Override
	public int hashCode() {
		return path.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Directory) {
			final Directory other = (Directory) obj;
			return this.path.equals(other.path);
		} else {
			return false;
		}
	}

	/* graceful shutdown */

	private class ShutdownTask implements Runnable {

		@Override
		public void run() {
			stopServer();
		}

	}

}

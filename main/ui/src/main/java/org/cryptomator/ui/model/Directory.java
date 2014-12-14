package org.cryptomator.ui.model;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.cryptomator.crypto.Cryptor;
import org.cryptomator.crypto.SamplingDecorator;
import org.cryptomator.crypto.aes256.Aes256Cryptor;
import org.cryptomator.ui.MainApplication;
import org.cryptomator.ui.util.MasterKeyFilter;
import org.cryptomator.ui.util.webdav.CommandFailedException;
import org.cryptomator.ui.util.webdav.WebDavMount;
import org.cryptomator.ui.util.webdav.WebDavMounter;
import org.cryptomator.webdav.WebDAVServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = DirectorySerializer.class)
@JsonDeserialize(using = DirectoryDeserializer.class)
public class Directory implements Serializable {

	private static final long serialVersionUID = 3754487289683599469L;
	private static final Logger LOG = LoggerFactory.getLogger(Directory.class);

	private final WebDAVServer server = new WebDAVServer();
	private final Cryptor cryptor = SamplingDecorator.decorate(new Aes256Cryptor());
	private final ObjectProperty<Boolean> unlocked = new SimpleObjectProperty<Boolean>(this, "unlocked", Boolean.FALSE);
	private final Path path;
	// private boolean unlocked;
	private WebDavMount webDavMount;
	private final Runnable shutdownTask = new ShutdownTask();

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
		if (server.start(path.toString(), cryptor)) {
			MainApplication.addShutdownTask(shutdownTask);
			return true;
		} else {
			return false;
		}
	}

	public synchronized void stopServer() {
		if (server.isRunning()) {
			MainApplication.removeShutdownTask(shutdownTask);
			this.unmount();
			server.stop();
			cryptor.swipeSensitiveData();
		}
	}

	public boolean mount() {
		try {
			URI shareUri = URI.create(String.format("dav://localhost:%d", server.getPort()));
			webDavMount = WebDavMounter.mount(shareUri);
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

	/**
	 * @return Directory name without preceeding path components
	 */
	public String getName() {
		return path.getFileName().toString();
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

	public WebDAVServer getServer() {
		return server;
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

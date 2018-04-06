package org.cryptomator.ui.model;


import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.cryptofs.CryptoFileSystem;
import org.cryptomator.frontend.webdav.WebDavServer;
import org.cryptomator.frontend.webdav.mount.MountParams;
import org.cryptomator.frontend.webdav.mount.Mounter;
import org.cryptomator.frontend.webdav.servlet.WebDavServletController;

import javax.inject.Inject;
import javax.inject.Provider;

import java.net.InetAddress;
import java.net.UnknownHostException;

@VaultModule.PerVault
public class WebDavVolume implements Volume {

	private static final String LOCALHOST_ALIAS = "cryptomator-vault";

	private final Provider<WebDavServer> serverProvider;
	private final VaultSettings vaultSettings;
	private final Settings settings;

	private WebDavServer server;
	private WebDavServletController servlet;
	private Mounter.Mount mount;

	@Inject
	public WebDavVolume(Provider<WebDavServer> serverProvider, VaultSettings vaultSettings, Settings settings) {
		this.serverProvider = serverProvider;
		this.vaultSettings = vaultSettings;
		this.settings = settings;
	}

	@Override
	public void prepare(CryptoFileSystem fs) {
		if (server == null) {
			server = serverProvider.get();
		}
		if (!server.isRunning()) {
			server.start();
		}
		servlet = server.createWebDavServlet(fs.getPath("/"), vaultSettings.getId() + "/" + vaultSettings.mountName().get());
		servlet.start();
	}

	@Override
	public void mount() throws CommandFailedException {
		if (servlet == null) {
			throw new IllegalStateException("Mounting requires unlocked WebDAV servlet.");
		}
		MountParams mountParams = MountParams.create() //
				.withWindowsDriveLetter(vaultSettings.winDriveLetter().get()) //
				.withPreferredGvfsScheme(settings.preferredGvfsScheme().get())//
				.withWebdavHostname(getLocalhostAliasOrNull()) //
				.build();
		try {
			this.mount = servlet.mount(mountParams); // might block this thread for a while
		} catch (Mounter.CommandFailedException e) {
			e.printStackTrace();
			throw new CommandFailedException(e);
		}
	}

	@Override
	public void reveal() throws CommandFailedException {
		try {
			mount.reveal();
		} catch (Mounter.CommandFailedException e) {
			e.printStackTrace();
			throw new CommandFailedException(e);
		}
	}

	@Override
	public synchronized void unmount() throws CommandFailedException {
		try {
			mount.unmount();
		} catch (Mounter.CommandFailedException e) {
			throw new CommandFailedException(e);
		}
	}

	@Override
	public synchronized void unmountForced() {
		mount.forced();
	}

	private String getLocalhostAliasOrNull() {
		try {
			InetAddress alias = InetAddress.getByName(LOCALHOST_ALIAS);
			if (alias.getHostAddress().equals("127.0.0.1")) {
				return LOCALHOST_ALIAS;
			} else {
				return null;
			}
		} catch (UnknownHostException e) {
			return null;
		}
	}

	@Override
	public void stop() {
		if (servlet != null) {
			servlet.stop();
		}

	}

	public synchronized String getMountUri() {
		return servlet.getServletRootUri().toString() + "/";
	}

	/**
	 * TODO: what to check wether it is implemented?
	 *
	 * @return
	 */
	@Override
	public boolean isSupported() {
		return true;
	}

	public boolean supportsForcedUnmount() {
		return mount != null && mount.forced().isPresent();
	}
}

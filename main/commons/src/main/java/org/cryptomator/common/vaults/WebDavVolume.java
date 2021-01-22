package org.cryptomator.common.vaults;


import com.google.common.base.CharMatcher;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.settings.VolumeImpl;
import org.cryptomator.cryptofs.CryptoFileSystem;
import org.cryptomator.frontend.webdav.WebDavServer;
import org.cryptomator.frontend.webdav.mount.MountParams;
import org.cryptomator.frontend.webdav.mount.Mounter;
import org.cryptomator.frontend.webdav.servlet.WebDavServletController;

import javax.inject.Inject;
import javax.inject.Provider;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

public class WebDavVolume implements Volume {

	private static final String LOCALHOST_ALIAS = "cryptomator-vault";

	private final Provider<WebDavServer> serverProvider;
	private final VaultSettings vaultSettings;
	private final Settings settings;
	private final WindowsDriveLetters windowsDriveLetters;

	private WebDavServer server;
	private WebDavServletController servlet;
	private Mounter.Mount mount;

	@Inject
	public WebDavVolume(Provider<WebDavServer> serverProvider, VaultSettings vaultSettings, Settings settings, WindowsDriveLetters windowsDriveLetters) {
		this.serverProvider = serverProvider;
		this.vaultSettings = vaultSettings;
		this.settings = settings;
		this.windowsDriveLetters = windowsDriveLetters;
	}

	@Override
	public void mount(CryptoFileSystem fs, String mountFlags) throws VolumeException {
		startServlet(fs);
		mountServlet();
	}

	private void startServlet(CryptoFileSystem fs){
		if (server == null) {
			server = serverProvider.get();
		}
		if (!server.isRunning()) {
			server.start();
		}
		CharMatcher acceptable = CharMatcher.inRange('0', '9').or(CharMatcher.inRange('A', 'Z')).or(CharMatcher.inRange('a', 'z'));
		String urlConformMountName = acceptable.negate().collapseFrom(vaultSettings.mountName().get(), '_');
		servlet = server.createWebDavServlet(fs.getPath("/"), vaultSettings.getId() + "/" + urlConformMountName);
		servlet.start();
	}

	private void mountServlet() throws VolumeException {
		if (servlet == null) {
			throw new IllegalStateException("Mounting requires unlocked WebDAV servlet.");
		}

		//on windows, prevent an automatic drive letter selection in the upstream library. Either we choose already a specifc one or there is no free.
		Supplier<String> driveLetterSupplier;
		if(System.getProperty("os.name").toLowerCase().contains("windows") && vaultSettings.winDriveLetter().isEmpty().get()) {
			driveLetterSupplier = () -> windowsDriveLetters.getAvailableDriveLetter().orElse(null);
		} else {
			driveLetterSupplier = () -> vaultSettings.winDriveLetter().get();
		}

		MountParams mountParams = MountParams.create() //
				.withWindowsDriveLetter(driveLetterSupplier.get()) //
				.withPreferredGvfsScheme(settings.preferredGvfsScheme().get().getPrefix())//
				.withWebdavHostname(getLocalhostAliasOrNull()) //
				.build();
		try {
			this.mount = servlet.mount(mountParams); // might block this thread for a while
		} catch (Mounter.CommandFailedException e) {
			throw new VolumeException(e);
		}
	}

	@Override
	public void reveal(Revealer revealer) throws VolumeException {
		try {
			mount.reveal(revealer::reveal);
		} catch (Exception e) {
			throw new VolumeException(e);
		}
	}

	@Override
	public synchronized void unmount() throws VolumeException {
		try {
			mount.unmount();
		} catch (Mounter.CommandFailedException e) {
			throw new VolumeException(e);
		}
		cleanup();
	}

	@Override
	public synchronized void unmountForced() throws VolumeException {
		try {
			mount.forced().orElseThrow(IllegalStateException::new).unmount();
		} catch (Mounter.CommandFailedException e) {
			throw new VolumeException(e);
		}
		cleanup();
	}

	@Override
	public Optional<Path> getMountPoint() {
		return mount.getMountPoint();
	}

	@Override
	public MountPointRequirement getMountPointRequirement() {
		return MountPointRequirement.NONE;
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

	private void cleanup() {
		if (servlet != null) {
			servlet.stop();
		}

	}

	@Override
	public boolean isSupported() {
		return WebDavVolume.isSupportedStatic();
	}

	@Override
	public VolumeImpl getImplementationType() {
		return VolumeImpl.WEBDAV;
	}

	@Override
	public boolean supportsForcedUnmount() {
		return mount != null && mount.forced().isPresent();
	}


	public static boolean isSupportedStatic() {
		return true;
	}
}

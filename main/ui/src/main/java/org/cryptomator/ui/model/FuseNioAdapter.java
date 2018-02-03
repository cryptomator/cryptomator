package org.cryptomator.ui.model;

import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.cryptofs.CryptoFileSystem;
import org.cryptomator.frontend.fuse.AdapterFactory;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

@VaultModule.PerVault
public class FuseNioAdapter implements NioAdapter {

	private static final Logger LOG = LoggerFactory.getLogger(FuseNioAdapter.class);
	private static final String AUTOASSIGN_DRRIVE_LETTER = "*";

	private enum OS {
		WINDOWS,
		LINUX,
		MAC;

		public static OS getCurrentOS() {
			if (SystemUtils.IS_OS_WINDOWS) {
				return WINDOWS;
			} else if (SystemUtils.IS_OS_MAC) {
				return MAC;
			} else {
				return LINUX;
			}
		}

	}


	private final VaultSettings vaultSettings;
	private final Settings settings;
	private final WindowsDriveLetters windowsDriveLetters;
	private final OS os = OS.getCurrentOS();
	private org.cryptomator.frontend.fuse.FuseNioAdapter ffs;
	private String mountNameAndId;
	private String mountURL;
	private CryptoFileSystem cfs;

	@Inject
	public FuseNioAdapter(VaultSettings vaultSettings, Settings settings, WindowsDriveLetters windowsDriveLetters) {
		this.vaultSettings = vaultSettings;
		this.settings = settings;
		this.windowsDriveLetters = windowsDriveLetters;
	}

	@Override
	public void unlock(CryptoFileSystem fs) {
		this.cfs = fs;
		ffs = AdapterFactory.createReadWriteAdapter(fs.getPath("/"));
	}

	/**
	 * TODO: should createTempDirectory() be used instead of createDirectory()?
	 *
	 * @throws CommandFailedException
	 */
	@Override
	public void mount() throws CommandFailedException {
		ArrayList<String> mountOptions = new ArrayList<>(8);
		mountOptions.add(("-oatomic_o_trunc"));
		Path path;
		try {
			switch (os) {
				case MAC:
					path = Paths.get(vaultSettings.mountPath().get() + vaultSettings.mountName().get());
					createVaultDirIfNotExist(path);
					mountOptions.add("-ouid=" + getUIdOrGID("uid"));
					mountOptions.add("-ogid=" + getUIdOrGID("gid"));
					mountOptions.add("-ovolname=" + vaultSettings.mountName().get());
					mountOptions.add("-oauto_xattr");
					break;
				case WINDOWS:
					if (vaultSettings.winDriveLetter().get().equals(AUTOASSIGN_DRRIVE_LETTER)) {
						if (!windowsDriveLetters.getAvailableDriveLetters().isEmpty()) {
							path = Paths.get(windowsDriveLetters.getAvailableDriveLetters().iterator().next() + ":\\");
						} else {
							throw new CommandFailedException("No free drive letter to mount.");
						}
					} else {
						path = Paths.get(vaultSettings.winDriveLetter().get() + ":\\");
					}
					mountOptions.add("-ouid=-1");
					mountOptions.add("-ogid=-1");
					mountOptions.add("-ovolname=" + vaultSettings.mountName().get());
					mountOptions.add("-oFileInfoTimeout=-1");
					break;
				case LINUX:
					path = Paths.get(vaultSettings.mountPath().get() + vaultSettings.mountName().get());
					createVaultDirIfNotExist(path);
					mountOptions.add("-ouid=" + getUIdOrGID("uid"));
					mountOptions.add("-ogid=" + getUIdOrGID("gid"));
					mountOptions.add("-oauto_unmount");
					mountOptions.add("-ofsname=CryptoFs");
					break;
				default:
					throw new IllegalStateException("Not Supported OS.");
			}
			ffs.mount(path, false, false, mountOptions.toArray(new String[mountOptions.size()]));
			mountURL = path.toAbsolutePath().toUri().toURL().toString();
		} catch (Exception e) {
			throw new CommandFailedException("Unable to mount Filesystem", e);
		}
	}

	private void createVaultDirIfNotExist(Path p) throws IOException {
		try {
			if (Files.exists(p)) {
				if (Files.isDirectory(p)) {
					if (Files.newDirectoryStream(p).iterator().hasNext()) {
						return;
					} else {
						throw new DirectoryNotEmptyException("Directory not empty.");
					}
				}
			} else {
				Files.createDirectory(p);
			}
		} catch (IOException e) {
			throw e;
		}
	}

	private String getUIdOrGID(String idtype) throws IOException {
		String id;
		String parameter;
		switch (idtype) {
			case "uid":
				parameter = "-u";
				break;
			case "gid":
				parameter = "-g";
				break;
			default:
				throw new IllegalArgumentException("Unkown ID type");
		}
		Process getId = new ProcessBuilder("sh", "-c", "id " + parameter).start();
		Scanner s = new Scanner(getId.getInputStream()).useDelimiter("\\A");
		try {
			getId.waitFor(1000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		id = s.nextLine();
		return id;
	}

	@Override
	public synchronized void unmount() throws CommandFailedException {
		if (!(cfs.getStats().pollBytesRead() > 0 || cfs.getStats().pollBytesWritten() > 0)) {
			unmountForced();
		} else {
			throw new CommandFailedException("Pending read or write operations.");
		}
	}

	@Override
	public synchronized void unmountForced() throws CommandFailedException {
		ffs.umount();
	}

	@Override
	public void stop() {
		switch (os) {
			case WINDOWS:
				return;
			case MAC:
			case LINUX:
				try {
					Files.deleteIfExists(Paths.get(vaultSettings.mountPath().get() + vaultSettings.mountName().get()));
				} catch (IOException e) {
					LOG.warn("Could not delete mount directory of vault " + vaultSettings.mountName());
					e.printStackTrace();
				}
				return;
			default:
				return;
		}

	}

	@Override
	public String getFilesystemRootUrl() {
		return mountURL;
	}

	/**
	 * TODO: what should i check here?
	 */
	@Override
	public boolean isSupported() {
		switch (os) {
			case LINUX:
				return true;
			case WINDOWS:
				break;
			case MAC:
				break;
			default:
				return false;
		}
		return true;
	}

	@Override
	public boolean supportsForcedUnmount() {
		return true;
	}

}

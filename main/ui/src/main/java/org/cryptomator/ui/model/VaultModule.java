/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.model;

import dagger.Module;
import dagger.Provides;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.settings.VolumeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

@Module
public class VaultModule {

	private static final Logger LOG = LoggerFactory.getLogger(VaultModule.class);

	@Provides
	public Volume provideVolume(Settings settings, WebDavVolume webDavVolume, FuseVolume fuseVolume, DokanyVolume dokanyVolume) {
		VolumeImpl preferredImpl = settings.preferredVolumeImpl().get();
		if (VolumeImpl.DOKANY == preferredImpl && dokanyVolume.isSupported()) {
			return dokanyVolume;
		} else if (VolumeImpl.FUSE == preferredImpl && fuseVolume.isSupported()) {
			return fuseVolume;
		} else {
			if (VolumeImpl.WEBDAV != preferredImpl) {
				LOG.warn("Using WebDAV, because {} is not supported.", preferredImpl.getDisplayName());
			}
			assert webDavVolume.isSupported();
			return webDavVolume;
		}
	}

	@Provides
	@PerVault
	@DefaultMountFlags
	public Supplier<String> provideDefaultMountFlags(Settings settings, VaultSettings vaultSettings) {
		return () -> {
			VolumeImpl preferredImpl = settings.preferredVolumeImpl().get();
			switch (preferredImpl) {
				case FUSE:
					if (SystemUtils.IS_OS_MAC_OSX) {
						return getMacFuseDefaultMountFlags(settings, vaultSettings);
					} else if (SystemUtils.IS_OS_LINUX) {
						return getLinuxFuseDefaultMountFlags(settings, vaultSettings);
					}
				case DOKANY:
					return getDokanyDefaultMountFlags(settings, vaultSettings);
				default:
					return "--flags-supported-on-FUSE-or-DOKANY-only";
			}
		};
	}

	// see: https://github.com/osxfuse/osxfuse/wiki/Mount-options
	private String getMacFuseDefaultMountFlags(Settings settings, VaultSettings vaultSettings) {
		assert SystemUtils.IS_OS_MAC_OSX;

		StringBuilder flags = new StringBuilder();
		if (vaultSettings.usesReadOnlyMode().get()) {
			flags.append(" -ordonly");
		}
		flags.append(" -ovolname=").append(vaultSettings.mountName().get());
		flags.append(" -oatomic_o_trunc");
		flags.append(" -oauto_xattr");
		flags.append(" -oauto_cache");
		flags.append(" -omodules=iconv,from_code=UTF-8,to_code=UTF-8-MAC"); // show files names in Unicode NFD encoding
		flags.append(" -onoappledouble"); // vastly impacts performance for some reason...
		flags.append(" -odefault_permissions"); // let the kernel assume permissions based on file attributes etc

		try {
			Path userHome = Paths.get(System.getProperty("user.home"));
			int uid = (int) Files.getAttribute(userHome, "unix:uid");
			int gid = (int) Files.getAttribute(userHome, "unix:gid");
			flags.append(" -ouid=").append(uid);
			flags.append(" -ogid=").append(gid);
		} catch (IOException e) {
			LOG.error("Could not read uid/gid from USER_HOME", e);
		}

		return flags.toString().strip();
	}

	// see https://manpages.debian.org/testing/fuse/mount.fuse.8.en.html
	private String getLinuxFuseDefaultMountFlags(Settings settings, VaultSettings vaultSettings) {
		assert SystemUtils.IS_OS_LINUX;

		StringBuilder flags = new StringBuilder();
		if (vaultSettings.usesReadOnlyMode().get()) {
			flags.append(" -oro");
		}
		flags.append(" -oauto_unmount");

		try {
			Path userHome = Paths.get(System.getProperty("user.home"));
			int uid = (int) Files.getAttribute(userHome, "unix:uid");
			int gid = (int) Files.getAttribute(userHome, "unix:gid");
			flags.append(" -ouid=").append(uid);
			flags.append(" -ogid=").append(gid);
		} catch (IOException e) {
			LOG.error("Could not read uid/gid from USER_HOME", e);
		}

		return flags.toString().strip();
	}

	// see https://github.com/cryptomator/dokany-nio-adapter/blob/develop/src/main/java/org/cryptomator/frontend/dokany/MountUtil.java#L30-L34
	private String getDokanyDefaultMountFlags(Settings settings, VaultSettings vaultSettings) {
		assert SystemUtils.IS_OS_WINDOWS;

		StringBuilder flags = new StringBuilder();
		flags.append(" --options CURRENT_SESSION");
		if (vaultSettings.usesReadOnlyMode().get()) {
			flags.append(",WRITE_PROTECTION");
		}
		flags.append(" --thread-count 5");
		flags.append(" --timeout 10000");
		flags.append(" --allocation-unit-size 4096");
		flags.append(" --sector-size 4096");
		return flags.toString().strip();
	}

}

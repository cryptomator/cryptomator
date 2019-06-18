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
		VolumeImpl preferredImpl = settings.preferredVolumeImpl().get();
		switch (preferredImpl) {
			case FUSE:
				if (SystemUtils.IS_OS_MAC_OSX) {
					return () -> getMacFuseDefaultMountFlags(settings, vaultSettings);
				} else if (SystemUtils.IS_OS_LINUX) {
					return () -> getLinuxFuseDefaultMountFlags(settings, vaultSettings);
				}
			case DOKANY:
				return () -> getDokanyDefaultMountFlags(settings, vaultSettings);
			default:
				return () -> "--flags-supported-on-FUSE-or-DOKANY-only";
		}
	}

	private String getMacFuseDefaultMountFlags(Settings settings, VaultSettings vaultSettings) {
		assert SystemUtils.IS_OS_MAC_OSX;
		// see: https://github.com/osxfuse/osxfuse/wiki/Mount-options
		try {
			Path userHome = Paths.get(System.getProperty("user.home"));
			int uid = (int) Files.getAttribute(userHome, "unix:uid");
			int gid = (int) Files.getAttribute(userHome, "unix:gid");
			return "-ovolname=" + vaultSettings.mountName().get() // volume name
					+ " -ouid=" + uid //
					+ " -ogid=" + gid //
					+ " -oatomic_o_trunc" //
					+ " -oauto_xattr" //
					+ " -oauto_cache" //
					+ " -omodules=iconv,from_code=UTF-8,to_code=UTF-8-MAC" // show files names in Unicode NFD encoding
					+ " -onoappledouble" // vastly impacts performance for some reason...
					+ " -odefault_permissions"; // let the kernel assume permissions based on file attributes etc
		} catch (IOException e) {
			LOG.error("Could not read uid/gid from USER_HOME", e);
			return "";
		}
	}

	private String getLinuxFuseDefaultMountFlags(Settings settings, VaultSettings vaultSettings) {
		assert SystemUtils.IS_OS_LINUX;
		try {
			Path userHome = Paths.get(System.getProperty("user.home"));
			int uid = (int) Files.getAttribute(userHome, "unix:uid");
			int gid = (int) Files.getAttribute(userHome, "unix:gid");
			return "-oauto_unmount" //
					+ " -ouid=" + uid //
					+ " -ogid=" + gid;
		} catch (IOException e) {
			LOG.error("Could not read uid/gid from USER_HOME", e);
			return "";
		}
	}

	private String getDokanyDefaultMountFlags(Settings settings, VaultSettings vaultSettings) {
		// TODO
		return "--not-yet-supported";
	}

}

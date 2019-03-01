/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.model;

import dagger.Module;
import dagger.Provides;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.VolumeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Scope;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Module
public class VaultModule {

	private static final Logger LOG = LoggerFactory.getLogger(VaultModule.class);

	@Scope
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@interface PerVault {

	}

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

}

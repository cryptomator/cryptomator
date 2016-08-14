/*******************************************************************************
 * Copyright (c) 2014, 2016 Sebastian Stenzel, Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation, strategy fine tuning
 *     Markus Kreusch - Refactored WebDavMounter to use strategy pattern
 ******************************************************************************/
package org.cryptomator.frontend.webdav.mount;

import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.frontend.CommandFailedException;
import org.cryptomator.frontend.Frontend.MountParam;
import org.cryptomator.frontend.webdav.mount.command.Script;

@Singleton
final class MacOsXShellScriptWebDavMounter implements WebDavMounterStrategy {

	private final Comparator<String> semVerComparator;

	@Inject
	MacOsXShellScriptWebDavMounter(@Named("SemVer") Comparator<String> semVerComparator) {
		this.semVerComparator = semVerComparator;
	}

	@Override
	public boolean shouldWork(Map<MountParam, Optional<String>> mountParams) {
		return SystemUtils.IS_OS_MAC_OSX && semVerComparator.compare(SystemUtils.OS_VERSION, "10.10") < 0;
	}

	@Override
	public void warmUp(int serverPort) {
		// no-op
	}

	@Override
	public WebDavMount mount(URI uri, Map<MountParam, Optional<String>> mountParams) throws CommandFailedException {
		final String mountName = mountParams.getOrDefault(MountParam.MOUNT_NAME, Optional.empty()).orElseThrow(() -> {
			return new IllegalArgumentException("Missing mount parameter MOUNT_NAME.");
		});

		// we don't use the uri to derive a path, as it *could* be longer than 255 chars.
		final String path = "/Volumes/Cryptomator_" + UUID.randomUUID().toString();
		final Script mountScript = Script.fromLines("mkdir \"$MOUNT_PATH\"", "mount_webdav -S -v $MOUNT_NAME \"$DAV_AUTHORITY$DAV_PATH\" \"$MOUNT_PATH\"").addEnv("DAV_AUTHORITY", uri.getRawAuthority())
				.addEnv("DAV_PATH", uri.getRawPath()).addEnv("MOUNT_PATH", path).addEnv("MOUNT_NAME", mountName);
		mountScript.execute();
		return new MacWebDavMount(path);
	}

	private static class MacWebDavMount extends AbstractWebDavMount {
		private final String mountPath;
		private final Script revealScript;
		private final Script unmountScript;

		private MacWebDavMount(String mountPath) {
			this.mountPath = mountPath;
			this.revealScript = Script.fromLines("open \"$MOUNT_PATH\"").addEnv("MOUNT_PATH", mountPath);
			this.unmountScript = Script.fromLines("diskutil umount $MOUNT_PATH").addEnv("MOUNT_PATH", mountPath);
		}

		@Override
		public void unmount() throws CommandFailedException {
			// only attempt unmount if user didn't unmount manually:
			if (Files.exists(FileSystems.getDefault().getPath(mountPath))) {
				unmountScript.execute();
			}
		}

		@Override
		public void reveal() throws CommandFailedException {
			revealScript.execute();
		}

	}

}

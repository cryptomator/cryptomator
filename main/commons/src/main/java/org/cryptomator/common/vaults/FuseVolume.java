package org.cryptomator.common.vaults;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.mountpoint.InvalidMountPointException;
import org.cryptomator.common.mountpoint.MountPointChooser;
import org.cryptomator.common.settings.VolumeImpl;
import org.cryptomator.cryptofs.CryptoFileSystem;
import org.cryptomator.frontend.fuse.mount.CommandFailedException;
import org.cryptomator.frontend.fuse.mount.EnvironmentVariables;
import org.cryptomator.frontend.fuse.mount.FuseMountFactory;
import org.cryptomator.frontend.fuse.mount.FuseNotSupportedException;
import org.cryptomator.frontend.fuse.mount.Mount;
import org.cryptomator.frontend.fuse.mount.Mounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FuseVolume extends AbstractVolume {

	private static final Logger LOG = LoggerFactory.getLogger(FuseVolume.class);

	private static final Pattern pattern = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'"); //Thanks to https://stackoverflow.com/a/366532

	private Mount mount;

	@Inject
	public FuseVolume(@Named("orderedMountPointChoosers") SortedSet<MountPointChooser> choosers) {
		super(choosers);
	}

	@Override
	public void mount(CryptoFileSystem fs, String mountFlags) throws InvalidMountPointException, VolumeException {
		this.mountPoint = determineMountPoint();

		mount(fs.getPath("/"), mountFlags);
	}

	private void mount(Path root, String mountFlags) throws VolumeException {
		try {
			Mounter mounter = FuseMountFactory.getMounter();
			EnvironmentVariables envVars = EnvironmentVariables.create() //
					.withFlags(splitFlags(mountFlags)).withMountPoint(mountPoint) //
					.build();
			this.mount = mounter.mount(root, envVars);
		} catch (CommandFailedException | FuseNotSupportedException e) {
			throw new VolumeException("Unable to mount Filesystem", e);
		}
	}

	private String[] splitFlags(String str) {
		List<String> strings = new ArrayList<>();
		List<MatchResult> results = pattern.matcher(str).results().collect(Collectors.toList());
		for (int i = 0; i < results.size(); i++) {
			MatchResult current = results.get(i);
			MatchResult next = i + 1 < results.size() ? results.get(i + 1) : null;
			if (getSpecialString(next) != null && current.group().endsWith("=")) {
				//"next" is a quoted elements and "current" is missing it's argument
				//--> "next" must be joined with "current" and is skipped in the regular iteration
				strings.add(current.group() + getSpecialString(next));
				i++;
			} else {
				//"next" is a normal unquoted string/is not missing from "current"
				//--> Add "current" and advance
				strings.add(current.group());
			}
		}
		return strings.toArray(new String[0]);
	}

	private String getSpecialString(MatchResult result) {
		if (result == null) {
			return null;
		}
		return result.group(1) != null ? result.group(1) : result.group(2);
	}

	@Override
	public void reveal() throws VolumeException {
		try {
			mount.revealInFileManager();
		} catch (CommandFailedException e) {
			LOG.debug("Revealing the vault in file manger failed: " + e.getMessage());
			throw new VolumeException(e);
		}
	}

	@Override
	public boolean supportsForcedUnmount() {
		return true;
	}

	@Override
	public synchronized void unmountForced() throws VolumeException {
		try {
			mount.unmountForced();
			mount.close();
		} catch (CommandFailedException e) {
			throw new VolumeException(e);
		}
		cleanupMountPoint();
	}

	@Override
	public synchronized void unmount() throws VolumeException {
		try {
			mount.unmount();
			mount.close();
		} catch (CommandFailedException e) {
			throw new VolumeException(e);
		}
		cleanupMountPoint();
	}

	@Override
	public boolean isSupported() {
		return FuseVolume.isSupportedStatic();
	}

	@Override
	public VolumeImpl getImplementationType() {
		return VolumeImpl.FUSE;
	}

	@Override
	public MountPointRequirement getMountPointRequirement() {
		return SystemUtils.IS_OS_WINDOWS ? MountPointRequirement.PARENT_NO_MOUNT_POINT : MountPointRequirement.EMPTY_MOUNT_POINT;
	}

	public static boolean isSupportedStatic() {
		return FuseMountFactory.isFuseSupported();
	}

}

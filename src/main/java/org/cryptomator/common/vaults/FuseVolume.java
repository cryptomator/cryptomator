package org.cryptomator.common.vaults;

import com.google.common.collect.Iterators;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.mountpoint.InvalidMountPointException;
import org.cryptomator.common.mountpoint.MountPointChooser;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.settings.VolumeImpl;
import org.cryptomator.cryptofs.CryptoFileSystem;
import org.cryptomator.frontend.fuse.mount.EnvironmentVariables;
import org.cryptomator.frontend.fuse.mount.FuseMountException;
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
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class FuseVolume extends AbstractVolume {

	private static final Logger LOG = LoggerFactory.getLogger(FuseVolume.class);
	private static final Pattern NON_WHITESPACE_OR_QUOTED = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'"); // Thanks to https://stackoverflow.com/a/366532

	private final VaultSettings vaultSettings;

	private Mount mount;

	@Inject
	public FuseVolume(VaultSettings vaultSettings, @Named("orderedMountPointChoosers") Iterable<MountPointChooser> choosers) {
		super(choosers);
		this.vaultSettings = vaultSettings;
	}

	@Override
	public void mount(CryptoFileSystem fs, String mountFlags, Consumer<Throwable> onExitAction) throws InvalidMountPointException, VolumeException {
		this.mountPoint = determineMountPoint();
		mount(fs.getPath("/"), mountFlags, onExitAction);
	}

	private void mount(Path root, String mountFlags, Consumer<Throwable> onExitAction) throws VolumeException {
		try {
			Mounter mounter = FuseMountFactory.getMounter();
			EnvironmentVariables envVars = EnvironmentVariables.create() //
					.withFlags(splitFlags(mountFlags)) //
					.withMountPoint(mountPoint) //
					.withFileNameTranscoder(mounter.defaultFileNameTranscoder()) //
					.build();
			this.mount = mounter.mount(root, envVars, onExitAction);
		} catch (FuseMountException | FuseNotSupportedException e) {
			throw new VolumeException("Unable to mount Filesystem", e);
		}
	}

	private String[] splitFlags(String str) {
		List<String> flags = new ArrayList<>();
		var matches = Iterators.peekingIterator(NON_WHITESPACE_OR_QUOTED.matcher(str).results().iterator());
		while (matches.hasNext()) {
			String flag = matches.next().group();
			// check if flag is missing its argument:
			if (flag.endsWith("=") && matches.hasNext() && matches.peek().group(1) != null) { // next is "double quoted"
				// next is "double quoted" and flag is missing its argument
				flag += matches.next().group(1);
			} else if (flag.endsWith("=") && matches.hasNext() && matches.peek().group(2) != null) {
				// next is 'single quoted' and flag is missing its argument
				flag += matches.next().group(2);
			}
			flags.add(flag);
		}
		return flags.toArray(String[]::new);
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
	public boolean supportsForcedUnmount() {
		return true;
	}

	@Override
	public synchronized void unmountForced() throws VolumeException {
		try {
			mount.unmountForced();
		} catch (FuseMountException e) {
			throw new VolumeException(e);
		}
		cleanupMountPoint();
	}

	@Override
	public synchronized void unmount() throws VolumeException {
		try {
			mount.unmount();
		} catch (FuseMountException e) {
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
		if (!SystemUtils.IS_OS_WINDOWS) {
			return MountPointRequirement.EMPTY_MOUNT_POINT;
		}
		return this.vaultSettings.getWinDriveLetter().isPresent() ? MountPointRequirement.UNUSED_ROOT_DIR : MountPointRequirement.PARENT_NO_MOUNT_POINT;
	}

	public static boolean isSupportedStatic() {
		return FuseMountFactory.isFuseSupported();
	}

}

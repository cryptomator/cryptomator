package org.cryptomator.ui.model;

import org.cryptomator.common.settings.VaultSettings;

import javax.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

@VaultModule.PerVault
public class WindowsFuseEnvironment implements FuseEnvironment{

	private static final String AUTOASSIGN_DRRIVE_LETTER = "*";

	private final VaultSettings vaultSettings;
	private final WindowsDriveLetters windowsDriveLetters;

	private Path root;

	@Inject
	public WindowsFuseEnvironment(VaultSettings vaultSettings, WindowsDriveLetters windowsDriveLetters){
		this.vaultSettings = vaultSettings;
		this.windowsDriveLetters = windowsDriveLetters;
	}

	@Override
	public void prepare() throws CommandFailedException {
		if (vaultSettings.winDriveLetter().get().equals(AUTOASSIGN_DRRIVE_LETTER)) {
			if (!windowsDriveLetters.getAvailableDriveLetters().isEmpty()) {
				root= Paths.get(windowsDriveLetters.getAvailableDriveLetters().iterator().next() + ":\\").toAbsolutePath();
			} else {
				throw new CommandFailedException("No free drive letter to mount.");
			}
		} else {
			root = Paths.get(vaultSettings.winDriveLetter().get() + ":\\").toAbsolutePath();
		}
	}

	@Override
	public String[] getMountParameters() throws CommandFailedException {
		ArrayList<String> mountOptions = new ArrayList<>(8);
		mountOptions.add(("-oatomic_o_trunc"));
		mountOptions.add("-ouid=-1");
		mountOptions.add("-ogid=-1");
		mountOptions.add("-ovolname=" + vaultSettings.mountName().get());
		mountOptions.add("-oFileInfoTimeout=-1");
		return mountOptions.toArray(new String [mountOptions.size()]);
	}

	@Override
	public Path getFsRootPath() {
		return root;
	}

	@Override
	public void revealFsRootInFilesystemManager() throws CommandFailedException {
		throw new CommandFailedException("Not Implemented");
	}

	@Override
	public void cleanUp() {

	}

	@Override
	public boolean supportsFuse() {
		return false;
	}
}

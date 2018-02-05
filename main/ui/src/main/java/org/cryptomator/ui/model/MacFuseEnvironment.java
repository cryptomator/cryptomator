package org.cryptomator.ui.model;

import org.cryptomator.common.settings.VaultSettings;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class MacFuseEnvironment implements FuseEnvironment {

	private final VaultSettings vaultSettings;
	private Path root;

	@Inject
	public MacFuseEnvironment(VaultSettings vaultSettings){
		this.vaultSettings = vaultSettings;
	}

	@Override
	public void prepare() throws CommandFailedException {
		this.root = Paths.get(vaultSettings.mountPath().get() + vaultSettings.mountName().get()).toAbsolutePath();
		try {
			createVaultDirIfNotExist(root);
		} catch (IOException e) {
			throw new CommandFailedException(e);
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

	@Override
	public String[] getMountParameters() throws CommandFailedException {
		ArrayList<String> mountOptions = new ArrayList<>(8);
		mountOptions.add(("-oatomic_o_trunc"));
		try {
			mountOptions.add("-ouid=" + getUIdOrGID("uid"));
			mountOptions.add("-ogid=" + getUIdOrGID("gid"));
		} catch (IOException e) {
			e.printStackTrace();
			throw new CommandFailedException(e);
		}
			mountOptions.add("-ovolname=" + vaultSettings.mountName().get());
			mountOptions.add("-oauto_xattr");
		return mountOptions.toArray(new String [mountOptions.size()]);
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
	public Path getFsRootPath() {
		return root;
	}

	@Override
	public void revealFsRootInFilesystemManager() throws CommandFailedException {
		throw new CommandFailedException("Not implemented.");
	}


	@Override
	public void cleanUp() {
		try {
			Files.deleteIfExists(Paths.get(vaultSettings.mountPath().get() + vaultSettings.mountName().get()));
		} catch (IOException e) {
			//LOG.warn("Could not delete mount directory of vault " + vaultSettings.mountName().get());
			e.printStackTrace();
		}
	}

	@Override
	public boolean supportsFuse() {
		return false;
	}
}

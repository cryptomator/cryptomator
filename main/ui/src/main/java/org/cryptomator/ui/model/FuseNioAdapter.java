package org.cryptomator.ui.model;

import org.cryptomator.cryptofs.CryptoFileSystem;
import org.cryptomator.frontend.fuse.AdapterFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@VaultModule.PerVault
public class FuseNioAdapter implements NioAdapter {

	private static final Logger LOG = LoggerFactory.getLogger(FuseNioAdapter.class);
	private final FuseEnvironment fuseEnv;

	private org.cryptomator.frontend.fuse.FuseNioAdapter ffs;
	private CryptoFileSystem cfs;

	@Inject
	public FuseNioAdapter(FuseEnvironment fuseEnv) {
		this.fuseEnv = fuseEnv;
	}

	@Override
	public void prepare(CryptoFileSystem fs) {
		this.cfs = fs;
		ffs = AdapterFactory.createReadWriteAdapter(fs.getPath("/"));
	}

	@Override
	public void mount() throws CommandFailedException {
		try {
			fuseEnv.prepare();
			ffs.mount(fuseEnv.getFsRootPath(), false, false, fuseEnv.getMountParameters());
		} catch (Exception e) {
			throw new CommandFailedException("Unable to mount Filesystem", e);
		}
	}

	@Override
	public void reveal() throws CommandFailedException {
		fuseEnv.revealFsRootInFilesystemManager();
	}

	@Override
	public synchronized void unmount() throws CommandFailedException {
		if (cfs.getStats().pollBytesRead() == 0 && cfs.getStats().pollBytesWritten() == 0) {
			unmountRaw();
		} else {
			throw new CommandFailedException("Pending read or write operations.");
		}
	}

	@Override
	public synchronized void unmountForced() throws CommandFailedException {
		this.unmountRaw();
	}

	private synchronized void unmountRaw() {
		ffs.umount();
	}

	@Override
	public void stop() {
		fuseEnv.cleanUp();
	}

	@Override
	public String getMountUrl() {
		return fuseEnv.getFsRootPath().toUri().toString();
	}

	@Override
	public boolean isSupported() {
		return fuseEnv.supportsFuse();
	}

	@Override
	public boolean supportsForcedUnmount() {
		return true;
	}

}

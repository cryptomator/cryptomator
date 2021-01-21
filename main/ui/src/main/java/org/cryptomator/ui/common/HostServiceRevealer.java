package org.cryptomator.ui.common;

import org.cryptomator.common.vaults.Volume;
import org.cryptomator.ui.launcher.FxApplicationStarter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.file.Path;

@Singleton
public class HostServiceRevealer implements Volume.Revealer {

	private final FxApplicationStarter fxApplicationStarter;

	@Inject
	public HostServiceRevealer(FxApplicationStarter fxApplicationStarter) {
		this.fxApplicationStarter = fxApplicationStarter;
	}

	@Override
	public void reveal(Path p) throws Volume.VolumeException {
		fxApplicationStarter.get().thenAccept(app -> app.getHostServices().showDocument(p.toUri().toString()));
	}
}

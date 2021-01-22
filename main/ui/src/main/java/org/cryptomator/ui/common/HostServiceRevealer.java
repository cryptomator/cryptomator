package org.cryptomator.ui.common;

import dagger.Lazy;
import org.cryptomator.common.vaults.Volume;
import org.cryptomator.ui.fxapp.FxApplicationScoped;

import javax.inject.Inject;
import javafx.application.Application;
import java.nio.file.Path;

@FxApplicationScoped
public class HostServiceRevealer implements Volume.Revealer {

	private final Lazy<Application> application;

	@Inject
	public HostServiceRevealer(Lazy<Application> application) {
		this.application = application;
	}

	@Override
	public void reveal(Path p) throws Volume.VolumeException {
		application.get().getHostServices().showDocument(p.toUri().toString());
	}
}

package org.cryptomator.ui.common;

import dagger.Lazy;
import org.cryptomator.ui.fxapp.FxApplicationScoped;

import javax.inject.Inject;
import javafx.application.Application;
import java.nio.file.Path;

@FxApplicationScoped
public class HostServiceRevealer {

	private final Lazy<Application> application;

	@Inject
	public HostServiceRevealer(Lazy<Application> application) {
		this.application = application;
	}

	public void reveal(Path p) {
		application.get().getHostServices().showDocument(p.toUri().toString());
	}
}

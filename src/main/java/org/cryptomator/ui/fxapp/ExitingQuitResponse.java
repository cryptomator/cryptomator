package org.cryptomator.ui.fxapp;

import javafx.application.Platform;
import java.awt.desktop.QuitResponse;

record ExitingQuitResponse(QuitResponse delegate) implements QuitResponse {

	@Override
	public void performQuit() {
		Platform.exit();
		// TODO wait a moment for javafx to terminate?
		delegate.performQuit();
	}

	@Override
	public void cancelQuit() {
		delegate.cancelQuit();
	}

}

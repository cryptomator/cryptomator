package org.cryptomator.ui;

import java.util.concurrent.ExecutorService;

import javax.inject.Singleton;

import org.cryptomator.ui.controllers.MainController;
import org.cryptomator.ui.util.DeferredCloser;

import dagger.Component;

@Singleton
@Component(modules = CryptomatorModule.class)
interface CryptomatorComponent {
	ExecutorService executorService();

	DeferredCloser deferredCloser();

	MainController mainController();
}
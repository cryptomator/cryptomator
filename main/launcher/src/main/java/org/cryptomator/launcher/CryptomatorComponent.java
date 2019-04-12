package org.cryptomator.launcher;

import dagger.Component;
import org.cryptomator.common.CommonsModule;
import org.cryptomator.logging.LoggerModule;

import javax.inject.Singleton;

@Singleton
@Component(modules = {CryptomatorModule.class, CommonsModule.class, LoggerModule.class})
public interface CryptomatorComponent {

	Cryptomator application();

	FxApplicationComponent.Builder fxApplicationComponent();

}

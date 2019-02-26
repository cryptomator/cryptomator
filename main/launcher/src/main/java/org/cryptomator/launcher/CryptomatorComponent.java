package org.cryptomator.launcher;

import dagger.Component;
import org.cryptomator.common.CommonsModule;
import org.cryptomator.common.Environment;
import org.cryptomator.logging.DebugMode;
import org.cryptomator.logging.LoggerModule;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Optional;

@Singleton
@Component(modules = {CryptomatorModule.class, CommonsModule.class, LoggerModule.class})
public interface CryptomatorComponent {

	Cryptomator application();

	FxApplicationComponent fxApplicationComponent();

}

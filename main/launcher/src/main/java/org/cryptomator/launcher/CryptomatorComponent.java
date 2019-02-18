package org.cryptomator.launcher;

import dagger.Component;
import org.cryptomator.common.CommonsModule;
import org.cryptomator.common.Environment;

import javax.inject.Named;
import javax.inject.Singleton;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;

@Singleton
@Component(modules = {CryptomatorModule.class, CommonsModule.class})
public interface CryptomatorComponent {

	Environment environment();

	FileOpenRequestHandler fileOpenRequestHanlder();

	@Named("applicationVersion")
	Optional<String> applicationVersion();

	FxApplicationComponent.Builder fxApplicationComponent();

}

package org.cryptomator.ui.error;

import dagger.Module;
import dagger.Provides;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlLoaderFactory;

import javax.inject.Named;
import javax.inject.Provider;
import javafx.scene.Scene;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.ResourceBundle;

@Module
abstract class ErrorModule {

	@Provides
	static FxmlLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FxmlLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@Named("stackTrace")
	static String provideStackTrace(@ErrorReport Throwable cause) {
		// TODO deduplicate VaultDetailUnknownErrorController.java
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		cause.printStackTrace(new PrintStream(baos));
		return baos.toString(StandardCharsets.UTF_8);
	}

	@Provides
	@Named("errorScene")
	static Scene provideErrorScene(FxmlLoaderFactory fxmlLoaders, @ErrorReport FxmlFile file) {
		return fxmlLoaders.createScene(file);
	}
}

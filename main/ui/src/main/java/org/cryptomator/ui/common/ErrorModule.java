package org.cryptomator.ui.common;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;

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
	static String provideStackTrace(Throwable cause) {
		// TODO deduplicate VaultDetailUnknownErrorController.java
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		cause.printStackTrace(new PrintStream(baos));
		return baos.toString(StandardCharsets.UTF_8);
	}

	@Binds
	@IntoMap
	@FxControllerKey(ErrorController.class)
	abstract FxController bindErrorController(ErrorController controller);

	@Provides
	@FxmlScene(FxmlFile.ERROR)
	static Scene provideErrorScene(FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.ERROR);
	}


}

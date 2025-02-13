package org.cryptomator.ui.eventviewer;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlLoaderFactory;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.StageFactory;

import javax.inject.Named;
import javax.inject.Provider;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.Map;
import java.util.ResourceBundle;

@Module
abstract class EventViewerModule {

	@Provides
	@EventViewerScoped
	@EventViewerWindow
	static Stage provideStage(StageFactory factory, ResourceBundle resourceBundle, @Named("owner") Stage owner) {
		Stage stage = factory.create();
		stage.setTitle("TODO EVENTVIEWER");
		stage.setResizable(true);
		stage.initModality(Modality.NONE);
		stage.initOwner(owner);
		return stage;
	}

	@Provides
	@EventViewerScoped
	@EventViewerWindow
	static FxmlLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FxmlLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@FxmlScene(FxmlFile.EVENT_VIEWER)
	@EventViewerScoped
	static Scene provideEventViewerScene(@EventViewerWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.EVENT_VIEWER);
	}


	@Binds
	@IntoMap
	@FxControllerKey(EventViewController.class)
	abstract FxController bindEventViewController(EventViewController controller);

	@Binds
	@IntoMap
	@FxControllerKey(UpdateEventViewController.class)
	abstract FxController bindUpdateEventViewController(UpdateEventViewController controller);
}

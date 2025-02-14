package org.cryptomator.ui.eventview;

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
abstract class EventViewModule {

	@Provides
	@EventViewScoped
	@EventViewWindow
	static Stage provideStage(StageFactory factory, ResourceBundle resourceBundle, @Named("owner") Stage owner) {
		Stage stage = factory.create();
		stage.setTitle("TODO EVENTVIEWER");
		stage.setResizable(true);
		stage.initModality(Modality.NONE);
		stage.initOwner(owner);
		return stage;
	}

	@Provides
	@EventViewScoped
	@EventViewWindow
	static FxmlLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FxmlLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@FxmlScene(FxmlFile.EVENT_VIEW)
	@EventViewScoped
	static Scene provideEventViewerScene(@EventViewWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.EVENT_VIEW);
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

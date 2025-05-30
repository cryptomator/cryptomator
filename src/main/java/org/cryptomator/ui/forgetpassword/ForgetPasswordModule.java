package org.cryptomator.ui.forgetpassword;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FxmlLoaderFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.StageFactory;

import javax.inject.Named;
import javax.inject.Provider;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.Map;
import java.util.ResourceBundle;

@Module
abstract class ForgetPasswordModule {

	@Provides
	@ForgetPasswordWindow
	@ForgetPasswordScoped
	static FxmlLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FxmlLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@ForgetPasswordWindow
	@ForgetPasswordScoped
	static Stage provideStage(StageFactory factory, ResourceBundle resourceBundle, @Named("forgetPasswordOwner") Stage owner) {
		Stage stage = factory.create();
		stage.setTitle(resourceBundle.getString("forgetPassword.title"));
		stage.setResizable(false);
		stage.initModality(Modality.WINDOW_MODAL);
		stage.initOwner(owner);
		return stage;
	}

	@Provides
	@ForgetPasswordWindow
	@ForgetPasswordScoped
	static BooleanProperty provideConfirmedProperty() {
		return new SimpleBooleanProperty(false);
	}

	@Binds
	@ForgetPasswordWindow
	@ForgetPasswordScoped
	abstract ReadOnlyBooleanProperty bindReadOnlyConfirmedProperty(@ForgetPasswordWindow BooleanProperty confirmedProperty);

	// ------------------

	@Provides
	@FxmlScene(FxmlFile.FORGET_PASSWORD)
	@ForgetPasswordScoped
	static Scene provideForgetPasswordScene(@ForgetPasswordWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.FORGET_PASSWORD);
	}

	// ------------------

	@Binds
	@IntoMap
	@FxControllerKey(ForgetPasswordController.class)
	abstract FxController bindForgetPasswordController(ForgetPasswordController controller);
}

package org.cryptomator.ui.forgetPassword;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Named;
import javax.inject.Provider;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

@Module
abstract class ForgetPasswordModule {

	@Provides
	@ForgetPasswordWindow
	@ForgetPasswordScoped
	static FXMLLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FXMLLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@ForgetPasswordWindow
	@ForgetPasswordScoped
	static Stage provideStage(ResourceBundle resourceBundle, @Named("windowIcons") List<Image> windowIcons, @Named("forgetPasswordOwner") Stage owner) {
		Stage stage = new Stage();
		stage.setTitle(resourceBundle.getString("forgetPassword.title"));
		stage.setResizable(false);
		stage.initModality(Modality.WINDOW_MODAL);
		stage.initOwner(owner);
		stage.getIcons().addAll(windowIcons);
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
	static Scene provideForgetPasswordScene(@ForgetPasswordWindow FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene("/fxml/forget_password.fxml");
	}

	// ------------------

	@Binds
	@IntoMap
	@FxControllerKey(ForgetPasswordController.class)
	abstract FxController bindForgetPasswordController(ForgetPasswordController controller);
}

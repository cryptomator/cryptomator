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
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Named;
import javax.inject.Provider;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

@Module
abstract class ForgetPasswordModule {

	@Provides
	@ForgetPasswordWindow
	@ForgetPasswordScoped
	static FXMLLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, ResourceBundle resourceBundle) {
		return new FXMLLoaderFactory(factories, resourceBundle);
	}

	@Provides
	@ForgetPasswordWindow
	@ForgetPasswordScoped
	static Stage provideStage(ResourceBundle resourceBundle, @Named("windowIcon") Optional<Image> windowIcon, @Named("forgetPasswordOwner") Stage owner) {
		Stage stage = new Stage();
		stage.setTitle(resourceBundle.getString("forgetPassword.title"));
		stage.setResizable(false);
		stage.initModality(Modality.WINDOW_MODAL);
		stage.initOwner(owner);
		windowIcon.ifPresent(stage.getIcons()::add);
		return stage;
	}

	@Provides
	@FxmlScene(FxmlFile.FORGET_PASSWORD)
	@ForgetPasswordScoped
	static Scene provideForgetPasswordScene(@ForgetPasswordWindow FXMLLoaderFactory fxmlLoaders, @ForgetPasswordWindow Stage window) {
		return fxmlLoaders.createScene("/fxml/forget_password.fxml");
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

	@Binds
	@IntoMap
	@FxControllerKey(ForgetPasswordController.class)
	abstract FxController bindForgetPasswordController(ForgetPasswordController controller);
}

package org.cryptomator.ui.changepassword;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import org.cryptomator.ui.common.NewPasswordController;
import org.cryptomator.ui.common.PasswordStrengthUtil;
import org.cryptomator.ui.common.StageFactory;

import javax.inject.Named;
import javax.inject.Provider;
import java.nio.CharBuffer;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

@Module
abstract class ChangePasswordModule {
	
	@Provides
	@ChangePasswordScoped
	@Named("newPassword")
	static ObjectProperty<CharSequence> provideNewPasswordProperty() {
		return new SimpleObjectProperty<>("");
	}

	@Provides
	@ChangePasswordWindow
	@ChangePasswordScoped
	static FXMLLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FXMLLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@ChangePasswordWindow
	@ChangePasswordScoped
	static Stage provideStage(StageFactory factory, @Named("changePasswordOwner") Stage owner, ResourceBundle resourceBundle) {
		Stage stage = factory.create();
		stage.setTitle(resourceBundle.getString("changepassword.title"));
		stage.setResizable(false);
		stage.initModality(Modality.WINDOW_MODAL);
		stage.initOwner(owner);
		return stage;
	}

	@Provides
	@FxmlScene(FxmlFile.CHANGEPASSWORD)
	@ChangePasswordScoped
	static Scene provideUnlockScene(@ChangePasswordWindow FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene("/fxml/changepassword.fxml");
	}


	// ------------------

	@Binds
	@IntoMap
	@FxControllerKey(ChangePasswordController.class)
	abstract FxController bindUnlockController(ChangePasswordController controller);
	
	@Provides
	@IntoMap
	@FxControllerKey(NewPasswordController.class)
	static FxController provideNewPasswordController(ResourceBundle resourceBundle, PasswordStrengthUtil strengthRater, @Named("newPassword") ObjectProperty<CharSequence> password) {
		return new NewPasswordController(resourceBundle, strengthRater, password);
	}

}

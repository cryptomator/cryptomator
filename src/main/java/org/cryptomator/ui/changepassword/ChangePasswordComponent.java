package org.cryptomator.ui.changepassword;

import dagger.BindsInstance;
import dagger.Lazy;
import dagger.Subcomponent;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Named;
import javafx.scene.Scene;
import javafx.stage.Stage;

@ChangePasswordScoped
@Subcomponent(modules = {ChangePasswordModule.class})
public interface ChangePasswordComponent {

	@ChangePasswordWindow
	Stage window();

	@FxmlScene(FxmlFile.CHANGEPASSWORD)
	Lazy<Scene> scene();

	default void showChangePasswordWindow() {
		Stage stage = window();
		stage.setScene(scene().get());
		stage.show();
	}

	@Subcomponent.Builder
	interface Builder {

		@BindsInstance
		Builder vault(@ChangePasswordWindow Vault vault);

		@BindsInstance
		Builder owner(@Named("changePasswordOwner") Stage owner);

		ChangePasswordComponent build();
	}

}

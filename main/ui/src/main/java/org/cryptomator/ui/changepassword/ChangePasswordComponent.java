package org.cryptomator.ui.changepassword;

import dagger.BindsInstance;
import dagger.Lazy;
import dagger.Subcomponent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

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

		ChangePasswordComponent build();
	}

}

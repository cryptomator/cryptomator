package org.cryptomator.ui.health;

import dagger.BindsInstance;
import dagger.Lazy;
import dagger.Subcomponent;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Named;
import javafx.scene.Scene;
import javafx.stage.Stage;

@HealthCheckScoped
@Subcomponent(modules = {HealthCheckModule.class})
public interface HealthCheckComponent {

	@HealthCheckWindow
	Stage window();

	@Named("windowToClose")
	Stage windowToClose();

	@FxmlScene(FxmlFile.HEALTH_START)
	Lazy<Scene> scene();

	default Stage showHealthCheckWindow() {
		Stage stage = window();
		stage.setScene(scene().get());
		stage.show();
		windowToClose().close();
		return stage;
	}

	@Subcomponent.Builder
	interface Builder {

		@BindsInstance
		Builder vault(@HealthCheckWindow Vault vault);

		@BindsInstance
		Builder windowToClose(@Named("windowToClose") Stage window);

		HealthCheckComponent build();
	}

}

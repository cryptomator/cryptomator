package org.cryptomator.ui.health;

import dagger.BindsInstance;
import dagger.Lazy;
import dagger.Subcomponent;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Named;
import javafx.scene.Scene;
import javafx.stage.Stage;

@HealthCheckScoped
@Subcomponent(modules = {HealthCheckModule.class})
public interface HealthCheckComponent {

	LoadUnverifiedConfigResult loadConfig();

	@HealthCheckWindow
	Stage window();

	@FxmlScene(FxmlFile.HEALTH_START)
	Lazy<Scene> startScene();

	@FxmlScene(FxmlFile.HEALTH_START_FAIL)
	Lazy<Scene> failScene();

	default Stage showHealthCheckWindow() {
		Stage stage = window();
		// TODO reevaluate config loading, as soon as we have the new generic error screen
		var unverifiedConf = loadConfig();
		if (unverifiedConf.config() != null) {
			stage.setScene(startScene().get());
		} else {
			stage.setScene(failScene().get());
		}
		stage.show();
		return stage;
	}

	@Subcomponent.Builder
	interface Builder {

		@BindsInstance
		Builder vault(@HealthCheckWindow Vault vault);

		@BindsInstance
		Builder owner(@Named("healthCheckOwner") Stage owner);

		HealthCheckComponent build();
	}

	record LoadUnverifiedConfigResult(VaultConfig.UnverifiedVaultConfig config, Throwable error) {}
}

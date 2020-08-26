package org.cryptomator.ui.stats;

import dagger.BindsInstance;
import dagger.Lazy;
import dagger.Subcomponent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

@VaultStatisticsScoped
@Subcomponent(modules = {VaultStatisticsModule.class})
public interface VaultStatisticsComponent {

	@VaultStatisticsWindow
	Stage window();

	@FxmlScene(FxmlFile.VAULT_STATISTICS)
	Lazy<Scene> scene();

	default void showVaultStatisticsWindow() {
		Stage stage = window();
		stage.setScene(scene().get());
		stage.sizeToScene();
		stage.show();
		stage.requestFocus();
	}

	@Subcomponent.Builder
	interface Builder {

		@BindsInstance
		Builder vault(@VaultStatisticsWindow Vault vault);

		VaultStatisticsComponent build();
	}

}

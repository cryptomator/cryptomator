package org.cryptomator.ui.stats;

import dagger.BindsInstance;
import dagger.Lazy;
import dagger.Subcomponent;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * For each vault there can be up to one statistics component.
 * <p>
 * <b>Important:</b> Outside of {@link org.cryptomator.ui.stats}, this component should be weakly referenced,
 * as it include memory-intensive UI nodes.
 * <p>
 * While the stats window is visible, this component is strongly referenced by the window's main controller.
 * As soon as the window is closed, the full objectgraph becomes eligible for GC.
 */
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

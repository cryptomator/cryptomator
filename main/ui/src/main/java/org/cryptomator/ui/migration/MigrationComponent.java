package org.cryptomator.ui.migration;

import dagger.BindsInstance;
import dagger.Lazy;
import dagger.Subcomponent;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javafx.scene.Scene;
import javafx.stage.Stage;

@MigrationScoped
@Subcomponent(modules = {MigrationModule.class})
public interface MigrationComponent {

	@MigrationWindow
	Stage window();

	@FxmlScene(FxmlFile.MIGRATION_START)
	Lazy<Scene> scene();

	default Stage showMigrationWindow() {
		Stage stage = window();
		stage.setScene(scene().get());
		stage.show();
		return stage;
	}

	@Subcomponent.Builder
	interface Builder {

		@BindsInstance
		Builder vault(@MigrationWindow Vault vault);

		MigrationComponent build();
	}

}

package org.cryptomator.ui.importtemplate;

import dagger.BindsInstance;
import dagger.Lazy;
import dagger.Subcomponent;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Named;
import javafx.scene.Scene;
import javafx.stage.Stage;

@ImportTemplateScoped
@Subcomponent(modules = {ImportTemplateModule.class})
public interface ImportTemplateComponent {

	@ImportTemplateWindow
	Stage window();

	@FxmlScene(FxmlFile.IMPORT_TEMPLATE_LOCATION)
	Lazy<Scene> scene();

	default void showImportTemplateWindow() {
		Stage stage = window();
		stage.setScene(scene().get());
		stage.show();
	}

	@Subcomponent.Factory
	interface Factory {
		ImportTemplateComponent create(@BindsInstance @Named("vaultName") String name, @BindsInstance VaultTemplate template);
	}

}

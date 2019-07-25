package org.cryptomator.ui.addvaultwizard;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.mainwindow.MainWindow;

import javax.inject.Provider;
import java.nio.file.Path;
import java.util.Map;
import java.util.ResourceBundle;

@Module
public abstract class AddVaultModule {

	@Provides
	@AddVaultWizard
	@AddVaultWizardScoped
	static FXMLLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, ResourceBundle resourceBundle) {
		return new FXMLLoaderFactory(factories, resourceBundle);
	}

	@Provides
	@AddVaultWizard
	@AddVaultWizardScoped
	static Stage provideStage(@MainWindow Stage owner) {
		Stage stage = new Stage();
		stage.setMinWidth(500);
		stage.setMinHeight(500);
		stage.initStyle(StageStyle.DECORATED);
		stage.initModality(Modality.WINDOW_MODAL);
		stage.initOwner(owner);
		return stage;
	}

	@Provides
	@AddVaultWizardScoped
	static ObjectProperty<Path> provideVaultPath() {
		return new SimpleObjectProperty<>();
	}

	// ------------------

	@Binds
	@IntoMap
	@FxControllerKey(AddVaultWelcomeController.class)
	abstract FxController bindWelcomeController(AddVaultWelcomeController controller);

	@Binds
	@IntoMap
	@FxControllerKey(ChooseExistingVaultController.class)
	abstract FxController bindChooseExistingVaultController(ChooseExistingVaultController controller);


}

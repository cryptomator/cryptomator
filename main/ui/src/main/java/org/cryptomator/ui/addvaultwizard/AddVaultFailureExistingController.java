package org.cryptomator.ui.addvaultwizard;

import dagger.Lazy;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Inject;
import java.nio.file.Path;

@AddVaultWizardScoped
public class AddVaultFailureExistingController implements FxController {

	private final Stage window;
	private final Lazy<Scene> previousScene;
	private final StringBinding vaultName;

	@Inject
	AddVaultFailureExistingController(@AddVaultWizardWindow Stage window, @FxmlScene(FxmlFile.ADDVAULT_EXISTING) Lazy<Scene> previousScene, ObjectProperty<Path> pathOfFailedVault){
		this.window = window;
		this.previousScene = previousScene;
		this.vaultName = Bindings.createStringBinding(() -> pathOfFailedVault.get().getFileName().toString(),pathOfFailedVault);
	}

	@FXML
	public void close(){
		window.close();
	}

	@FXML
	public void back(){
		window.setScene(previousScene.get());
	}

	// Getter & Setter

	public StringBinding vaultNameProperty(){
		return vaultName;
	}

	public String getVaultName(){
		return vaultName.get();
	}

}

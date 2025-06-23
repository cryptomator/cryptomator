package org.cryptomator.ui.decryptname;

import dagger.BindsInstance;
import dagger.Lazy;
import dagger.Subcomponent;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.nio.file.Path;
import java.util.List;

@DecryptNameScoped
@Subcomponent(modules = DecryptNameModule.class)
public interface DecryptNameComponent {

	Logger LOG = LoggerFactory.getLogger(DecryptNameComponent.class);

	@DecryptNameWindow
	Stage window();

	@FxmlScene(FxmlFile.DECRYPTNAMES)
	Lazy<Scene> decryptNamesView();

	@DecryptNameWindow
	Vault vault();

	default void showDecryptFileNameWindow() {
		Stage s = window();
		s.setScene(decryptNamesView().get());
		s.sizeToScene();
		if (vault().isUnlocked()) {
			s.show();
		} else {
			LOG.error("Aborted showing DecryptFileName window: vault state is not {}, but {}.", VaultState.Value.UNLOCKED, vault().getState());
		}
	}

	@Subcomponent.Factory
	interface Factory {

		DecryptNameComponent create(@BindsInstance @DecryptNameWindow Vault vault, @BindsInstance @Named("windowOwner") Stage owner, @BindsInstance @DecryptNameWindow List<Path> pathsToDecrypt);
	}
}

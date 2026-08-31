package org.cryptomator.ui.keyloading;

import dagger.BindsInstance;
import dagger.Subcomponent;
import org.cryptomator.common.Nullable;
import org.cryptomator.common.vaults.Vault;

import javafx.stage.Stage;

@KeyLoadingScoped
@Subcomponent(modules = {KeyLoadingModule.class})
public interface KeyLoadingComponent {

	@KeyLoading
	KeyLoadingStrategy keyloadingStrategy();

	@Subcomponent.Factory
	interface Factory {

		/**
		 * @param vaultRef the {@link KeyLoadingRef} containing the info to load the key
		 * @param vault    the local vault, or {@code null} if it is not set up on this machine.
		 * @param window   the window to show the key loading scenes in
		 */
		KeyLoadingComponent create(@BindsInstance @KeyLoading KeyLoadingRef vaultRef, //
								   @BindsInstance @KeyLoading @Nullable Vault vault, //
								   @BindsInstance @KeyLoading Stage window);
	}

}

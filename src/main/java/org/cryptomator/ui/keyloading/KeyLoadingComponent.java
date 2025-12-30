package org.cryptomator.ui.keyloading;

import dagger.BindsInstance;
import dagger.Subcomponent;
import org.cryptomator.common.vaults.Vault;

import javafx.stage.Stage;

@KeyLoadingScoped
@Subcomponent(modules = {KeyLoadingModule.class})
public interface KeyLoadingComponent {

	@KeyLoading
	KeyLoadingStrategy keyloadingStrategy();

	@Subcomponent.Factory
	interface Factory {

		KeyLoadingComponent create(@BindsInstance @KeyLoading Vault vault, @KeyLoading @BindsInstance Stage window);
	}

}

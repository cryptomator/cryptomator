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

	@Subcomponent.Builder
	interface Builder {

		@BindsInstance
		Builder vault(@KeyLoading Vault vault);

		@BindsInstance
		Builder window(@KeyLoading Stage window);

		KeyLoadingComponent build();
	}

}

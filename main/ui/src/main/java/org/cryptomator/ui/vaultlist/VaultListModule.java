package org.cryptomator.ui.vaultlist;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;
import org.cryptomator.ui.FxController;
import org.cryptomator.ui.FxControllerKey;

@Module
public abstract class VaultListModule {

	@Binds
	@IntoMap
	@FxControllerKey(VaultListController.class)
	abstract FxController bindController(VaultListController controller);

}

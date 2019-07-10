package org.cryptomator.ui.mainwindow;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;
import org.cryptomator.ui.FxController;
import org.cryptomator.ui.FxControllerKey;
import org.cryptomator.ui.vaultlist.VaultListController;

@Module
public abstract class MainWindowModule {

	@Binds
	@IntoMap
	@FxControllerKey(MainWindowController.class)
	abstract FxController bindController(MainWindowController controller);

}

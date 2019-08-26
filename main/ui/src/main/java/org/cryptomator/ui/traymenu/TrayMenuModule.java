package org.cryptomator.ui.traymenu;

import dagger.Module;
import org.cryptomator.common.JniModule;
import org.cryptomator.ui.fxapp.FxApplicationComponent;

@Module(includes = {JniModule.class}, subcomponents = {FxApplicationComponent.class})
abstract class TrayMenuModule {

}

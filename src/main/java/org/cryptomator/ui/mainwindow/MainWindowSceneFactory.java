package org.cryptomator.ui.mainwindow;

import dagger.Lazy;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.ui.common.DefaultSceneFactory;

import javax.inject.Inject;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;

@MainWindowScoped
public class MainWindowSceneFactory extends DefaultSceneFactory {

	protected static final KeyCodeCombination SHORTCUT_N = new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN);
	protected static final KeyCodeCombination SHORTCUT_O = new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN);

	private final Lazy<MainWindowTitleController> mainWindowTitleController;
	private final Lazy<VaultListController> vaultListController;

	@Inject
	public MainWindowSceneFactory(Settings settings, Lazy<MainWindowTitleController> mainWindowTitleController, Lazy<VaultListController> vaultListController) {
		super(settings);
		this.mainWindowTitleController = mainWindowTitleController;
		this.vaultListController = vaultListController;
	}

	@Override
	protected void setupDefaultAccelerators(Scene scene, Stage stage) {
		if (SystemUtils.IS_OS_WINDOWS) {
			scene.getAccelerators().put(ALT_F4, mainWindowTitleController.get()::close);
		} else {
			scene.getAccelerators().put(SHORTCUT_W, mainWindowTitleController.get()::close);
		}
		scene.getAccelerators().put(SHORTCUT_N, vaultListController.get()::didClickAddNewVault);
		scene.getAccelerators().put(SHORTCUT_O, vaultListController.get()::didClickAddExistingVault);
	}
}

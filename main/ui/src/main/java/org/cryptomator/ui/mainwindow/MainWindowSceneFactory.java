package org.cryptomator.ui.mainwindow;

import dagger.Lazy;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.ui.common.DefaultSceneFactory;

import javax.inject.Inject;

@MainWindowScoped
public class MainWindowSceneFactory extends DefaultSceneFactory {

	protected static final KeyCodeCombination SHORTCUT_N = new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN);

	private final Lazy<MainWindowController> mainWindowController;
	private final Lazy<VaultListController> vaultListController;

	@Inject
	public MainWindowSceneFactory(Settings settings, Lazy<MainWindowController> mainWindowController, Lazy<VaultListController> vaultListController) {
		super(settings);
		this.mainWindowController = mainWindowController;
		this.vaultListController = vaultListController;
	}

	@Override
	protected void setupDefaultAccelerators(Scene scene, Stage stage) {
		if (SystemUtils.IS_OS_WINDOWS) {
			scene.getAccelerators().put(ALT_F4, mainWindowController.get()::close);
		} else {
			scene.getAccelerators().put(SHORTCUT_W, mainWindowController.get()::close);
		}
		scene.getAccelerators().put(SHORTCUT_N, vaultListController.get()::didClickAddVault);
	}
}

package org.cryptomator.ui.mainwindow;

import javafx.beans.binding.Binding;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.common.vaults.Volume;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.Tasks;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.cryptomator.ui.fxapp.FxApplication;
import org.cryptomator.ui.vaultoptions.VaultOptionsComponent;
import org.fxmisc.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;

@MainWindowScoped
public class VaultDetailController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(VaultDetailController.class);

	private final ReadOnlyObjectProperty<Vault> vault;
	private final Binding<FontAwesome5Icon> glyph;
	private final BooleanBinding anyVaultSelected;
	private final ExecutorService executor;
	private final FxApplication application;
	private final VaultOptionsComponent.Builder vaultOptionsWindow;

	@Inject
	VaultDetailController(ObjectProperty<Vault> vault, ExecutorService executor, FxApplication application, VaultOptionsComponent.Builder vaultOptionsWindow) {
		this.vault = vault;
		this.glyph = EasyBind.select(vault).selectObject(Vault::stateProperty).map(this::getGlyphForVaultState).orElse(FontAwesome5Icon.EXCLAMATION_TRIANGLE);
		this.executor = executor;
		this.application = application;
		this.vaultOptionsWindow = vaultOptionsWindow;
		this.anyVaultSelected = vault.isNotNull();
	}

	private FontAwesome5Icon getGlyphForVaultState(VaultState state) {
		switch (state) {
			case LOCKED:
				return FontAwesome5Icon.LOCK_ALT;
			case PROCESSING:
				return FontAwesome5Icon.SPINNER;
			case UNLOCKED:
				return FontAwesome5Icon.LOCK_OPEN_ALT;
			default:
				return FontAwesome5Icon.EXCLAMATION_TRIANGLE;
		}
	}

	@FXML
	public void unlock() {
		application.showUnlockWindow(vault.get());
	}

	@FXML
	public void lock() {
		Vault v = vault.get();
		v.setState(VaultState.PROCESSING);
		Tasks.create(() -> {
			v.lock(false);
		}).onSuccess(() -> {
			LOG.trace("Regular unmount succeeded.");
			v.setState(VaultState.LOCKED);
		}).onError(Exception.class, e -> {
			v.setState(VaultState.UNLOCKED);
			LOG.error("Regular unmount failed.", e);
			// TODO
		}).runOnce(executor);
	}

	@FXML
	public void showVaultOptions() {
		vaultOptionsWindow.vault(vault.get()).build().showVaultOptionsWindow();
	}

	@FXML
	public void revealStorageLocation(ActionEvent actionEvent) {
		application.getHostServices().showDocument(vault.get().getPath().toUri().toString());
	}

	@FXML
	public void revealAccessLocation(ActionEvent actionEvent) {
		try {
			vault.get().reveal();
		} catch (Volume.VolumeException e) {
			LOG.error("Failed to reveal vault.", e);
		}
	}

	/* Observable Properties */

	public ReadOnlyObjectProperty<Vault> vaultProperty() {
		return vault;
	}

	public Vault getVault() {
		return vault.get();
	}

	public Binding<FontAwesome5Icon> glyphProperty() {
		return glyph;
	}

	public FontAwesome5Icon getGlyph() {
		return glyph.getValue();
	}

	public BooleanBinding anyVaultSelectedProperty() {
		return anyVaultSelected;
	}

	public boolean isAnyVaultSelected() {
		return anyVaultSelected.get();
	}
}

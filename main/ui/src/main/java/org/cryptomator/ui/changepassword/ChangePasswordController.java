package org.cryptomator.ui.changepassword;

import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import java.util.ResourceBundle;

@ChangePasswordScoped
public class ChangePasswordController implements FxController {

	private final Stage window;
	private final Vault vault;
	private final ResourceBundle resourceBundle;

	@Inject
	public ChangePasswordController(@ChangePasswordWindow Stage window, @ChangePasswordWindow Vault vault, ResourceBundle resourceBundle) {
		this.window = window;
		this.vault = vault;
		this.resourceBundle = resourceBundle;
	}

}

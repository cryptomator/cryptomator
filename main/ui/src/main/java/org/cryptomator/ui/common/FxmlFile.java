package org.cryptomator.ui.common;

public enum FxmlFile {
	ADDVAULT_WELCOME("/fxml/addvault_welcome.fxml"), //
	ADDVAULT_EXISTING("/fxml/addvault_existing.fxml"), //
	ADDVAULT_NEW_NAME("/fxml/addvault_new_name.fxml"), //
	ADDVAULT_NEW_LOCATION("/fxml/addvault_new_location.fxml"), //
	ADDVAULT_NEW_PASSWORD("/fxml/addvault_new_password.fxml"), //
	ADDVAULT_SUCCESS("/fxml/addvault_success.fxml"), //
	CHANGEPASSWORD("/fxml/changepassword.fxml"), //
	MAIN_WINDOW("/fxml/main_window.fxml"), //
	PREFERENCES("/fxml/preferences.fxml"), //
	QUIT("/fxml/quit.fxml"), //
	REMOVE_VAULT("/fxml/remove_vault.fxml"), //
	UNLOCK("/fxml/unlock2.fxml"), // TODO rename
	UNLOCK_SUCCESS("/fxml/unlock_success.fxml"), //
	VAULT_OPTIONS("/fxml/vault_options.fxml"), //
	WRONGFILEALERT("/fxml/wrongfilealert.fxml");

	private final String filename;

	FxmlFile(String filename) {
		this.filename = filename;
	}
}

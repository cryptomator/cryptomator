package org.cryptomator.ui.common;

public enum FxmlFile {
	ADDVAULT_WELCOME("/fxml/addvault_welcome.fxml"), //
	ADDVAULT_EXISTING("/fxml/addvault_existing.fxml"), //
	ADDVAULT_EXISTING_ERROR("/fxml/addvault_existing_error.fxml"),
	ADDVAULT_NEW_NAME("/fxml/addvault_new_name.fxml"), //
	ADDVAULT_NEW_LOCATION("/fxml/addvault_new_location.fxml"), //
	ADDVAULT_NEW_PASSWORD("/fxml/addvault_new_password.fxml"), //
	ADDVAULT_NEW_RECOVERYKEY("/fxml/addvault_new_recoverykey.fxml"), //
	ADDVAULT_SUCCESS("/fxml/addvault_success.fxml"), //
	CHANGEPASSWORD("/fxml/changepassword.fxml"), //
	FORGET_PASSWORD("/fxml/forget_password.fxml"), //
	MAIN_WINDOW("/fxml/main_window.fxml"), //
	MIGRATION_GENERIC_ERROR("/fxml/migration_generic_error.fxml"), //
	MIGRATION_RUN("/fxml/migration_run.fxml"), //
	MIGRATION_START("/fxml/migration_start.fxml"), //
	MIGRATION_SUCCESS("/fxml/migration_success.fxml"), //
	PREFERENCES("/fxml/preferences.fxml"), //
	QUIT("/fxml/quit.fxml"), //
	RECOVERYKEY_CREATE("/fxml/recoverykey_create.fxml"), //
	RECOVERYKEY_SUCCESS("/fxml/recoverykey_success.fxml"), //
	RECOVER_VAULT("/fxml/recovervault.fxml"),// TODO
	REMOVE_VAULT("/fxml/remove_vault.fxml"), //
	UNLOCK("/fxml/unlock.fxml"),
	UNLOCK_GENERIC_ERROR("/fxml/unlock_generic_error.fxml"), //
	UNLOCK_INVALID_MOUNT_POINT("/fxml/unlock_invalid_mount_point.fxml"), //
	UNLOCK_SUCCESS("/fxml/unlock_success.fxml"), //
	VAULT_OPTIONS("/fxml/vault_options.fxml"), //
	WRONGFILEALERT("/fxml/wrongfilealert.fxml");

	private final String ressourcePathString;

	FxmlFile(String ressourcePathString) {
		this.ressourcePathString = ressourcePathString;
	}

	public String getRessourcePathString(){
		return ressourcePathString;
	}
}

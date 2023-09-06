package org.cryptomator.ui.common;

public enum FxmlFile {
	ADDVAULT_EXISTING("/fxml/addvault_existing.fxml"), //
	ADDVAULT_NEW_NAME("/fxml/addvault_new_name.fxml"), //
	ADDVAULT_NEW_LOCATION("/fxml/addvault_new_location.fxml"), //
	ADDVAULT_NEW_EXPERT_SETTINGS("/fxml/addvault_new_expert_settings.fxml"), //
	ADDVAULT_NEW_PASSWORD("/fxml/addvault_new_password.fxml"), //
	ADDVAULT_NEW_RECOVERYKEY("/fxml/addvault_new_recoverykey.fxml"), //
	ADDVAULT_SUCCESS("/fxml/addvault_success.fxml"), //
	CHANGEPASSWORD("/fxml/changepassword.fxml"), //
	CONVERTVAULT_HUBTOPASSWORD_START("/fxml/convertvault_hubtopassword_start.fxml"), //
	CONVERTVAULT_HUBTOPASSWORD_CONVERT("/fxml/convertvault_hubtopassword_convert.fxml"), //
	CONVERTVAULT_HUBTOPASSWORD_SUCCESS("/fxml/convertvault_hubtopassword_success.fxml"), //
	ERROR("/fxml/error.fxml"), //
	FORGET_PASSWORD("/fxml/forget_password.fxml"), //
	HEALTH_START("/fxml/health_start.fxml"), //
	HEALTH_CHECK_LIST("/fxml/health_check_list.fxml"), //
	HUB_NO_KEYCHAIN("/fxml/hub_no_keychain.fxml"), //
	HUB_AUTH_FLOW("/fxml/hub_auth_flow.fxml"), //
	HUB_INVALID_LICENSE("/fxml/hub_invalid_license.fxml"), //
	HUB_RECEIVE_KEY("/fxml/hub_receive_key.fxml"), //
	HUB_LEGACY_REGISTER_DEVICE("/fxml/hub_legacy_register_device.fxml"), //
	HUB_REGISTER_SUCCESS("/fxml/hub_register_success.fxml"), //
	HUB_REGISTER_FAILED("/fxml/hub_register_failed.fxml"), //
	HUB_SETUP_DEVICE("/fxml/hub_setup_device.fxml"), //
	HUB_UNAUTHORIZED_DEVICE("/fxml/hub_unauthorized_device.fxml"), //
	LOCK_FORCED("/fxml/lock_forced.fxml"), //
	LOCK_FAILED("/fxml/lock_failed.fxml"), //
	MAIN_WINDOW("/fxml/main_window.fxml"), //
	MIGRATION_CAPABILITY_ERROR("/fxml/migration_capability_error.fxml"), //
	MIGRATION_IMPOSSIBLE("/fxml/migration_impossible.fxml"),
	MIGRATION_RUN("/fxml/migration_run.fxml"), //
	MIGRATION_START("/fxml/migration_start.fxml"), //
	MIGRATION_SUCCESS("/fxml/migration_success.fxml"), //
	PREFERENCES("/fxml/preferences.fxml"), //
	QUIT("/fxml/quit.fxml"), //
	QUIT_FORCED("/fxml/quit_forced.fxml"), //
	RECOVERYKEY_CREATE("/fxml/recoverykey_create.fxml"), //
	RECOVERYKEY_RECOVER("/fxml/recoverykey_recover.fxml"), //
	RECOVERYKEY_RESET_PASSWORD("/fxml/recoverykey_reset_password.fxml"), //
	RECOVERYKEY_RESET_PASSWORD_SUCCESS("/fxml/recoverykey_reset_password_success.fxml"), //
	RECOVERYKEY_SUCCESS("/fxml/recoverykey_success.fxml"), //
	REMOVE_VAULT("/fxml/remove_vault.fxml"), //
	UPDATE_REMINDER("/fxml/update_reminder.fxml"), //
	UNLOCK_ENTER_PASSWORD("/fxml/unlock_enter_password.fxml"),
	UNLOCK_INVALID_MOUNT_POINT("/fxml/unlock_invalid_mount_point.fxml"), //
	UNLOCK_SELECT_MASTERKEYFILE("/fxml/unlock_select_masterkeyfile.fxml"), //
	UNLOCK_SUCCESS("/fxml/unlock_success.fxml"), //
	VAULT_OPTIONS("/fxml/vault_options.fxml"), //
	VAULT_STATISTICS("/fxml/stats.fxml"), //
	WRONGFILEALERT("/fxml/wrongfilealert.fxml");

	private final String ressourcePathString;

	FxmlFile(String ressourcePathString) {
		this.ressourcePathString = ressourcePathString;
	}

	public String getRessourcePathString() {
		return ressourcePathString;
	}
}

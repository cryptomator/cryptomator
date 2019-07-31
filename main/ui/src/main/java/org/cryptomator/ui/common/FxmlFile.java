package org.cryptomator.ui.common;

public enum FxmlFile {
	MAIN_WINDOW("/fxml/main_window.fxml"), //
	ADDVAULT_WELCOME("/fxml/addvault_welcome.fxml"), //
	ADDVAULT_EXISTING("/fxml/addvault_existing.fxml"), //
	PREFERENCES("/fxml/preferences.fxml"), //
	UNLOCK("/fxml/unlock2.fxml"), // TODO rename
	UNLOCK_SUCCESS("/fxml/unlock_success.fxml"),
	;

	private final String filename;

	FxmlFile(String filename) {
		this.filename = filename;
	}
}

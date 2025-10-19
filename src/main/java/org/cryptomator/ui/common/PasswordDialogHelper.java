package org.cryptomator.ui.common;

import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import java.util.Optional;

/**
 * Helper utility for creating password prompt dialogs with masked input.
 * Uses PasswordField instead of TextInputDialog to hide password from screenshots
 * and assistive technology.
 */
public class PasswordDialogHelper {

	/**
	 * Show a dialog that prompts for a password with masked input.
	 * 
	 * @param owner Parent window for the dialog
	 * @param title Dialog title
	 * @param header Header text
	 * @param prompt Label text for the password field
	 * @return Optional containing the entered password, or empty if canceled
	 */
	public static Optional<String> promptPassword(Window owner, String title, String header, String prompt) {
		Dialog<String> dialog = new Dialog<>();
		dialog.initOwner(owner);
		dialog.setTitle(title);
		dialog.setHeaderText(header);
		
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		
		PasswordField passwordField = new PasswordField();
		passwordField.setPromptText(prompt);
		
		grid.add(new Label(prompt), 0, 0);
		grid.add(passwordField, 1, 0);
		
		dialog.getDialogPane().setContent(grid);
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		
		// Request focus on password field when dialog is shown
		Platform.runLater(passwordField::requestFocus);
		
		// Convert button result to password string
		dialog.setResultConverter(button -> 
			button == ButtonType.OK ? passwordField.getText() : null
		);
		
		return dialog.showAndWait();
	}
}

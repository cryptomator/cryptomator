/*******************************************************************************
 * Copyright (c) 2016, 2017 Sebastian Stenzel and others.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Jean-Noël Charon - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui.util;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.text.Text;

public class DialogBuilderUtil {

	public DialogBuilderUtil() {
	}

	public static Alert buildInformationDialog(String title, String header, String content, ButtonType defaultButton) {
		return buildDialog(title, header, content, Alert.AlertType.INFORMATION, defaultButton);
	}

	public static Alert buildWarningDialog(String title, String header, String content, ButtonType defaultButton) {
		return buildDialog(title, header, content, Alert.AlertType.WARNING, defaultButton);
	}

	public static Alert buildErrorDialog(String title, String header, String content, ButtonType defaultButton) {
		return buildDialog(title, header, content, Alert.AlertType.ERROR, defaultButton);
	}

	public static Alert buildConfirmationDialog(String title, String header, String content, ButtonType defaultButton) {
		return buildDialog(title, header, content, Alert.AlertType.CONFIRMATION, defaultButton);
	}

	public static Alert buildYesNoDialog(String title, String header, String content, ButtonType defaultButton) {
		return buildDialog(title, header, content, Alert.AlertType.CONFIRMATION, defaultButton, ButtonType.YES, ButtonType.NO);
	}

	public static Alert buildGracefulShutdownDialog(String title, String header, String content, ButtonType defaultButton, ButtonType... buttons) {
		return buildDialog(title, header, content, Alert.AlertType.WARNING, defaultButton, buttons);
	}

	private static Alert buildDialog(String title, String header, String content, Alert.AlertType type, ButtonType defaultButton, ButtonType... buttons) {
		Text contentText = new Text(content);
		contentText.setWrappingWidth(360.0);

		Alert alert = new Alert(type, content, buttons);
		alert.setTitle(title);
		alert.setHeaderText(header);
		alert.getDialogPane().setContent(contentText);

		alert.getDialogPane().getButtonTypes().stream().forEach(buttonType -> {
			Button btn = (Button) alert.getDialogPane().lookupButton(buttonType);
			btn.setDefaultButton(buttonType.equals(defaultButton));
		});

		return alert;
	}
}

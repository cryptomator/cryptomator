/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Jean-Noël Charon - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui.util;

import javafx.scene.control.Alert;

public class DialogBuilderUtil {

    public DialogBuilderUtil() {}

    public static Alert buildInformationDialog(String title, String header, String content) {
        return buildDialog(title, header, content,Alert.AlertType.INFORMATION);
    }

    public static Alert buildWarningDialog(String title, String header, String content) {
        return buildDialog(title, header, content,Alert.AlertType.WARNING);
    }

    public static Alert buildErrorDialog(String title, String header, String content) {
        return buildDialog(title, header, content,Alert.AlertType.ERROR);
    }

    public static Alert buildConfirmationDialog(String title, String header, String content) {
        return buildDialog(title, header, content,Alert.AlertType.CONFIRMATION);
    }

    private static Alert buildDialog(String title, String header, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        return alert;
    }
}

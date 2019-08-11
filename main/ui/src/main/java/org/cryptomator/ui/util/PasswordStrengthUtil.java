/*******************************************************************************
 * Copyright (c) 2016, 2017 Sebastian Stenzel and others.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Jean-Noël Charon - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui.util;

import com.google.common.base.Strings;
import com.nulabinc.zxcvbn.Zxcvbn;
import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import org.cryptomator.ui.fxapp.FxApplicationScoped;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

@FxApplicationScoped
public class PasswordStrengthUtil {

	private static final int PW_TRUNC_LEN = 100; // truncate very long passwords, since zxcvbn memory and runtime depends vastly on the length
	private static final String RESSOURCE_PREFIX = "passwordStrength.messageLabel.";

	private final Zxcvbn zxcvbn;
	private final List<String> sanitizedInputs;
	private final ResourceBundle resourceBundle;

	@Inject
	public PasswordStrengthUtil(ResourceBundle resourceBundle) {
		this.resourceBundle = resourceBundle;
		this.zxcvbn = new Zxcvbn();
		this.sanitizedInputs = new ArrayList<>();
		this.sanitizedInputs.add("cryptomator");
	}

	public int computeRate(String password) {
		if (Strings.isNullOrEmpty(password)) {
			return -1;
		} else {
			int numCharsToRate = Math.min(PW_TRUNC_LEN, password.length());
			return zxcvbn.measure(password.substring(0, numCharsToRate), sanitizedInputs).getScore();
		}
	}

	public Color getStrengthColor(Number score) {
		switch (score.intValue()) {
			case 0:
				return Color.web("#e74c3c");
			case 1:
				return Color.web("#e67e22");
			case 2:
				return Color.web("#f1c40f");
			case 3:
				return Color.web("#40d47e");
			case 4:
				return Color.web("#27ae60");
			default:
				return Color.web("#ffffff", 0.5);
		}
	}

	public Background getBackgroundWithStrengthColor(Number score) {
		Color c = this.getStrengthColor(score);
		BackgroundFill fill = new BackgroundFill(c, CornerRadii.EMPTY, Insets.EMPTY);
		return new Background(fill);
	}

	public Background getBackgroundWithStrengthColor(Number score, Number threshold) {
		return score.intValue() >= threshold.intValue() ? getBackgroundWithStrengthColor(score) : getBackgroundWithStrengthColor(-1);
	}

	public String getStrengthDescription(Number score) {
		if (resourceBundle.containsKey(RESSOURCE_PREFIX + score.intValue())) {
			return resourceBundle.getString("passwordStrength.messageLabel." + score.intValue());
		} else {
			return "";
		}
	}

}

/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Jean-Noël Charon - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui.util;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.cryptomator.ui.settings.Localization;

import com.nulabinc.zxcvbn.Zxcvbn;

import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;

@Singleton
public class PasswordStrengthUtil {

	private final Zxcvbn zxcvbn;
	private final List<String> sanitizedInputs;
	private final Localization localization;

	@Inject
	public PasswordStrengthUtil(Localization localization) {
		this.localization = localization;
		this.zxcvbn = new Zxcvbn();
		this.sanitizedInputs = new ArrayList<>();
		this.sanitizedInputs.add("cryptomator");
	}

	public int computeRate(String password) {
		if (StringUtils.isEmpty(password)) {
			return -1;
		} else {
			return zxcvbn.measure(password, sanitizedInputs).getScore();
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
		String key = "initialize.messageLabel.passwordStrength." + score.intValue();
		if (localization.containsKey(key)) {
			return localization.getString(key);
		} else {
			return "";
		}
	}

}

/*******************************************************************************
 * Copyright (c) 2016, 2017 Sebastian Stenzel and others.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Jean-Noël Charon - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui.changepassword;

import com.nulabinc.zxcvbn.Zxcvbn;
import org.cryptomator.common.Environment;
import org.cryptomator.ui.fxapp.FxApplicationScoped;

import javax.inject.Inject;
import java.util.List;
import java.util.ResourceBundle;

@FxApplicationScoped
public class PasswordStrengthUtil {

	private static final int PW_TRUNC_LEN = 100; // truncate very long passwords, since zxcvbn memory and runtime depends vastly on the length
	private static final String RESSOURCE_PREFIX = "passwordStrength.messageLabel.";
	private static final List<String> SANITIZED_INPUTS = List.of("cryptomator");

	private final ResourceBundle resourceBundle;
	private final int minPwLength;
	private final Zxcvbn zxcvbn;

	@Inject
	public PasswordStrengthUtil(ResourceBundle resourceBundle, Environment environment) {
		this.resourceBundle = resourceBundle;
		this.minPwLength = environment.getMinPwLength();
		this.zxcvbn = new Zxcvbn();
	}

	public boolean fulfillsMinimumRequirements(CharSequence password) {
		return password.length() >= minPwLength;
	}

	public int computeRate(CharSequence password) {
		if (password == null || password.length() < minPwLength) {
			return -1;
		} else {
			int numCharsToRate = Math.min(PW_TRUNC_LEN, password.length());
			return zxcvbn.measure(password.subSequence(0, numCharsToRate), SANITIZED_INPUTS).getScore();
		}
	}

	public String getStrengthDescription(Number score) {
		if (score.intValue() == -1) {
			return String.format(resourceBundle.getString(RESSOURCE_PREFIX + "tooShort"), minPwLength);
		} else if (resourceBundle.containsKey(RESSOURCE_PREFIX + score.intValue())) {
			return resourceBundle.getString(RESSOURCE_PREFIX + score.intValue());
		} else {
			return "";
		}
	}

}

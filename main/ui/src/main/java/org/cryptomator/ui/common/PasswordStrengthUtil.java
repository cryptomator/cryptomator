/*******************************************************************************
 * Copyright (c) 2016, 2017 Sebastian Stenzel and others.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Jean-Noël Charon - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui.common;

import com.google.common.base.Strings;
import com.nulabinc.zxcvbn.Zxcvbn;
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

	public String getStrengthDescription(Number score) {
		if (resourceBundle.containsKey(RESSOURCE_PREFIX + score.intValue())) {
			return resourceBundle.getString("passwordStrength.messageLabel." + score.intValue());
		} else {
			return "";
		}
	}

}

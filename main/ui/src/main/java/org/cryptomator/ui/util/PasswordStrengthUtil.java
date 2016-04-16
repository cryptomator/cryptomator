/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Jean-Noël Charon - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui.util;

import com.nulabinc.zxcvbn.Zxcvbn;
import javafx.beans.property.IntegerProperty;
import javafx.scene.paint.Color;
import org.apache.commons.lang3.StringUtils;
import org.cryptomator.ui.settings.Localization;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class PasswordStrengthUtil {

    private final Zxcvbn zxcvbn;
    private final List<String> sanitizedInputs;
    private final Localization localization;

    @Inject
    public PasswordStrengthUtil(Localization localization){
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
        Color strengthColor = Color.web("#FF0000");
        switch (score.intValue()) {
            case 0:
                strengthColor = Color.web("#FF0000");
                break;
            case 1:
                strengthColor = Color.web("#FF8000");
                break;
            case 2:
                strengthColor = Color.web("#FFBF00");
                break;
            case 3:
                strengthColor = Color.web("#FFFF00");
                break;
            case 4:
                strengthColor = Color.web("#BFFF00");
                break;
            default:
                strengthColor = Color.web("#FF0000");
                break;
        }
        return strengthColor;
    }

    public int getWidth(Number score) {
        int width = 0;
        switch (score.intValue()) {
            case 0:
                width += 5;
                break;
            case 1:
                width += 25;
                break;
            case 2:
                width += 50;
                break;
            case 3:
                width += 75;
                break;
            case 4:
                width = 100;
                break;
            default:
                width = 0;
                break;
        }
        return Math.round(width*2.23f);
    }

    public float getStrokeWidth(Number score) {
        if (score.intValue() >= 0) {
            return 0.5f;
        } else {
            return 0;
        }
    }

    public String getStrengthDescription(Number score) {
        if (score.intValue() >= 0) {
            return String.format(localization.getString("initialize.messageLabel.passwordStrength"),
                    localization.getString("initialize.messageLabel.passwordStrength." + score.intValue()));
        } else {
            return "";
        }
    }

}

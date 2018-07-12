package org.cryptomator.ui.util;

import org.cryptomator.ui.l10n.Localization;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class PasswordStrengthUtilTest {

	@Test(timeout = 5000)
	public void testLongPasswords() {
		PasswordStrengthUtil util = new PasswordStrengthUtil(Mockito.mock(Localization.class));
		StringBuilder longPwBuilder = new StringBuilder();
		for (int i = 0; i < 10000; i++) {
			longPwBuilder.append('x');
		}
		util.computeRate(longPwBuilder.toString());
	}

}

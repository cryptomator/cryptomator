package org.cryptomator.ui.util;

import org.cryptomator.ui.l10n.Localization;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class PasswordStrengthUtilTest {

	@Test
	public void testLongPasswordsWillBeRatedAsStrong() {
		PasswordStrengthUtil util = new PasswordStrengthUtil(Mockito.mock(Localization.class));
		StringBuilder longPwBuilder = new StringBuilder();
		for (int i = 0; i < 101; i++) {
			longPwBuilder.append('x');
		}
		int strength = util.computeRate(longPwBuilder.toString());
		Assert.assertEquals(4, strength);
	}

}

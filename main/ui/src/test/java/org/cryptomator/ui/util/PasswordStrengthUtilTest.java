package org.cryptomator.ui.util;

import org.cryptomator.ui.l10n.Localization;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;

public class PasswordStrengthUtilTest {

	@Test
	public void testLongPasswords() {
		PasswordStrengthUtil util = new PasswordStrengthUtil(Mockito.mock(Localization.class));
		StringBuilder longPwBuilder = new StringBuilder();
		for (int i = 0; i < 10000; i++) {
			longPwBuilder.append('x');
		}
		Assertions.assertTimeout(Duration.ofSeconds(5), () -> {
			util.computeRate(longPwBuilder.toString());
		});
	}

}

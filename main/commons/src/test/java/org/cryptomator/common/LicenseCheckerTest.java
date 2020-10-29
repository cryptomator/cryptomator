package org.cryptomator.common;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

class LicenseCheckerTest {

	private static final String PUBLIC_KEY = "MIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQBgc4HZz+/fBbC7lmEww0AO3NK9wVZ" //
			+ "PDZ0VEnsaUFLEYpTzb90nITtJUcPUbvOsdZIZ1Q8fnbquAYgxXL5UgHMoywAib47" //
			+ "6MkyyYgPk0BXZq3mq4zImTRNuaU9slj9TVJ3ScT3L1bXwVuPJDzpr5GOFpaj+WwM" //
			+ "Al8G7CqwoJOsW7Kddns=";

	private LicenseChecker licenseChecker;

	@BeforeEach
	public void setup() {
		licenseChecker = new LicenseChecker(PUBLIC_KEY);
	}

	@Test
	public void testCheckValidLicense() {
		String license = "eyJhbGciOiJFUzUxMiIsInR5cCI6IkpXVCIsImtpZCI6InhaRGZacHJ5NFA5dlpQWnlHMmZOQlJqLTdMejVvbVZkbTd0SG9DZ1NOZlkifQ.eyJzdWIiOiJjcnlwdG9ib3RAZXhhbXBsZS5jb20iLCJpYXQiOjE1MTYyMzkwMjJ9.AQaBIKQdNCxmRJi2wLOcbagTgi39WhdWwgdpKTYSPicg-aPr_tst_RjmnqMemx3cBe0Blr4nEbj_lAtSKHz_i61fAUyI1xCIAZYbK9Q3ICHIHQl3AiuCpBwFl-k81OB4QDYiKpEc9gLN5dhW_VymJMsgOvyiC0UjC91f2AM7s46byDNj";

		Optional<DecodedJWT> decoded = licenseChecker.check(license);

		Assertions.assertTrue(decoded.isPresent());
		Assertions.assertEquals("cryptobot@example.com", decoded.get().getSubject());
	}

	@Test
	public void testCheckInvalidLicenseHeader() {
		String license = "EyJhbGciOiJFUzUxMiIsInR5cCI6IkpXVCIsImtpZCI6InhaRGZacHJ5NFA5dlpQWnlHMmZOQlJqLTdMejVvbVZkbTd0SG9DZ1NOZlkifQ.eyJzdWIiOiJjcnlwdG9ib3RAZXhhbXBsZS5jb20iLCJpYXQiOjE1MTYyMzkwMjJ9.AQaBIKQdNCxmRJi2wLOcbagTgi39WhdWwgdpKTYSPicg-aPr_tst_RjmnqMemx3cBe0Blr4nEbj_lAtSKHz_i61fAUyI1xCIAZYbK9Q3ICHIHQl3AiuCpBwFl-k81OB4QDYiKpEc9gLN5dhW_VymJMsgOvyiC0UjC91f2AM7s46byDNj";

		Optional<DecodedJWT> decoded = licenseChecker.check(license);

		Assertions.assertFalse(decoded.isPresent());
	}

	@Test
	public void testCheckInvalidLicensePayload() {
		String license = "eyJhbGciOiJFUzUxMiIsInR5cCI6IkpXVCIsImtpZCI6InhaRGZacHJ5NFA5dlpQWnlHMmZOQlJqLTdMejVvbVZkbTd0SG9DZ1NOZlkifQ.EyJzdWIiOiJjcnlwdG9ib3RAZXhhbXBsZS5jb20iLCJpYXQiOjE1MTYyMzkwMjJ9.AQaBIKQdNCxmRJi2wLOcbagTgi39WhdWwgdpKTYSPicg-aPr_tst_RjmnqMemx3cBe0Blr4nEbj_lAtSKHz_i61fAUyI1xCIAZYbK9Q3ICHIHQl3AiuCpBwFl-k81OB4QDYiKpEc9gLN5dhW_VymJMsgOvyiC0UjC91f2AM7s46byDNj";

		Optional<DecodedJWT> decoded = licenseChecker.check(license);

		Assertions.assertFalse(decoded.isPresent());
	}

	@Test
	public void testCheckInvalidLicenseSignature() {
		String license = "eyJhbGciOiJFUzUxMiIsInR5cCI6IkpXVCIsImtpZCI6InhaRGZacHJ5NFA5dlpQWnlHMmZOQlJqLTdMejVvbVZkbTd0SG9DZ1NOZlkifQ.eyJzdWIiOiJjcnlwdG9ib3RAZXhhbXBsZS5jb20iLCJpYXQiOjE1MTYyMzkwMjJ9.aQaBIKQdNCxmRJi2wLOcbagTgi39WhdWwgdpKTYSPicg-aPr_tst_RjmnqMemx3cBe0Blr4nEbj_lAtSKHz_i61fAUyI1xCIAZYbK9Q3ICHIHQl3AiuCpBwFl-k81OB4QDYiKpEc9gLN5dhW_VymJMsgOvyiC0UjC91f2AM7s46byDNj";

		Optional<DecodedJWT> decoded = licenseChecker.check(license);

		Assertions.assertFalse(decoded.isPresent());
	}

}
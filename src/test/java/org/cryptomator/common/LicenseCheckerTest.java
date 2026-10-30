package org.cryptomator.common;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LicenseCheckerTest {

	private static final String LEGACY_PUBLIC_KEY = """
			MIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQBiUVQ0HhZMuAOqiO2lPIT+MMSH4bcl\
			6BOWnFn205bzTcRI9RuRdtrXVNwp/IPtjMVXTj/oW0r12HcrEdLmi9QI6QASTEByW\
			LNTS/d94IoXmRYQTnC+RtH+H/4I1TWYw90aiig2yV0G1s0qCgAiyKswj+ST6r71NM\
			/gepmlW3+qiv9/PU=\
			""";

	private static final String TEST_ROOT_CERT = """
			-----BEGIN CERTIFICATE-----
			MIIBpjCCAVigAwIBAgIUJWMTr10U1lkxgPhxjed4kPqSu4EwBQYDK2VwMD8xCzAJ
			BgNVBAYTAkRFMRYwFAYDVQQKDA1Ta3ltYXRpYyBHbWJIMRgwFgYDVQQDDA9MaWNl
			bnNlIFRlc3QgQ0EwHhcNMjYwMjI2MTUzMzIwWhcNMzYwMjI0MTUzMzIwWjA/MQsw
			CQYDVQQGEwJERTEWMBQGA1UECgwNU2t5bWF0aWMgR21iSDEYMBYGA1UEAwwPTGlj
			ZW5zZSBUZXN0IENBMCowBQYDK2VwAyEAVi5WsfyHgHiL0vr4d2Lt1g7kPgC5m8u0
			DIKLalKJqHSjZjBkMBIGA1UdEwEB/wQIMAYBAf8CAQEwDgYDVR0PAQH/BAQDAgEG
			MB0GA1UdDgQWBBQBiO+yqMTag59eH8fgcF9gMsXeEzAfBgNVHSMEGDAWgBQBiO+y
			qMTag59eH8fgcF9gMsXeEzAFBgMrZXADQQD1I6bFKdHW+MaSpNVl/seCJny0Tp5L
			3+v6IyybvV0e66ks8BhsRWbqoSSBEaF4zlX7gAPzNuOIEFadM4s1fVMC
			-----END CERTIFICATE-----
			""";

	private static final String TEST_CN = "License Issuer CA (Test)";

	// test chain (leaf first) issued by TEST_ROOT_CERT, along with the leaf's private key
	private static final String LEAF_CERTIFICATE = "MIICDDCCAb6gAwIBAgIUDMOzxJY381JjdN82cW7qd+B3vh0wBQYDK2VwME4xCzAJBgNVBAYTAkRFMRYwFAYDVQQKDA1Ta3ltYXRpYyBHbWJIMScwJQYDVQQDDB5MaWNlbnNlIEludGVybWVkaWF0ZSBDQSAoVGVzdCkwHhcNMjYwMjI2MTU0MDM2WhcNMzYwMjI0MTU0MDM2WjBIMQswCQYDVQQGEwJERTEWMBQGA1UECgwNU2t5bWF0aWMgR21iSDEhMB8GA1UEAwwYTGljZW5zZSBJc3N1ZXIgQ0EgKFRlc3QpMIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQBiUVQ0HhZMuAOqiO2lPIT+MMSH4bcl6BOWnFn205bzTcRI9RuRdtrXVNwp/IPtjMVXTj/oW0r12HcrEdLmi9QI6QASTEByWLNTS/d94IoXmRYQTnC+RtH+H/4I1TWYw90aiig2yV0G1s0qCgAiyKswj+ST6r71NM/gepmlW3+qiv9/PWjQjBAMB0GA1UdDgQWBBSNjBwv+/iYQvpOOqz02u7xaASSITAfBgNVHSMEGDAWgBRMASsp1kiawJm8YoJ6+8/sq21X4zAFBgMrZXADQQA4Ok/+y0bdzm2RUmkHd6QFS6WbBKf9O4zz3Uc7iBMpKIq1kBlq+7TbbgMHJu+aYbODcRWT++5sx4i2OspkgOsJ";
	private static final String INTERMEDIATE_CERTIFICATE = "MIIBtTCCAWegAwIBAgIUJmx8UqtgnKcauQbqb04TaUBG+UIwBQYDK2VwMD8xCzAJBgNVBAYTAkRFMRYwFAYDVQQKDA1Ta3ltYXRpYyBHbWJIMRgwFgYDVQQDDA9MaWNlbnNlIFRlc3QgQ0EwHhcNMjYwMjI2MTUzNjM2WhcNMzYwMjI0MTUzNjM2WjBOMQswCQYDVQQGEwJERTEWMBQGA1UECgwNU2t5bWF0aWMgR21iSDEnMCUGA1UEAwweTGljZW5zZSBJbnRlcm1lZGlhdGUgQ0EgKFRlc3QpMCowBQYDK2VwAyEAG3vr8NK7YZs1q6pQtJhHDbT2xMD3C2ysmsnemw1FQlajZjBkMBIGA1UdEwEB/wQIMAYBAf8CAQAwDgYDVR0PAQH/BAQDAgEGMB0GA1UdDgQWBBRMASsp1kiawJm8YoJ6+8/sq21X4zAfBgNVHSMEGDAWgBQBiO+yqMTag59eH8fgcF9gMsXeEzAFBgMrZXADQQBKdGG2njVkrj0L1meWU4NplZdyGQ2qaCq4PRzy59H9YmDS793RlMa4MOZwAmTkUQ5vakuvtz134Iiz6jJkDBgF";
	private static final String LEAF_PRIVATE_KEY = "MGACAQAwEAYHKoZIzj0CAQYFK4EEACMESTBHAgEBBEIA+tBtqmK6OyXS+0ATPadXIF3mf1uwAY/ujIbhtox+dcqolusy8fR8cIVYNqbRb8wUZvbY++xn24nsDAiw6Za4NTg=";
	// period during which both leaf and intermediate are valid (leaf notBefore, intermediate notAfter)
	private static final Instant CHAIN_NOT_BEFORE = Instant.parse("2026-02-26T15:40:36Z");
	private static final Instant CHAIN_NOT_AFTER = Instant.parse("2036-02-24T15:36:36Z");

	private LicenseChecker licenseChecker;

	@BeforeEach
	public void setup() {
		licenseChecker = new LicenseChecker(LEGACY_PUBLIC_KEY, TEST_ROOT_CERT, TEST_CN);
	}

	@Test
	public void testCheckValidLegacyLicense() {
		String license = "eyJhbGciOiJFUzUxMiJ9.eyJhdWQiOiJDcnlwdG9tYXRvciIsInN1YiI6ImNyeXB0b2JvdEBleGFtcGxlLmNvbSIsImtpZCI6IllFVjFFSXY5dllMR1lfR1RoQ0VPTGtleW1xUDBlZVhVVTdNbWxKaHFnR0EiLCJpc3MiOiJodHRwOlwvXC9sb2NhbGhvc3Q6ODA4MVwvIiwiZXhwIjozMjUwMzY4MDAwMCwiaWF0Ijo5NDY2ODQ4MDAsInNlYXRzIjozfQ.ATHMZ95Z3cx-ghYvbT9XBT-Z-c4BWJOK6WM3JbOjCzIO-3pddYpey0uAfMipIfLzHZypE4NPtNR4nB8z7JRGcqj8AMBpXBxZnDGXsftGHkOuHw6kTq2b-HPmcYBKFZ4hH7ptQRS9byaypFc7ftonLyYODWqar6DXtHSJtlf01jnuAdVi";

		Optional<DecodedJWT> decoded = licenseChecker.check(license);

		Assertions.assertTrue(decoded.isPresent());
		Assertions.assertEquals("cryptobot@example.com", decoded.get().getSubject());
	}

	@Test
	public void testCheckValidLicense() {
		String license = signTestLicense(CHAIN_NOT_BEFORE.plus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS));

		Optional<DecodedJWT> decoded = licenseChecker.check(license);

		Assertions.assertTrue(decoded.isPresent());
		Assertions.assertEquals("cryptobot@example.com", decoded.get().getSubject());
	}

	@Test
	public void testCheckLicenseSignedWithChainExpiredSince() {
		// validity is checked against iat (not now), so a license must not become invalid when its intermediate certificate gets rotated out; cf. timestamped code signing
		var root = X509Helper.parsePemCertificate(TEST_ROOT_CERT);
		var chain = X509Helper.parseX5cCertificateChain(List.of(LEAF_CERTIFICATE, INTERMEDIATE_CERTIFICATE));

		Assertions.assertDoesNotThrow(() -> X509Helper.validateChain(chain, root, CHAIN_NOT_BEFORE.plus(1, ChronoUnit.DAYS)));
		Assertions.assertThrows(GeneralSecurityException.class, () -> X509Helper.validateChain(chain, root, CHAIN_NOT_AFTER.plus(1, ChronoUnit.DAYS)));
	}

	@Test
	public void testRejectLicenseIssuedBeforeChainValidity() {
		String license = signTestLicense(CHAIN_NOT_BEFORE.minus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS));

		Optional<DecodedJWT> decoded = licenseChecker.check(license);

		Assertions.assertFalse(decoded.isPresent());
	}

	@Test
	public void testRejectLicenseIssuedAfterChainExpiry() {
		String license = signTestLicense(CHAIN_NOT_AFTER.plus(1, ChronoUnit.DAYS), CHAIN_NOT_AFTER.plus(2, ChronoUnit.DAYS));

		Optional<DecodedJWT> decoded = licenseChecker.check(license);

		Assertions.assertFalse(decoded.isPresent());
	}

	@Test
	public void testRejectX5cLicenseWithoutIat() {
		String license = JWT.create() //
				.withHeader(Map.of("x5c", List.of(LEAF_CERTIFICATE, INTERMEDIATE_CERTIFICATE))) //
				.withSubject("cryptobot@example.com") //
				.withExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS)) //
				.sign(Algorithm.ECDSA512(null, decodePrivateKey(LEAF_PRIVATE_KEY)));

		Optional<DecodedJWT> decoded = licenseChecker.check(license);

		Assertions.assertFalse(decoded.isPresent());
	}

	@Test
	public void testCheckInvalidLicenseHeader() {
		String license = "EyJ4NWMiOlsiTUlJQ0REQ0NBYjZnQXdJQkFnSVVETU96eEpZMzgxSmpkTjgyY1c3cWQrQjN2aDB3QlFZREsyVndNRTR4Q3pBSkJnTlZCQVlUQWtSRk1SWXdGQVlEVlFRS0RBMVRhM2x0WVhScFl5QkhiV0pJTVNjd0pRWURWUVFEREI1TWFXTmxibk5sSUVsdWRHVnliV1ZrYVdGMFpTQkRRU0FvVkdWemRDa3dIaGNOTWpZd01qSTJNVFUwTURNMldoY05Nell3TWpJME1UVTBNRE0yV2pCSU1Rc3dDUVlEVlFRR0V3SkVSVEVXTUJRR0ExVUVDZ3dOVTJ0NWJXRjBhV01nUjIxaVNERWhNQjhHQTFVRUF3d1lUR2xqWlc1elpTQkpjM04xWlhJZ1EwRWdLRlJsYzNRcE1JR2JNQkFHQnlxR1NNNDlBZ0VHQlN1QkJBQWpBNEdHQUFRQmlVVlEwSGhaTXVBT3FpTzJsUElUK01NU0g0YmNsNkJPV25GbjIwNWJ6VGNSSTlSdVJkdHJYVk53cFwvSVB0ak1WWFRqXC9vVzByMTJIY3JFZExtaTlRSTZRQVNURUJ5V0xOVFNcL2Q5NElvWG1SWVFUbkMrUnRIK0hcLzRJMVRXWXc5MGFpaWcyeVYwRzFzMHFDZ0FpeUtzd2orU1Q2cjcxTk1cL2dlcG1sVzMrcWl2OVwvUFdqUWpCQU1CMEdBMVVkRGdRV0JCU05qQnd2K1wvaVlRdnBPT3F6MDJ1N3hhQVNTSVRBZkJnTlZIU01FR0RBV2dCUk1BU3NwMWtpYXdKbThZb0o2KzhcL3NxMjFYNHpBRkJnTXJaWEFEUVFBNE9rXC8reTBiZHptMlJVbWtIZDZRRlM2V2JCS2Y5TzR6ejNVYzdpQk1wS0lxMWtCbHErN1RiYmdNSEp1K2FZYk9EY1JXVCsrNXN4NGkyT3Nwa2dPc0oiLCJNSUlCdFRDQ0FXZWdBd0lCQWdJVUpteDhVcXRnbktjYXVRYnFiMDRUYVVCRytVSXdCUVlESzJWd01EOHhDekFKQmdOVkJBWVRBa1JGTVJZd0ZBWURWUVFLREExVGEzbHRZWFJwWXlCSGJXSklNUmd3RmdZRFZRUUREQTlNYVdObGJuTmxJRlJsYzNRZ1EwRXdIaGNOTWpZd01qSTJNVFV6TmpNMldoY05Nell3TWpJME1UVXpOak0yV2pCT01Rc3dDUVlEVlFRR0V3SkVSVEVXTUJRR0ExVUVDZ3dOVTJ0NWJXRjBhV01nUjIxaVNERW5NQ1VHQTFVRUF3d2VUR2xqWlc1elpTQkpiblJsY20xbFpHbGhkR1VnUTBFZ0tGUmxjM1FwTUNvd0JRWURLMlZ3QXlFQUczdnI4Tks3WVpzMXE2cFF0SmhIRGJUMnhNRDNDMnlzbXNuZW13MUZRbGFqWmpCa01CSUdBMVVkRXdFQlwvd1FJTUFZQkFmOENBUUF3RGdZRFZSMFBBUUhcL0JBUURBZ0VHTUIwR0ExVWREZ1FXQkJSTUFTc3Axa2lhd0ptOFlvSjYrOFwvc3EyMVg0ekFmQmdOVkhTTUVHREFXZ0JRQmlPK3lxTVRhZzU5ZUg4ZmdjRjlnTXNYZUV6QUZCZ01yWlhBRFFRQktkR0cybmpWa3JqMEwxbWVXVTROcGxaZHlHUTJxYUNxNFBSenk1OUg5WW1EUzc5M1JsTWE0TU9ad0FtVGtVUTV2YWt1dnR6MTM0SWl6NmpKa0RCZ0YiXSwiYWxnIjoiRVM1MTIifQ.eyJhdWQiOiJDcnlwdG9tYXRvciIsInN1YiI6ImNyeXB0b2JvdEBleGFtcGxlLmNvbSIsImtpZCI6IllFVjFFSXY5dllMR1lfR1RoQ0VPTGtleW1xUDBlZVhVVTdNbWxKaHFnR0EiLCJpc3MiOiJodHRwOlwvXC9sb2NhbGhvc3Q6ODA4MVwvIiwiZXhwIjozMjUwMzY4MDAwMCwiaWF0Ijo5NDY2ODQ4MDAsInNlYXRzIjozfQ.AMBwG0CnIu8FHLsmynfT-JHwE-pwjXGh8SoExaYWY6n88cL18gzI_tYl3cjZSvLVZImg-VRZi7SkhDVt5zrOXX12AdNst1jJc0HJIA0dJUiM22b3XmkIjionTXmK-Njgp9XIQV4qtYoFPXjsL4KKTa95yx5oTvDD7ZAHFxur5_nN3y4t";

		Optional<DecodedJWT> decoded = licenseChecker.check(license);

		Assertions.assertFalse(decoded.isPresent());
	}

	@Test
	public void testCheckInvalidLicensePayload() {
		String license = "eyJ4NWMiOlsiTUlJQ0REQ0NBYjZnQXdJQkFnSVVETU96eEpZMzgxSmpkTjgyY1c3cWQrQjN2aDB3QlFZREsyVndNRTR4Q3pBSkJnTlZCQVlUQWtSRk1SWXdGQVlEVlFRS0RBMVRhM2x0WVhScFl5QkhiV0pJTVNjd0pRWURWUVFEREI1TWFXTmxibk5sSUVsdWRHVnliV1ZrYVdGMFpTQkRRU0FvVkdWemRDa3dIaGNOTWpZd01qSTJNVFUwTURNMldoY05Nell3TWpJME1UVTBNRE0yV2pCSU1Rc3dDUVlEVlFRR0V3SkVSVEVXTUJRR0ExVUVDZ3dOVTJ0NWJXRjBhV01nUjIxaVNERWhNQjhHQTFVRUF3d1lUR2xqWlc1elpTQkpjM04xWlhJZ1EwRWdLRlJsYzNRcE1JR2JNQkFHQnlxR1NNNDlBZ0VHQlN1QkJBQWpBNEdHQUFRQmlVVlEwSGhaTXVBT3FpTzJsUElUK01NU0g0YmNsNkJPV25GbjIwNWJ6VGNSSTlSdVJkdHJYVk53cFwvSVB0ak1WWFRqXC9vVzByMTJIY3JFZExtaTlRSTZRQVNURUJ5V0xOVFNcL2Q5NElvWG1SWVFUbkMrUnRIK0hcLzRJMVRXWXc5MGFpaWcyeVYwRzFzMHFDZ0FpeUtzd2orU1Q2cjcxTk1cL2dlcG1sVzMrcWl2OVwvUFdqUWpCQU1CMEdBMVVkRGdRV0JCU05qQnd2K1wvaVlRdnBPT3F6MDJ1N3hhQVNTSVRBZkJnTlZIU01FR0RBV2dCUk1BU3NwMWtpYXdKbThZb0o2KzhcL3NxMjFYNHpBRkJnTXJaWEFEUVFBNE9rXC8reTBiZHptMlJVbWtIZDZRRlM2V2JCS2Y5TzR6ejNVYzdpQk1wS0lxMWtCbHErN1RiYmdNSEp1K2FZYk9EY1JXVCsrNXN4NGkyT3Nwa2dPc0oiLCJNSUlCdFRDQ0FXZWdBd0lCQWdJVUpteDhVcXRnbktjYXVRYnFiMDRUYVVCRytVSXdCUVlESzJWd01EOHhDekFKQmdOVkJBWVRBa1JGTVJZd0ZBWURWUVFLREExVGEzbHRZWFJwWXlCSGJXSklNUmd3RmdZRFZRUUREQTlNYVdObGJuTmxJRlJsYzNRZ1EwRXdIaGNOTWpZd01qSTJNVFV6TmpNMldoY05Nell3TWpJME1UVXpOak0yV2pCT01Rc3dDUVlEVlFRR0V3SkVSVEVXTUJRR0ExVUVDZ3dOVTJ0NWJXRjBhV01nUjIxaVNERW5NQ1VHQTFVRUF3d2VUR2xqWlc1elpTQkpiblJsY20xbFpHbGhkR1VnUTBFZ0tGUmxjM1FwTUNvd0JRWURLMlZ3QXlFQUczdnI4Tks3WVpzMXE2cFF0SmhIRGJUMnhNRDNDMnlzbXNuZW13MUZRbGFqWmpCa01CSUdBMVVkRXdFQlwvd1FJTUFZQkFmOENBUUF3RGdZRFZSMFBBUUhcL0JBUURBZ0VHTUIwR0ExVWREZ1FXQkJSTUFTc3Axa2lhd0ptOFlvSjYrOFwvc3EyMVg0ekFmQmdOVkhTTUVHREFXZ0JRQmlPK3lxTVRhZzU5ZUg4ZmdjRjlnTXNYZUV6QUZCZ01yWlhBRFFRQktkR0cybmpWa3JqMEwxbWVXVTROcGxaZHlHUTJxYUNxNFBSenk1OUg5WW1EUzc5M1JsTWE0TU9ad0FtVGtVUTV2YWt1dnR6MTM0SWl6NmpKa0RCZ0YiXSwiYWxnIjoiRVM1MTIifQ.EyJhdWQiOiJDcnlwdG9tYXRvciIsInN1YiI6ImNyeXB0b2JvdEBleGFtcGxlLmNvbSIsImtpZCI6IllFVjFFSXY5dllMR1lfR1RoQ0VPTGtleW1xUDBlZVhVVTdNbWxKaHFnR0EiLCJpc3MiOiJodHRwOlwvXC9sb2NhbGhvc3Q6ODA4MVwvIiwiZXhwIjozMjUwMzY4MDAwMCwiaWF0Ijo5NDY2ODQ4MDAsInNlYXRzIjozfQ.AMBwG0CnIu8FHLsmynfT-JHwE-pwjXGh8SoExaYWY6n88cL18gzI_tYl3cjZSvLVZImg-VRZi7SkhDVt5zrOXX12AdNst1jJc0HJIA0dJUiM22b3XmkIjionTXmK-Njgp9XIQV4qtYoFPXjsL4KKTa95yx5oTvDD7ZAHFxur5_nN3y4t";

		Optional<DecodedJWT> decoded = licenseChecker.check(license);

		Assertions.assertFalse(decoded.isPresent());
	}

	@Test
	public void testCheckInvalidLicenseSignature() {
		String validLicense = signTestLicense(CHAIN_NOT_BEFORE.plus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS));
		String license = validLicense.substring(0, validLicense.length() - 1) + (validLicense.endsWith("A") ? "B" : "A"); // tamper with the last byte of the signature

		Optional<DecodedJWT> decoded = licenseChecker.check(license);

		Assertions.assertFalse(decoded.isPresent());
	}

	@Test
	public void testCheckEmptyX5c() {
		// header: {"x5c":[],"alg":"ES512"}
		String license = "eyJ4NWMiOltdLCJhbGciOiJFUzUxMiJ9.eyJhdWQiOiJDcnlwdG9tYXRvciIsInN1YiI6ImNyeXB0b2JvdEBleGFtcGxlLmNvbSIsImtpZCI6IllFVjFFSXY5dllMR1lfR1RoQ0VPTGtleW1xUDBlZVhVVTdNbWxKaHFnR0EiLCJpc3MiOiJodHRwOlwvXC9sb2NhbGhvc3Q6ODA4MVwvIiwiZXhwIjozMjUwMzY4MDAwMCwiaWF0Ijo5NDY2ODQ4MDAsInNlYXRzIjozfQ.AMBwG0CnIu8FHLsmynfT-JHwE-pwjXGh8SoExaYWY6n88cL18gzI_tYl3cjZSvLVZImg-VRZi7SkhDVt5zrOXX12AdNst1jJc0HJIA0dJUiM22b3XmkIjionTXmK-Njgp9XIQV4qtYoFPXjsL4KKTa95yx5oTvDD7ZAHFxur5_nN3y4t";

		Optional<DecodedJWT> decoded = licenseChecker.check(license);

		Assertions.assertFalse(decoded.isPresent());
	}

	@Test
	public void testCheckMalformedX5cEntry() {
		// header: {"x5c":["bm90IGEgY2VydGlmaWNhdGU="],"alg":"ES512"}, i.e. valid Base64 but not a DER certificate
		String license = "eyJ4NWMiOlsiYm05MElHRWdZMlZ5ZEdsbWFXTmhkR1U9Il0sImFsZyI6IkVTNTEyIn0.eyJhdWQiOiJDcnlwdG9tYXRvciIsInN1YiI6ImNyeXB0b2JvdEBleGFtcGxlLmNvbSIsImtpZCI6IllFVjFFSXY5dllMR1lfR1RoQ0VPTGtleW1xUDBlZVhVVTdNbWxKaHFnR0EiLCJpc3MiOiJodHRwOlwvXC9sb2NhbGhvc3Q6ODA4MVwvIiwiZXhwIjozMjUwMzY4MDAwMCwiaWF0Ijo5NDY2ODQ4MDAsInNlYXRzIjozfQ.AMBwG0CnIu8FHLsmynfT-JHwE-pwjXGh8SoExaYWY6n88cL18gzI_tYl3cjZSvLVZImg-VRZi7SkhDVt5zrOXX12AdNst1jJc0HJIA0dJUiM22b3XmkIjionTXmK-Njgp9XIQV4qtYoFPXjsL4KKTa95yx5oTvDD7ZAHFxur5_nN3y4t";

		Optional<DecodedJWT> decoded = licenseChecker.check(license);

		Assertions.assertFalse(decoded.isPresent());
	}

	// signs a license with the test chain's leaf key, carrying the test chain in its x5c header
	private static String signTestLicense(Instant issuedAt, Instant expiresAt) {
		return JWT.create() //
				.withHeader(Map.of("x5c", List.of(LEAF_CERTIFICATE, INTERMEDIATE_CERTIFICATE))) //
				.withSubject("cryptobot@example.com") //
				.withIssuedAt(issuedAt) //
				.withExpiresAt(expiresAt) //
				.sign(Algorithm.ECDSA512(null, decodePrivateKey(LEAF_PRIVATE_KEY)));
	}

	private static ECPrivateKey decodePrivateKey(String encodedPkcs8PrivateKey) {
		try {
			byte[] keyBytes = Base64.getDecoder().decode(encodedPkcs8PrivateKey);
			return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
		} catch (GeneralSecurityException e) {
			throw new IllegalArgumentException("Invalid test private key", e);
		}
	}

}

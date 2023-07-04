package org.cryptomator.ui.keyloading.hub;

import com.nimbusds.jose.JWEObject;
import org.cryptomator.cryptolib.api.MasterkeyLoadingFailedException;
import org.cryptomator.cryptolib.common.P384KeyPair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Base64;

@SuppressWarnings("resource")
public class JWEHelperTest {

	// key pairs from frontend tests (crypto.spec.ts):
	private static final String USER_PRIV_KEY = "MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDDCi4K1Ts3DgTz/ufkLX7EGMHjGpJv+WJmFgyzLwwaDFSfLpDw0Kgf3FKK+LAsV8r+hZANiAARLOtFebIjxVYUmDV09Q1sVxz2Nm+NkR8fu6UojVSRcCW13tEZatx8XGrIY9zC7oBCEdRqDc68PMSvS5RA0Pg9cdBNc/kgMZ1iEmEv5YsqOcaNADDSs0bLlXb35pX7Kx5Y=";
	private static final String USER_PUB_KEY = "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAESzrRXmyI8VWFJg1dPUNbFcc9jZvjZEfH7ulKI1UkXAltd7RGWrcfFxqyGPcwu6AQhHUag3OvDzEr0uUQND4PXHQTXP5IDGdYhJhL+WLKjnGjQAw0rNGy5V29+aV+yseW";
	private static final String DEVICE_PRIV_KEY = "MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDB2bmFCWy2p+EbAn8NWS5Om+GA7c5LHhRZb8g2pSMSf0fsd7k7dZDVrnyHFiLdd/YGhZANiAAR6bsjTEdXKWIuu1Bvj6Y8wySlIROy7YpmVZTY128ItovCD8pcR4PnFljvAIb2MshCdr1alX4g6cgDOqcTeREiObcSfucOU9Ry1pJ/GnX6KA0eSljrk6rxjSDos8aiZ6Mg=";
	private static final String DEVICE_PUB_KEY = "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEem7I0xHVyliLrtQb4+mPMMkpSETsu2KZlWU2NdvCLaLwg/KXEeD5xZY7wCG9jLIQna9WpV+IOnIAzqnE3kRIjm3En7nDlPUctaSfxp1+igNHkpY65Oq8Y0g6LPGomejI";

	// used for JWE generation in frontend: (jwe.spec.ts):
	private static final String PRIV_KEY = "ME8CAQAwEAYHKoZIzj0CAQYFK4EEACIEODA2AgEBBDEA6QybmBitf94veD5aCLr7nlkF5EZpaXHCfq1AXm57AKQyGOjTDAF9EQB28fMywTDQ";
	private static final String PUB_KEY = "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAERxQR+NRN6Wga01370uBBzr2NHDbKIC56tPUEq2HX64RhITGhii8Zzbkb1HnRmdF0aq6uqmUy4jUhuxnKxsv59A6JeK7Unn+mpmm3pQAygjoGc9wrvoH4HWJSQYUlsXDu";

	@Test
	@DisplayName("decryptUserKey with device key")
	public void testDecryptUserKeyECDHES() throws ParseException, InvalidKeySpecException {
		var jwe = JWEObject.parse("""
			eyJhbGciOiJFQ0RILUVTIiwiZW5jIjoiQTI1NkdDTSIsImVwayI6eyJrZXlfb3BzIjpbXSwiZXh0Ijp\
			0cnVlLCJrdHkiOiJFQyIsIngiOiJoeHpiSWh6SUJza3A5ZkZFUmJSQ2RfOU1fbWYxNElqaDZhcnNoVX\
			NkcEEyWno5ejZZNUs4NHpZR2I4b2FHemNUIiwieSI6ImJrMGRaNWhpelZ0TF9hN2hNejBjTUduNjhIR\
			jZFdWlyNHdlclNkTFV5QWd2NWUzVzNYSG5sdHJ2VlRyU3pzUWYiLCJjcnYiOiJQLTM4NCJ9LCJhcHUi\
			OiIiLCJhcHYiOiIifQ..pu3Q1nR_yvgRAapG.4zW0xm0JPxbcvZ66R-Mn3k841lHelDQfaUvsZZAtWs\
			L2w4FMi6H_uu6ArAWYLtNREa_zfcPuyuJsFferYPSNRUWt4OW6aWs-l_wfo7G1ceEVxztQXzQiwD30U\
			TA8OOdPcUuFfEq2-d9217jezrcyO6m6FjyssEZIrnRArUPWKzGdghXccGkkf0LTZcGJoHeKal-RtyP8\
			PfvEAWTjSOCpBlSdUJ-1JL3tyd97uVFNaVuH3i7vvcMoUP_bdr0XW3rvRgaeC6X4daPLUvR1hK5Msut\
			QMtM2vpFghS_zZxIQRqz3B2ECxa9Bjxhmn8kLX5heZ8fq3lH-bmJp1DxzZ4V1RkWk.yVwXG9yARa5Ih\
			q2koh2NbQ""");
		var deviceKeyPair = P384KeyPair.create(new X509EncodedKeySpec(Base64.getDecoder().decode(DEVICE_PUB_KEY)), new PKCS8EncodedKeySpec(Base64.getDecoder().decode(DEVICE_PRIV_KEY)));

		var userKey = JWEHelper.decryptUserKey(jwe, deviceKeyPair.getPrivate());

		Assertions.assertArrayEquals(Base64.getDecoder().decode(USER_PRIV_KEY), userKey.getEncoded());
	}

	@Test
	@DisplayName("decryptUserKey with incorrect device key")
	public void testDecryptUserKeyECDHESWrongKey() throws ParseException, InvalidKeySpecException {
		var jwe = JWEObject.parse("""
			eyJhbGciOiJFQ0RILUVTIiwiZW5jIjoiQTI1NkdDTSIsImVwayI6eyJrZXlfb3BzIjpbXSwiZXh0Ijp\
			0cnVlLCJrdHkiOiJFQyIsIngiOiJoeHpiSWh6SUJza3A5ZkZFUmJSQ2RfOU1fbWYxNElqaDZhcnNoVX\
			NkcEEyWno5ejZZNUs4NHpZR2I4b2FHemNUIiwieSI6ImJrMGRaNWhpelZ0TF9hN2hNejBjTUduNjhIR\
			jZFdWlyNHdlclNkTFV5QWd2NWUzVzNYSG5sdHJ2VlRyU3pzUWYiLCJjcnYiOiJQLTM4NCJ9LCJhcHUi\
			OiIiLCJhcHYiOiIifQ..pu3Q1nR_yvgRAapG.4zW0xm0JPxbcvZ66R-Mn3k841lHelDQfaUvsZZAtWs\
			L2w4FMi6H_uu6ArAWYLtNREa_zfcPuyuJsFferYPSNRUWt4OW6aWs-l_wfo7G1ceEVxztQXzQiwD30U\
			TA8OOdPcUuFfEq2-d9217jezrcyO6m6FjyssEZIrnRArUPWKzGdghXccGkkf0LTZcGJoHeKal-RtyP8\
			PfvEAWTjSOCpBlSdUJ-1JL3tyd97uVFNaVuH3i7vvcMoUP_bdr0XW3rvRgaeC6X4daPLUvR1hK5Msut\
			QMtM2vpFghS_zZxIQRqz3B2ECxa9Bjxhmn8kLX5heZ8fq3lH-bmJp1DxzZ4V1RkWk.yVwXG9yARa5Ih\
			q2koh2NbQ""");
		var userKeyPair = P384KeyPair.create(new X509EncodedKeySpec(Base64.getDecoder().decode(USER_PUB_KEY)), new PKCS8EncodedKeySpec(Base64.getDecoder().decode(USER_PRIV_KEY)));
		var incorrectDevicePrivateKey = userKeyPair.getPrivate();

		Assertions.assertThrows(JWEHelper.InvalidJweKeyException.class, () -> JWEHelper.decryptUserKey(jwe, incorrectDevicePrivateKey));
	}

	@Test
	@DisplayName("decryptUserKey with setup code")
	public void testDecryptUserKeyPBES2() throws ParseException {
		var jwe = JWEObject.parse("""
				eyJhbGciOiJQQkVTMi1IUzUxMitBMjU2S1ciLCJlbmMiOiJBMjU2R0NNIiwicDJzIjoiT3hMY0Q\
				xX1pCODc1c2hvUWY2Q1ZHQSIsInAyYyI6MTAwMCwiYXB1IjoiIiwiYXB2IjoiIn0.FD4fcrP4Pb\
				aKOQ9ZfXl0gpMM6Fa2rfqAvL0K5ZyYUiVeHCNV-A02Rg.urT1ShSv6qQxh8X7.gEqAiUWD98a2E\
				P7ITCPTw4DJo6-BpqrxA73D6gNIj9z4d1hN-EP99Q4mWBWLH97H8ugbG5rGsm8xsjsBqpWORQqF\
				mJZR2AhlPiwFaC7n_MDDBupSy_swDnCfj731Lal297IP5WbkFcmozKsyhmwdkctxjf_VHA.fJki\
				kDjUaxwUKqpvT7qaAQ
				""");

		var userKey = JWEHelper.decryptUserKey(jwe, "123456");

		Assertions.assertArrayEquals(Base64.getDecoder().decode(PRIV_KEY), userKey.getEncoded());
	}

	@Test
	@DisplayName("decryptUserKey with incorrect setup code")
	public void testDecryptUserKeyPBES2WrongKey() throws ParseException {
		var jwe = JWEObject.parse("""
				eyJhbGciOiJQQkVTMi1IUzUxMitBMjU2S1ciLCJlbmMiOiJBMjU2R0NNIiwicDJzIjoiT3hMY0Q\
				xX1pCODc1c2hvUWY2Q1ZHQSIsInAyYyI6MTAwMCwiYXB1IjoiIiwiYXB2IjoiIn0.FD4fcrP4Pb\
				aKOQ9ZfXl0gpMM6Fa2rfqAvL0K5ZyYUiVeHCNV-A02Rg.urT1ShSv6qQxh8X7.gEqAiUWD98a2E\
				P7ITCPTw4DJo6-BpqrxA73D6gNIj9z4d1hN-EP99Q4mWBWLH97H8ugbG5rGsm8xsjsBqpWORQqF\
				mJZR2AhlPiwFaC7n_MDDBupSy_swDnCfj731Lal297IP5WbkFcmozKsyhmwdkctxjf_VHA.fJki\
				kDjUaxwUKqpvT7qaAQ
				""");

		Assertions.assertThrows(JWEHelper.InvalidJweKeyException.class, () -> JWEHelper.decryptUserKey(jwe, "654321"));
	}

	@Test
	@DisplayName("decryptVaultKey")
	public void testDecryptVaultKey() throws ParseException, InvalidKeySpecException {
		var jwe = JWEObject.parse("""
			eyJhbGciOiJFQ0RILUVTIiwiZW5jIjoiQTI1NkdDTSIsImVwayI6eyJrdHkiOiJFQyIsImNydiI6IlA\
			tMzg0Iiwia2V5X29wcyI6W10sImV4dCI6dHJ1ZSwieCI6IllUcEY3bGtTc3JvZVVUVFdCb21LNzBTN0\
			FhVTJyc0ptMURpZ1ZzbjRMY2F5eUxFNFBabldkYmFVcE9jQVV5a1ciLCJ5IjoiLU5pS3loUktjSk52N\
			m02Z0ZJUWc4cy1Xd1VXUW9uT3A5dkQ4cHpoa2tUU3U2RzFlU2FUTVlhZGltQ2Q4V0ExMSJ9LCJhcHUi\
			OiIiLCJhcHYiOiIifQ..BECWGzd9UvhHcTJC.znt4TlS-qiNEjxiu2v-du_E1QOBnyBR6LCt865SHxD\
			-kwRc1JwX_Lq9XVoFj2GnK9-9CgxhCLGurg5Jt9g38qv2brGAzWL7eSVeY1fIqdO_kUhLpGslRTN6h2\
			U0NHJi2-iE.WDVI2kOk9Dy3PWHyIg8gKA""");
		var privateKey = P384KeyPair.create(new X509EncodedKeySpec(Base64.getDecoder().decode(PUB_KEY)), new PKCS8EncodedKeySpec(Base64.getDecoder().decode(PRIV_KEY))).getPrivate();

		var masterkey = JWEHelper.decryptVaultKey(jwe, privateKey);

		var expectedEncKey = new byte[32];
		var expectedMacKey = new byte[32];
		Arrays.fill(expectedEncKey, (byte) 0x55);
		Arrays.fill(expectedMacKey, (byte) 0x77);
		Assertions.assertArrayEquals(expectedEncKey, masterkey.getEncKey().getEncoded());
		Assertions.assertArrayEquals(expectedMacKey, masterkey.getMacKey().getEncoded());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"eyJhbGciOiJFQ0RILUVTIiwiZW5jIjoiQTI1NkdDTSIsImVwayI6eyJrdHkiOiJFQyIsImNydiI6IlAtMzg0Iiwia2V5X29wcyI6W10sImV4dCI6dHJ1ZSwieCI6ImdodGR3VnNoUU8wRGFBdjVBOXBiZ1NCTW0yYzZKWVF4dkloR3p6RVdQTncxczZZcEFYeTRQTjBXRFJUWExtQ2wiLCJ5IjoiN3Rncm1Gd016NGl0ZmVQNzBndkpLcjRSaGdjdENCMEJHZjZjWE9WZ2M0bjVXMWQ4dFgxZ1RQakdrczNVSm1zUiJ9LCJhcHUiOiIiLCJhcHYiOiIifQ..x6JWRGSojUJUJYpp.5BRuzcaV.lLIhGH7Wz0n_iTBAubDFZA", // wrong key
			"eyJhbGciOiJFQ0RILUVTIiwiZW5jIjoiQTI1NkdDTSIsImVwayI6eyJrdHkiOiJFQyIsImNydiI6IlAtMzg0Iiwia2V5X29wcyI6W10sImV4dCI6dHJ1ZSwieCI6IkM2bWhsNE5BTHhEdHMwUlFlNXlyZWxQVDQyOGhDVzJNeUNYS3EwdUI0TDFMdnpXRHhVaVk3YTdZcEhJakJXcVoiLCJ5IjoiakM2dWc1NE9tbmdpNE9jUk1hdkNrczJpcFpXQjdkUmotR3QzOFhPSDRwZ2tpQ0lybWNlUnFxTnU3Z0c3Qk1yOSJ9LCJhcHUiOiIiLCJhcHYiOiIifQ..HNJJghL-SvERFz2v.N0z8YwFg.rYw29iX4i8XujdM4P4KKWg", // payload is not json
			"eyJhbGciOiJFQ0RILUVTIiwiZW5jIjoiQTI1NkdDTSIsImVwayI6eyJrdHkiOiJFQyIsImNydiI6IlAtMzg0Iiwia2V5X29wcyI6W10sImV4dCI6dHJ1ZSwieCI6InB3R05vcXRnY093MkJ6RDVmSnpBWDJvMzUwSWNsY3A5cFdVTHZ5VDRqRWVCRWdCc3hhTVJXQ1ZyNlJMVUVXVlMiLCJ5IjoiZ2lIVEE5MlF3VU5lbmg1OFV1bWFfb09BX3hnYmFDVWFXSlRnb3Z4WjU4R212TnN4eUlQRElLSm9WV1h5X0R6OSJ9LCJhcHUiOiIiLCJhcHYiOiIifQ..jDbzdI7d67_cUjGD.01BPnMq_tQ.aG_uFA6FYqoPS64QAJ4VBQ", // json payload doesn't contain "key"
			"eyJhbGciOiJFQ0RILUVTIiwiZW5jIjoiQTI1NkdDTSIsImVwayI6eyJrdHkiOiJFQyIsImNydiI6IlAtMzg0Iiwia2V5X29wcyI6W10sImV4dCI6dHJ1ZSwieCI6IkJyYm9UQkl5Y0NDUEdJQlBUekU2RjBnbTRzRjRCamZPN1I0a2x0aWlCaThKZkxxcVdXNVdUSVBLN01yMXV5QVUiLCJ5IjoiNUpGVUI0WVJiYjM2RUZpN2Y0TUxMcFFyZXd2UV9Tc3dKNHRVbFd1a2c1ZU04X1ZyM2pkeml2QXI2WThRczVYbSJ9LCJhcHUiOiIiLCJhcHYiOiIifQ..QEq4Z2m6iwBx2ioS.IBo8TbKJTS4pug.61Z-agIIXgP8bX10O_yEMA", // json payload field "key" not a string
			"eyJhbGciOiJFQ0RILUVTIiwiZW5jIjoiQTI1NkdDTSIsImVwayI6eyJrdHkiOiJFQyIsImNydiI6IlAtMzg0Iiwia2V5X29wcyI6W10sImV4dCI6dHJ1ZSwieCI6ImNZdlVFZm9LYkJjenZySE5zQjUxOGpycUxPMGJDOW5lZjR4NzFFMUQ5dk95MXRqd1piZzV3cFI0OE5nU1RQdHgiLCJ5IjoiaWRJekhCWERzSzR2NTZEeU9yczJOcDZsSG1zb29fMXV0VTlzX3JNdVVkbkxuVXIzUXdLZkhYMWdaVXREM1RKayJ9LCJhcHUiOiIiLCJhcHYiOiIifQ..0VZqu5ei9U3blGtq.eDvhU6drw7mIwvXu6Q.f05QnhI7JWG3IYHvexwdFQ" // json payload field "key" invalid base64 data
	})
	public void testDecryptInvalidVaultKey(String malformed) throws ParseException, InvalidKeySpecException {
		var jwe = JWEObject.parse(malformed);
		var privateKey = P384KeyPair.create(new X509EncodedKeySpec(Base64.getDecoder().decode(PUB_KEY)), new PKCS8EncodedKeySpec(Base64.getDecoder().decode(PRIV_KEY))).getPrivate();

		Assertions.assertThrows(MasterkeyLoadingFailedException.class, () -> JWEHelper.decryptVaultKey(jwe, privateKey));
	}

}
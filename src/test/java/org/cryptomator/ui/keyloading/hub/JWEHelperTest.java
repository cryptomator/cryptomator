package org.cryptomator.ui.keyloading.hub;

import com.nimbusds.jose.JWEObject;
import org.cryptomator.cryptolib.api.MasterkeyLoadingFailedException;
import org.cryptomator.cryptolib.common.P384KeyPair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Base64;

public class JWEHelperTest {

	private static final String JWE = "eyJhbGciOiJFQ0RILUVTIiwiZW5jIjoiQTI1NkdDTSIsImVwayI6eyJrdHkiOiJFQyIsImNydiI6IlAtMzg0Iiwia2V5X29wcyI6W10sImV4dCI6dHJ1ZSwieCI6IllUcEY3bGtTc3JvZVVUVFdCb21LNzBTN0FhVTJyc0ptMURpZ1ZzbjRMY2F5eUxFNFBabldkYmFVcE9jQVV5a1ciLCJ5IjoiLU5pS3loUktjSk52Nm02Z0ZJUWc4cy1Xd1VXUW9uT3A5dkQ4cHpoa2tUU3U2RzFlU2FUTVlhZGltQ2Q4V0ExMSJ9LCJhcHUiOiIiLCJhcHYiOiIifQ..BECWGzd9UvhHcTJC.znt4TlS-qiNEjxiu2v-du_E1QOBnyBR6LCt865SHxD-kwRc1JwX_Lq9XVoFj2GnK9-9CgxhCLGurg5Jt9g38qv2brGAzWL7eSVeY1fIqdO_kUhLpGslRTN6h2U0NHJi2-iE.WDVI2kOk9Dy3PWHyIg8gKA";
	private static final String PRIV_KEY = "ME8CAQAwEAYHKoZIzj0CAQYFK4EEACIEODA2AgEBBDEA6QybmBitf94veD5aCLr7nlkF5EZpaXHCfq1AXm57AKQyGOjTDAF9EQB28fMywTDQ";
	private static final String PUB_KEY = "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAERxQR+NRN6Wga01370uBBzr2NHDbKIC56tPUEq2HX64RhITGhii8Zzbkb1HnRmdF0aq6uqmUy4jUhuxnKxsv59A6JeK7Unn+mpmm3pQAygjoGc9wrvoH4HWJSQYUlsXDu";

	@Test
	public void testDecrypt() throws ParseException, InvalidKeySpecException {
		var jwe = JWEObject.parse(JWE);
		var keyPair = P384KeyPair.create(new X509EncodedKeySpec(Base64.getDecoder().decode(PUB_KEY)), new PKCS8EncodedKeySpec(Base64.getDecoder().decode(PRIV_KEY)));

		var masterkey = JWEHelper.decrypt(jwe, keyPair.getPrivate());

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
	public void testDecryptInvalid(String malformed) throws ParseException, InvalidKeySpecException {
		var jwe = JWEObject.parse(malformed);
		var keyPair = P384KeyPair.create(new X509EncodedKeySpec(Base64.getDecoder().decode(PUB_KEY)), new PKCS8EncodedKeySpec(Base64.getDecoder().decode(PRIV_KEY)));

		Assertions.assertThrows(MasterkeyLoadingFailedException.class, () -> {
			JWEHelper.decrypt(jwe, keyPair.getPrivate());
		});
	}

}
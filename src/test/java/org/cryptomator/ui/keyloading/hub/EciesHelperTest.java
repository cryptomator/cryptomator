package org.cryptomator.ui.keyloading.hub;

import com.google.common.io.BaseEncoding;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ArgumentConverter;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

public class EciesHelperTest {

	@DisplayName("ECDH + KDF")
	@ParameterizedTest
	@ValueSource(ints = {16, 32, 44, 128})
	public void testEcdhAndKdf(int len) throws NoSuchAlgorithmException {
		var alice = KeyPairGenerator.getInstance("EC").generateKeyPair();
		var bob = KeyPairGenerator.getInstance("EC").generateKeyPair();

		byte[] result1 = EciesHelper.ecdhAndKdf(alice.getPrivate(), bob.getPublic(), len);
		byte[] result2 = EciesHelper.ecdhAndKdf(bob.getPrivate(), alice.getPublic(), len);

		Assertions.assertArrayEquals(result1, result2);
	}

	@DisplayName("ANSI-X9.63-KDF")
	@ParameterizedTest
	@CsvSource(value = {
			"96c05619d56c328ab95fe84b18264b08725b85e33fd34f08, , 16, 443024c3dae66b95e6f5670601558f71",
			"96f600b73ad6ac5629577eced51743dd2c24c21b1ac83ee4, , 16, b6295162a7804f5667ba9070f82fa522",
			"22518b10e70f2a3f243810ae3254139efbee04aa57c7af7d, 75eef81aa3041e33b80971203d2c0c52, 128, c498af77161cc59f2962b9a713e2b215152d139766ce34a776df11866a69bf2e52a13d9c7c6fc878c50c5ea0bc7b00e0da2447cfd874f6cf92f30d0097111485500c90c3af8b487872d04685d14c8d1dc8d7fa08beb0ce0ababc11f0bd496269142d43525a78e5bc79a17f59676a5706dc54d54d4d1f0bd7e386128ec26afc21",
			"7e335afa4b31d772c0635c7b0e06f26fcd781df947d2990a, d65a4812733f8cdbcdfb4b2f4c191d87, 128, c0bd9e38a8f9de14c2acd35b2f3410c6988cf02400543631e0d6a4c1d030365acbf398115e51aaddebdc9590664210f9aa9fed770d4c57edeafa0b8c14f93300865251218c262d63dadc47dfa0e0284826793985137e0a544ec80abf2fdf5ab90bdaea66204012efe34971dc431d625cd9a329b8217cc8fd0d9f02b13f2f6b0b",
	})
	// test vectors from https://csrc.nist.gov/CSRC/media/Projects/Cryptographic-Algorithm-Validation-Program/documents/components/800-135testvectors/ansx963_2001.zip
	public void testKdf(@ConvertWith(HexConverter.class) byte[] sharedSecret, @ConvertWith(HexConverter.class) byte[] sharedInfo, int outLen, @ConvertWith(HexConverter.class) byte[] expectedResult) {
		byte[] result = EciesHelper.kdf(sharedSecret, sharedInfo, outLen);
		Assertions.assertArrayEquals(expectedResult, result);
	}

	public static class HexConverter implements ArgumentConverter {

		@Override
		public byte[] convert(Object source, ParameterContext context) throws ArgumentConversionException {
			if (source == null) {
				return new byte[0];
			} else if (source instanceof String s) {
				return BaseEncoding.base16().lowerCase().decode(s);
			} else {
				return null;
			}
		}
	}

}
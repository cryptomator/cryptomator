package org.cryptomator.ui.recoverykey;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WordEncoderTest {

	private static final Random PRNG = new Random(42l);
	private WordEncoder encoder;

	@BeforeAll
	public void setup() {
		encoder = new WordEncoder();
	}

	@DisplayName("decode(encode(input)) == input")
	@ParameterizedTest(name = "test {index}")
	@MethodSource("createRandomByteSequences")
	public void encodeAndDecode(byte[] input) {
		String encoded = encoder.encodePadded(input);
		byte[] decoded = encoder.decode(encoded);
		Assertions.assertArrayEquals(input, decoded);
	}

	public static Stream<byte[]> createRandomByteSequences() {
		return IntStream.range(0, 30).mapToObj(i -> {
			byte[] randomBytes = new byte[i * 3];
			PRNG.nextBytes(randomBytes);
			return randomBytes;
		});
	}

}
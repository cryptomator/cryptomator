package org.cryptomator.ui.recoverykey;

import java.util.Random;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WordEncoderTest {

    private static final Random PRNG = new Random(42L);
    private WordEncoder encoder;

    @BeforeAll
    public void setup() {
        encoder = new WordEncoder();
    }

    @DisplayName("decode(encode(input)) == input")
    @ParameterizedTest(name = "test {index}")
    @MethodSource("createRandomByteSequences")
    public void encodeAndDecode(byte[] input) {
        byte[] paddedInput = padToMultipleOfThree(input);
        String encoded = encoder.encodePadded(paddedInput);
        byte[] decoded = encoder.decode(encoded);
        Assertions.assertArrayEquals(paddedInput, decoded);
    }

    /** Partition Testing: Different Byte Arrays */
    public static Stream<byte[]> createRandomByteSequences() {
        return Stream.of(
            new byte[]{},                     // Empty byte array
            new byte[]{0},                    // Single zero byte
            new byte[]{(byte) 255},           // Single max value byte
            new byte[]{(byte) 0, (byte) 1},   // Two small values
            new byte[]{(byte) 127, (byte) 128}, // Edge of printable/non-printable ASCII
            new byte[]{(byte) 0, (byte) 255, (byte) 127}, // Mixed bytes
            generateRandomBytes(10),          // Medium-sized random array
            generateRandomBytes(100)          // Large byte array
        );
    }

    private static byte[] generateRandomBytes(int size) {
        byte[] randomBytes = new byte[size];
        PRNG.nextBytes(randomBytes);
        return randomBytes;
    }

    @Test
    @DisplayName("Encode empty array")
    public void encodeEmptyArray() {
        byte[] input = new byte[0];
        String encoded = encoder.encodePadded(input);
        Assertions.assertTrue(encoded.isEmpty(), "Encoding empty input should return an empty string");
    }

    @Test
    @DisplayName("Decode empty string")
    public void decodeEmptyString() {
        byte[] decoded = encoder.decode("");
        Assertions.assertEquals(0, decoded.length, "Decoding an empty string should return an empty byte array");
    }

    @Test
    @DisplayName("Encoding and decoding max-value bytes")
    public void encodeDecodeMaxValueBytes() {
        byte[] input = new byte[]{(byte) 255, (byte) 255, (byte) 255};
        String encoded = encoder.encodePadded(input);
        byte[] decoded = encoder.decode(encoded);
        Assertions.assertArrayEquals(input, decoded, "Max-value bytes should encode and decode correctly");
    }

    @Test
    @DisplayName("Handling corrupted encoded data")
    public void decodeCorruptData() {
        String corruptEncoded = "!!!invalid!!!";
        Assertions.assertThrows(IllegalArgumentException.class, () -> encoder.decode(corruptEncoded), "Decoding corrupted data should throw an exception");
    }

    /**
     * Pads the input byte array to a length that is a multiple of three.
     *
     * @param input The input byte array.
     * @return A new byte array padded to a multiple of three.
     */
    private byte[] padToMultipleOfThree(byte[] input) {
        int remainder = input.length % 3;
        if (remainder == 0) {
            return input;
        }
        int paddingLength = 3 - remainder;
        byte[] paddedInput = new byte[input.length + paddingLength];
        System.arraycopy(input, 0, paddedInput, 0, input.length);
        return paddedInput;
    }
}
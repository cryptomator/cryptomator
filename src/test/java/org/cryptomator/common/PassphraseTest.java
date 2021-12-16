package org.cryptomator.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class PassphraseTest {

	@ParameterizedTest
	@CsvSource(value = {
			"-1, 0",
			"0, -1",
			"0, 10",
			"10, 0",
			"10, 10"
	})
	public void testInvalidConstructorArgs(int offset, int length) {
		char[] data = "test".toCharArray();
		Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
			new Passphrase(data, offset, length);
		});
	}

	@ParameterizedTest
	@CsvSource(value = {
			"0, 4",
			"0, 0",
			"0, 1",
			"1, 1",
			"2, 2"
	})
	public void testValidConstructorArgs(int offset, int length) {
		char[] data = "test".toCharArray();
		var pw = new Passphrase(data, offset, length);
		Assertions.assertEquals(length, pw.length());
		Assertions.assertEquals("test".substring(offset, offset + length), pw.toString());
	}

	@Nested
	public class InstanceMethods {

		private Passphrase pw1;
		private Passphrase pw2;

		@BeforeEach
		public void setup() {
			char[] foo = "test test".toCharArray();
			pw1 = new Passphrase(foo, 5, 4);
			pw2 = Passphrase.copyOf("test");
		}

		@Test
		public void testToString() {
			Assertions.assertEquals("test", pw1.toString());
			Assertions.assertEquals("test", pw2.toString());
		}

		@Test
		public void testEquals() {
			Assertions.assertEquals(pw1, pw2);
		}

		@Test
		public void testHashcode() {
			Assertions.assertEquals(pw1.hashCode(), pw2.hashCode());
		}

		@Test
		public void testLength() {
			Assertions.assertEquals(4, pw1.length());
			Assertions.assertEquals(4, pw2.length());
		}

		@Test
		public void testCharAt() {
			Assertions.assertEquals('s', pw1.charAt(2));
		}

		@ParameterizedTest
		@ValueSource(ints = {-1, 4, 5})
		public void testInvalidCharAt(int idx) {
			Assertions.assertThrows(IndexOutOfBoundsException.class, () -> pw1.charAt(idx));
		}

		@ParameterizedTest
		@ValueSource(ints = {0, 1, 2, 3})
		public void testValidCharAt(int idx) {
			Assertions.assertEquals("test".charAt(idx), pw1.charAt(idx));
		}

		@ParameterizedTest
		@CsvSource(value = {
				"-1, 0",
				"0, -1",
				"-1, -1",
				"0, 5",
				"3, 2"
		})
		public void testInvalidSubSequence(int start, int end) {
			Assertions.assertThrows(IndexOutOfBoundsException.class, () -> pw1.subSequence(start, end));
		}

		@ParameterizedTest
		@CsvSource(value = {
				"0, 4",
				"1, 4",
				"0, 2",
				"2, 4",
				"4, 4",
		})
		public void testValidSubSequence(int start, int end) {
			Assertions.assertEquals("test".substring(start, end), pw1.subSequence(start, end).toString());
		}

		@Test
		public void testDestroy() {
			pw2.destroy();
			Assertions.assertFalse(pw1.isDestroyed());
			Assertions.assertTrue(pw2.isDestroyed());
			Assertions.assertNotEquals(pw1, pw2);
		}

	}

}
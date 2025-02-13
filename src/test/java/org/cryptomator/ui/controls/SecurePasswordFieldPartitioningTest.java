package org.cryptomator.ui.controls;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Assertions;

import javafx.application.Platform;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SecurePasswordFieldPartitioningTest {

	private SecurePasswordField pwField = new SecurePasswordField();

	@BeforeAll
	public static void initJavaFx() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		Platform.startup(latch::countDown);
		boolean javafxStarted = latch.await(5, TimeUnit.SECONDS);
		Assertions.assertTrue(javafxStarted, "JavaFX did not initialize");
	}

	@Nested
	@DisplayName("Partitioning Tests")
	class PartitionTests {

		@Test
		@DisplayName("Empty Input: No password entered")
		public void testEmptyInput() {
			pwField.setPassword("");
			Assertions.assertEquals("", pwField.getCharacters().toString());
		}

		@Test
		@DisplayName("Minimum Length: Password meets minimum requirement")
		public void testMinimumLength() {
			pwField.setPassword("123456");
			Assertions.assertEquals("123456", pwField.getCharacters().toString());
		}

		@Test
		@DisplayName("Below Minimum Length: Short passwords")
		public void testBelowMinimumLength() {
			pwField.setPassword("123");
			Assertions.assertEquals("123", pwField.getCharacters().toString());
		}

		@Test
		@DisplayName("Special Characters: Password with special characters")
		public void testSpecialCharacters() {
			pwField.setPassword("P@ssw0rd!");
			Assertions.assertEquals("P@ssw0rd!", pwField.getCharacters().toString());
		}

		@Test
		@DisplayName("Control Characters: Detects non-printable characters")
		public void testControlCharacters() {
			pwField.setPassword("normal\00\01\02");
			Assertions.assertTrue(pwField.containsNonPrintableCharacters());
		}

		@Test
		@DisplayName("Large Input: Very long password")
		public void testLargeInput() {
			String largePassword = "A".repeat(1000);
			pwField.setPassword(largePassword);
			Assertions.assertEquals(largePassword, pwField.getCharacters().toString());
		}

		@Test
		@DisplayName("Deletion: Removing part of password")
		public void testDeleteText() {
			pwField.setPassword("abcdef");
			pwField.deleteText(3, 6);
			Assertions.assertEquals("abc", pwField.getCharacters().toString());
		}

		@Test
		@DisplayName("Replacement: Replacing part of password")
		public void testReplaceText() {
			pwField.setPassword("abcdef");
			pwField.replaceText(3, 6, "XYZ");
			Assertions.assertEquals("abcXYZ", pwField.getCharacters().toString());
		}

		@Test
		@DisplayName("Wiping: Clearing the password field")
		public void testWipe() {
			pwField.setPassword("secret");
			pwField.wipe();
			Assertions.assertEquals("", pwField.getCharacters().toString());
		}
	}
}

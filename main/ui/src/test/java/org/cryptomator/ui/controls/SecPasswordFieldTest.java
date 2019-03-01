package org.cryptomator.ui.controls;

import javafx.application.Platform;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class SecPasswordFieldTest {

	private SecPasswordField pwField = new SecPasswordField();

	@BeforeAll
	static void initJavaFx() throws InterruptedException {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
		final CountDownLatch latch = new CountDownLatch(1);
		Platform.startup(latch::countDown);

		if (!latch.await(5L, TimeUnit.SECONDS)) {
			throw new ExceptionInInitializerError();
		}
	}

	@Nested
	@DisplayName("Content Update Events")
	class TextChange {

		@Test
		@DisplayName("\"ant\".append(\"eater\")")
		public void append() {
			pwField.setPassword("ant");
			pwField.appendText("eater");

			Assertions.assertEquals("anteater", pwField.getCharacters().toString());
		}

		@Test
		@DisplayName("\"eater\".insert(0, \"ant\")")
		public void insert1() {
			pwField.setPassword("eater");
			pwField.insertText(0, "ant");

			Assertions.assertEquals("anteater", pwField.getCharacters().toString());
		}

		@Test
		@DisplayName("\"anteater\".insert(3, \"b\")")
		public void insert2() {
			pwField.setPassword("anteater");
			pwField.insertText(3, "b");

			Assertions.assertEquals("antbeater", pwField.getCharacters().toString());
		}

		@Test
		@DisplayName("\"anteater\".delete(0, 3)")
		public void delete1() {
			pwField.setPassword("anteater");
			pwField.deleteText(0, 3);

			Assertions.assertEquals("eater", pwField.getCharacters().toString());
		}

		@Test
		@DisplayName("\"anteater\".delete(3, 8)")
		public void delete2() {
			pwField.setPassword("anteater");
			pwField.deleteText(3, 8);

			Assertions.assertEquals("ant", pwField.getCharacters().toString());
		}

		@Test
		@DisplayName("\"anteater\".replace(0, 3, \"hand\")")
		public void replace1() {
			pwField.setPassword("anteater");
			pwField.replaceText(0, 3, "hand");

			Assertions.assertEquals("handeater", pwField.getCharacters().toString());
		}

		@Test
		@DisplayName("\"anteater\".replace(3, 6, \"keep\")")
		public void replace2() {
			pwField.setPassword("anteater");
			pwField.replaceText(3, 6, "keep");

			Assertions.assertEquals("antkeeper", pwField.getCharacters().toString());
		}

		@Test
		@DisplayName("\"anteater\".replace(0, 3, \"bee\")")
		public void replace3() {
			pwField.setPassword("anteater");
			pwField.replaceText(0, 3, "bee");

			Assertions.assertEquals("beeeater", pwField.getCharacters().toString());
		}

	}

	@Test
	@DisplayName("entering NFC string leads to NFC char[]")
	public void enterNfcString() {
		pwField.appendText("str\u00F6m"); // ström
		pwField.insertText(0, "\u212Bng"); // Ång
		pwField.appendText("\uD83D\uDCA9"); // 💩

		CharSequence result = pwField.getCharacters();
		Assertions.assertEquals('\u00C5', result.charAt(0));
		Assertions.assertEquals('n', result.charAt(1));
		Assertions.assertEquals('g', result.charAt(2));
		Assertions.assertEquals('s', result.charAt(3));
		Assertions.assertEquals('t', result.charAt(4));
		Assertions.assertEquals('r', result.charAt(5));
		Assertions.assertEquals('ö', result.charAt(6));
		Assertions.assertEquals('m', result.charAt(7));
		Assertions.assertEquals('\uD83D', result.charAt(8));
		Assertions.assertEquals('\uDCA9', result.charAt(9));
	}

	@Test
	@DisplayName("entering NFD string leads to NFC char[]")
	public void enterNfdString() {
		pwField.appendText("str\u006F\u0308m"); // ström
		pwField.insertText(0, "\u0041\u030Ang"); // Ång
		pwField.appendText("\uD83D\uDCA9"); // 💩

		CharSequence result = pwField.getCharacters();
		Assertions.assertEquals('\u00C5', result.charAt(0));
		Assertions.assertEquals('n', result.charAt(1));
		Assertions.assertEquals('g', result.charAt(2));
		Assertions.assertEquals('s', result.charAt(3));
		Assertions.assertEquals('t', result.charAt(4));
		Assertions.assertEquals('r', result.charAt(5));
		Assertions.assertEquals('ö', result.charAt(6));
		Assertions.assertEquals('m', result.charAt(7));
		Assertions.assertEquals('\uD83D', result.charAt(8));
		Assertions.assertEquals('\uDCA9', result.charAt(9));
	}

	@Test
	@DisplayName("test swipe char[]")
	public void testSwipe() {
		pwField.appendText("topSecret");

		CharSequence result1 = pwField.getCharacters();
		Assertions.assertEquals("topSecret", result1.toString());
		pwField.swipe();
		CharSequence result2 = pwField.getCharacters();
		Assertions.assertEquals("         ", result1.toString());
		Assertions.assertEquals("", result2.toString());
	}

	@Test
	@DisplayName("test control characters")
	public void testControlCharacters() {
		pwField.appendText("normal");
		Assertions.assertFalse(pwField.containsNonPrintableCharacters());

		pwField.appendText("\00\01\02");
		Assertions.assertTrue(pwField.containsNonPrintableCharacters());

		CharSequence result1 = pwField.getCharacters();
		Assertions.assertEquals("normal\00\01\02", result1.toString());
	}

}

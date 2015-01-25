package org.cryptomator.ui.model;

import static org.junit.Assert.*;

import org.cryptomator.ui.model.Directory;
import org.junit.Test;

public class DirectoryTest {
	@Test
	public void testNormalize() throws Exception {
		assertEquals("_", Directory.normalize(" "));

		assertEquals("a", Directory.normalize("ä"));

		assertEquals("C", Directory.normalize("Ĉ"));

		assertEquals("_", Directory.normalize(":"));

		assertEquals("", Directory.normalize("汉语"));
	}
}

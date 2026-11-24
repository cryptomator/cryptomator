package org.cryptomator.event;

import org.cryptomator.cryptofs.event.FileWriteConflictEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

class EventFileNameTest {

	@Test
	public void testKnownCleartextPathShowsCleartextName() {
		var event = new FileWriteConflictEvent("/docs/report.txt", Path.of("/vault/d/AB/CDEF/abc123==.c9r"));

		Assertions.assertEquals("report.txt", EventFileName.of(event));
	}

	@Test
	public void testUnknownCleartextPathShowsEncryptedName() {
		var event = new FileWriteConflictEvent(null, Path.of("/vault/d/AB/CDEF/abc123==.c9r"));

		Assertions.assertEquals("abc123==.c9r", EventFileName.of(event));
	}

	@Test
	public void testUnknownCleartextPathOfShortenedNameShowsShortenedNodeName() {
		var event = new FileWriteConflictEvent(null, Path.of("/vault/d/AB/CDEF/hash123==.c9s/contents.c9r"));

		Assertions.assertEquals("hash123==.c9s", EventFileName.of(event));
	}

	@Test
	public void testSymlinkFileShowsItsNodeName() {
		Assertions.assertEquals("abc123==.c9r", EventFileName.ofCiphertext(Path.of("/vault/d/AB/CDEF/abc123==.c9r/symlink.c9r")));
		Assertions.assertEquals("hash123==.c9s", EventFileName.ofCiphertext(Path.of("/vault/d/AB/CDEF/hash123==.c9s/symlink.c9r")));
	}

}

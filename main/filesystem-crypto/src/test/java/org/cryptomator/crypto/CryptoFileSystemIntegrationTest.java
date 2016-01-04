package org.cryptomator.crypto;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.filesystem.inmem.InMemoryFileSystem;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CryptoFileSystemIntegrationTest {

	private FileSystem ciphertextFs;
	private FileSystem cleartextFs;

	@Before
	public void setupFileSystems() {
		ciphertextFs = new InMemoryFileSystem();
		// final Predicate<Node> isMetadataFolder = (Node node) -> node.equals(physicalFs.folder("m"));
		// final FileSystem metadataHidingFs = new BlacklistingFileSystem(physicalFs, isMetadataFolder);
		// final FileSystem shorteningFs = new ShorteningFileSystem(metadataHidingFs, physicalFs.folder("m"), 70);
		cleartextFs = DaggerCryptoTestComponent.create().cryptoFileSystemFactory().get(ciphertextFs, "TopSecret");
		cleartextFs.create();
	}

	@Test
	public void testRandomAccess() {
		File cleartextFile = cleartextFs.file("test");
		try (WritableFile writable = cleartextFile.openWritable()) {
			ByteBuffer buf = ByteBuffer.allocate(25000);
			for (int i = 0; i < 40; i++) { // 40 * 25k = 1M
				buf.clear();
				Arrays.fill(buf.array(), (byte) i);
				writable.write(buf);
			}
		}

		Folder ciphertextRootFolder = ciphertextFs.folder("d").folders().findAny().get().folders().findAny().get();
		Assert.assertTrue(ciphertextRootFolder.exists());
		File ciphertextFile = ciphertextRootFolder.files().findAny().get();
		Assert.assertTrue(ciphertextFile.exists());

		try (ReadableFile readable = cleartextFile.openReadable()) {
			ByteBuffer buf = ByteBuffer.allocate(1);
			for (int i = 0; i < 40; i++) {
				buf.clear();
				readable.position(i * 25000 + (long) Math.random() * 24999);
				readable.read(buf);
				buf.flip();
				Assert.assertEquals(i, buf.get());
			}
		}
	}

}

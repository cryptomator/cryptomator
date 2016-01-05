package org.cryptomator.filesystem.crypto;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.cryptomator.crypto.engine.impl.CryptoEngineTestModule;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.Node;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.filesystem.inmem.InMemoryFileSystem;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CryptoFileSystemComponentIntegrationTest {

	private static final CryptoFileSystemComponent cryptoFsComp = DaggerCryptoFileSystemComponent.builder().cryptoEngineModule(new CryptoEngineTestModule()).build();

	private FileSystem ciphertextFs;
	private FileSystem cleartextFs;

	@Before
	public void setupFileSystems() {
		ciphertextFs = new InMemoryFileSystem();
		cleartextFs = cryptoFsComp.cryptoFileSystemFactory().get(ciphertextFs, "TopSecret");
		cleartextFs.create();
	}

	@Test
	public void testEncryptionOfLongFolderNames() {
		final String shortName = "normal folder name";
		final String longName = "this will be a long filename after encryption, because its encrypted name is longer than onehundredandeighty characters";

		final Folder shortFolder = cleartextFs.folder(shortName);
		final Folder longFolder = cleartextFs.folder(longName);

		shortFolder.create();
		longFolder.create();

		// because of the long file, a metadata folder should exist on the physical layer:
		Assert.assertEquals(1, ciphertextFs.folder("m").folders().count());
		Assert.assertTrue(ciphertextFs.folder("m").exists());

		// but the shortened filenames must not be visible on the cleartext layer:
		Assert.assertArrayEquals(new String[] {shortName, longName}, cleartextFs.folders().map(Node::name).sorted().toArray());
	}

	@Test
	public void testEncryptionAndDecryptionOfFiles() {
		// write test content to encrypted file
		try (WritableFile writable = cleartextFs.file("test1.txt").openWritable()) {
			writable.write(ByteBuffer.wrap("Hello ".getBytes()));
			writable.write(ByteBuffer.wrap("World".getBytes()));
		}

		File physicalFile = ciphertextFs.folder("d").folders().findAny().get().folders().findAny().get().files().findAny().get();
		Assert.assertTrue(physicalFile.exists());

		// read test content from decrypted file
		try (ReadableFile readable = cleartextFs.file("test1.txt").openReadable()) {
			ByteBuffer buf1 = ByteBuffer.allocate(5);
			readable.read(buf1);
			buf1.flip();
			Assert.assertEquals("Hello", new String(buf1.array(), 0, buf1.remaining()));
			ByteBuffer buf2 = ByteBuffer.allocate(10);
			readable.read(buf2);
			buf2.flip();
			Assert.assertArrayEquals(" World".getBytes(), Arrays.copyOfRange(buf2.array(), 0, buf2.remaining()));
		}
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
				readable.position(i * 25000 + (long) Math.random() * 24999); // "random access", told you so.
				readable.read(buf);
				buf.flip();
				Assert.assertEquals(i, buf.get());
			}
		}
	}

}

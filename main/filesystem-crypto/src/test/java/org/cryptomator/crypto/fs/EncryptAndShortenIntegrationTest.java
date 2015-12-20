package org.cryptomator.crypto.fs;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.cryptomator.crypto.engine.Cryptor;
import org.cryptomator.crypto.engine.impl.TestCryptorImplFactory;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.FolderCreateMode;
import org.cryptomator.filesystem.Node;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.filesystem.inmem.InMemoryFileSystem;
import org.cryptomator.shortening.ShorteningFileSystem;
import org.junit.Assert;
import org.junit.Test;

public class EncryptAndShortenIntegrationTest {

	// private static final Logger LOG = LoggerFactory.getLogger(EncryptAndShortenIntegrationTest.class);

	@Test
	public void testEncryptionOfLongFolderNames() {
		final FileSystem physicalFs = new InMemoryFileSystem();
		final FileSystem shorteningFs = new ShorteningFileSystem(physicalFs, physicalFs.folder("m"), 70);
		final Cryptor cryptor = TestCryptorImplFactory.insecureCryptorImpl();
		cryptor.randomizeMasterkey();
		final FileSystem fs = new CryptoFileSystem(shorteningFs, cryptor, "foo");
		fs.create(FolderCreateMode.FAIL_IF_PARENT_IS_MISSING);
		final Folder shortFolder = fs.folder("normal folder name");
		shortFolder.create(FolderCreateMode.FAIL_IF_PARENT_IS_MISSING);
		final Folder longFolder = fs.folder("this will be a long filename after encryption");
		longFolder.create(FolderCreateMode.FAIL_IF_PARENT_IS_MISSING);

		// the long name will produce a metadata file on the physical layer:
		// LOG.debug("Physical file system:\n" + DirectoryPrinter.print(physicalFs));
		Assert.assertEquals(1, physicalFs.folder("m").folders().count());

		// on the second layer all .lng files are resolved to their actual names:
		// LOG.debug("Unlimited filename length:\n" + DirectoryPrinter.print(shorteningFs));
		DirectoryWalker.walk(shorteningFs, node -> {
			Assert.assertFalse(node.name().endsWith(".lng"));
		});

		// on the third (cleartext layer) we have cleartext names on the root level:
		// LOG.debug("Cleartext files:\n" + DirectoryPrinter.print(fs));
		Assert.assertArrayEquals(new String[] {"normal folder name", "this will be a long filename after encryption"}, fs.folders().map(Node::name).sorted().toArray());
	}

	@Test
	public void testEncryptionAndDecryptionOfFiles() {
		final FileSystem physicalFs = new InMemoryFileSystem();
		final FileSystem shorteningFs = new ShorteningFileSystem(physicalFs, physicalFs.folder("m"), 70);
		final Cryptor cryptor = TestCryptorImplFactory.insecureCryptorImpl();
		cryptor.randomizeMasterkey();
		final FileSystem fs = new CryptoFileSystem(shorteningFs, cryptor, "foo");
		fs.create(FolderCreateMode.FAIL_IF_PARENT_IS_MISSING);

		// write test content to encrypted file
		try (WritableFile writable = fs.file("test1.txt").openWritable()) {
			writable.write(ByteBuffer.wrap("Hello ".getBytes()));
			writable.write(ByteBuffer.wrap("World".getBytes()));
		}

		File physicalFile = physicalFs.folder("d").folders().findAny().get().folders().findAny().get().files().findAny().get();
		Assert.assertTrue(physicalFile.exists());

		// read test content from decrypted file
		try (ReadableFile readable = fs.file("test1.txt").openReadable()) {
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

}

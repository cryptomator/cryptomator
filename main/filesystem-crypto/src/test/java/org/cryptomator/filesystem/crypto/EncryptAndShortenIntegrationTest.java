/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.crypto;

import static org.cryptomator.filesystem.FileSystemVisitor.fileSystemVisitor;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.Predicate;

import org.cryptomator.crypto.engine.Cryptor;
import org.cryptomator.crypto.engine.impl.TestCryptorImplFactory;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.Node;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.filesystem.blacklisting.BlacklistingFileSystem;
import org.cryptomator.filesystem.inmem.InMemoryFileSystem;
import org.cryptomator.filesystem.shortening.ShorteningFileSystem;
import org.junit.Assert;
import org.junit.Test;

public class EncryptAndShortenIntegrationTest {

	// private static final Logger LOG = LoggerFactory.getLogger(EncryptAndShortenIntegrationTest.class);

	@Test
	public void testEncryptionOfLongFolderNames() {
		final FileSystem physicalFs = new InMemoryFileSystem();
		final Predicate<Node> isMetadataFolder = (Node node) -> node.equals(physicalFs.folder("m"));
		final FileSystem metadataHidingFs = new BlacklistingFileSystem(physicalFs, isMetadataFolder);
		final FileSystem shorteningFs = new ShorteningFileSystem(metadataHidingFs, physicalFs.folder("m"), 70);

		final Cryptor cryptor = TestCryptorImplFactory.insecureCryptorImpl();
		cryptor.randomizeMasterkey();
		final FileSystem fs = new CryptoFileSystem(shorteningFs, cryptor, "foo");
		fs.create();
		final Folder shortFolder = fs.folder("normal folder name");
		shortFolder.create();
		final Folder longFolder = fs.folder("this will be a long filename after encryption");
		longFolder.create();

		// on the first (physical) layer all files including metadata files are visible:
		// the long name will produce a metadata file on the physical layer:
		// LOG.debug("Physical file system:\n" + DirectoryPrinter.print(physicalFs));
		Assert.assertEquals(1, physicalFs.folder("m").folders().count());
		Assert.assertTrue(physicalFs.folder("m").exists());

		// on the second (blacklisting) layer we hide the metadata folder:
		// LOG.debug("Filtered files:\n" + DirectoryPrinter.print(metadataHidingFs));
		Assert.assertEquals(1, metadataHidingFs.folders().count()); // only "d", no "m".

		// on the third layer all .lng files are resolved to their actual names:
		// LOG.debug("Unlimited filename length:\n" + DirectoryPrinter.print(shorteningFs));
		fileSystemVisitor() //
				.forEachNode(node -> {
					Assert.assertFalse(node.name().endsWith(".lng"));
				}) //
				.visit(shorteningFs);

		// on the fourth (cleartext) layer we have cleartext names on the root level:
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
		fs.create();

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

/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.crypto;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;

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
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CryptoFileSystemIntegrationTest {

	private static final Logger LOG = LoggerFactory.getLogger(CryptoFileSystemIntegrationTest.class);

	private final CryptoFileSystemTestComponent cryptoFsComp = DaggerCryptoFileSystemTestComponent.builder().cryptoEngineModule(new CryptoEngineTestModule()).build();

	private CryptoFileSystemDelegate cryptoDelegate;
	private FileSystem ciphertextFs;
	private FileSystem cleartextFs;

	@Before
	public void setupFileSystems() {
		cryptoDelegate = Mockito.mock(CryptoFileSystemDelegate.class);
		ciphertextFs = new InMemoryFileSystem();
		FileSystem shorteningFs = cryptoFsComp.shorteningFileSystemFactory().get(ciphertextFs);
		cryptoFsComp.cryptoFileSystemFactory().initializeNew(shorteningFs, "TopSecret");
		cleartextFs = cryptoFsComp.cryptoFileSystemFactory().unlockExisting(shorteningFs, "TopSecret", cryptoDelegate);
	}

	@Test(timeout = 1000)
	public void testVaultStructureInitializationAndBackupBehaviour() throws UncheckedIOException, IOException {
		final FileSystem physicalFs = new InMemoryFileSystem();
		final File masterkeyFile = physicalFs.file("masterkey.cryptomator");
		final File masterkeyBkupFile = physicalFs.file("masterkey.cryptomator.bkup");
		final Folder physicalDataRoot = physicalFs.folder("d");
		Assert.assertFalse(masterkeyFile.exists());
		Assert.assertFalse(masterkeyBkupFile.exists());
		Assert.assertFalse(physicalDataRoot.exists());

		cryptoFsComp.cryptoFileSystemFactory().initializeNew(physicalFs, "asd");
		Assert.assertTrue(masterkeyFile.exists());
		Assert.assertFalse(masterkeyBkupFile.exists());
		Assert.assertFalse(physicalDataRoot.exists());

		@SuppressWarnings("unused")
		final FileSystem cryptoFs = cryptoFsComp.cryptoFileSystemFactory().unlockExisting(physicalFs, "asd", cryptoDelegate);
		Assert.assertTrue(masterkeyBkupFile.exists());
		Assert.assertTrue(physicalDataRoot.exists());
		Assert.assertEquals(3, physicalFs.children().count()); // d + masterkey.cryptomator + masterkey.cryptomator.bkup
		Assert.assertEquals(1, physicalDataRoot.folders().count()); // ROOT directory
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
	public void testForcedDecryptionOfManipulatedFile() {
		// write test content to encrypted file
		try (WritableFile writable = cleartextFs.file("test1.txt").openWritable()) {
			writable.write(ByteBuffer.wrap("Hello World".getBytes()));
		}

		File physicalFile = ciphertextFs.folder("d").folders().findAny().get().folders().findAny().get().files().findAny().get();
		Assert.assertTrue(physicalFile.exists());

		// toggle last bit
		try (WritableFile writable = physicalFile.openWritable(); ReadableFile readable = physicalFile.openReadable()) {
			ByteBuffer buf = ByteBuffer.allocate((int) readable.size());
			readable.read(buf);
			buf.array()[buf.limit() - 1] ^= 0x01;
			buf.flip();
			writable.write(buf);
		}

		// whitelist
		Mockito.when(cryptoDelegate.shouldSkipAuthentication("/test1.txt")).thenReturn(true);

		// read test content from decrypted file
		try (ReadableFile readable = cleartextFs.file("test1.txt").openReadable()) {
			ByteBuffer buf = ByteBuffer.allocate(11);
			readable.read(buf);
			buf.flip();
			Assert.assertArrayEquals("Hello World".getBytes(), buf.array());
		}
	}

	@Test(timeout = 20000) // assuming a minimum speed of 10mb/s during encryption and decryption 20s should be enough
	public void testEncryptionAndDecryptionSpeed() throws InterruptedException, IOException {
		File file = cleartextFs.file("benchmark.test");

		final long encStart = System.nanoTime();
		try (WritableFile writable = file.openWritable()) {
			final ByteBuffer cleartext = ByteBuffer.allocate(100000); // 100k
			for (int i = 0; i < 1000; i++) { // 100M total
				cleartext.rewind();
				writable.write(cleartext);
			}
		}
		final long encEnd = System.nanoTime();
		LOG.debug("Encryption of 100M took {}ms", (encEnd - encStart) / 1000 / 1000);

		final long decStart = System.nanoTime();
		try (ReadableFile readable = file.openReadable()) {
			final ByteBuffer cleartext = ByteBuffer.allocate(100000); // 100k
			for (int i = 0; i < 1000; i++) { // 100M total
				cleartext.clear();
				readable.read(cleartext);
				cleartext.flip();
				Assert.assertEquals(cleartext.get(), 0x00);
			}
		}
		final long decEnd = System.nanoTime();
		LOG.debug("Decryption of 100M took {}ms", (decEnd - decStart) / 1000 / 1000);

		file.delete();
	}

	@Test
	public void testRandomAccessOnLastBlock() {
		// prepare test data:
		ByteBuffer testData = ByteBuffer.allocate(16000 * Integer.BYTES); // < 64kb
		for (int i = 0; i < 16000; i++) {
			testData.putInt(i);
		}

		// write test data to file:
		File cleartextFile = cleartextFs.file("test");
		try (WritableFile writable = cleartextFile.openWritable()) {
			testData.flip();
			writable.write(testData);
		}

		// read last block:
		try (ReadableFile readable = cleartextFile.openReadable()) {
			ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES);
			buf.clear();
			readable.position(15999 * Integer.BYTES);
			readable.read(buf);
			buf.flip();
			Assert.assertEquals(15999, buf.getInt());
		}
	}

	@Test
	public void testSequentialRandomAccess() {
		// prepare test data:
		ByteBuffer testData = ByteBuffer.allocate(1_000_000 * Integer.BYTES); // = 4MB
		for (int i = 0; i < 1000000; i++) {
			testData.putInt(i);
		}

		// write test data to file:
		File cleartextFile = cleartextFs.file("test");
		try (WritableFile writable = cleartextFile.openWritable()) {
			testData.flip();
			writable.write(testData);
		}

		// shuffle our test positions:
		List<Integer> nums = new ArrayList<>();
		for (int i = 0; i < 1_000_000; i++) {
			nums.add(i);
		}
		Collections.shuffle(nums);

		// read parts from positions:
		try (ReadableFile readable = cleartextFile.openReadable()) {
			ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES);
			for (int i = 0; i < 1000; i++) {
				int num = nums.get(i);
				buf.clear();
				readable.position(num * Integer.BYTES);
				readable.read(buf);
				buf.flip();
				Assert.assertEquals(num, buf.getInt());
			}
		}
	}

	@Test
	public void testParallelRandomAccess() {
		// prepare test data:
		ByteBuffer testData = ByteBuffer.allocate(1_000_000 * Integer.BYTES); // = 4MB
		for (int i = 0; i < 1000000; i++) {
			testData.putInt(i);
		}

		// write test data to file:
		final File cleartextFile = cleartextFs.file("test");
		try (WritableFile writable = cleartextFile.openWritable()) {
			testData.flip();
			writable.write(testData);
		}

		// shuffle our test positions:
		List<Integer> nums = new ArrayList<>();
		for (int i = 0; i < 1_000_000; i++) {
			nums.add(i);
		}
		Collections.shuffle(nums);

		// read parts from positions in parallel:
		final ForkJoinPool pool = new ForkJoinPool(10);
		final List<Future<Boolean>> tasks = new ArrayList<>();
		for (int i = 0; i < 1000; i++) {
			final int num = nums.get(i);
			final ForkJoinTask<Boolean> task = ForkJoinTask.adapt(() -> {
				try (ReadableFile readable = cleartextFile.openReadable()) {
					ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES);
					buf.clear();
					readable.position(num * Integer.BYTES);
					readable.read(buf);
					buf.flip();
					int numRead = buf.getInt();
					return num == numRead;
				}
			});
			pool.execute(task);
			tasks.add(task);
		}

		// Wait for tasks to finish and check results
		Assert.assertTrue(tasks.stream().allMatch(task -> {
			try {
				return task.get();
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}));
	}

}

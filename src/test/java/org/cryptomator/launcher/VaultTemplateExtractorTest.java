package org.cryptomator.launcher;

import org.cryptomator.launcher.VaultTemplateExtractor.MalformedTemplateException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class VaultTemplateExtractorTest {

	private static final byte[] CONFIG = "hub-vault-config".getBytes(StandardCharsets.UTF_8);
	private static final byte[] CIPHERTEXT = "some-ciphertext".getBytes(StandardCharsets.UTF_8);

	@Test
	@DisplayName("a vault at the archive root is moved to the destination")
	public void testVaultAtArchiveRoot(@TempDir Path tmp) throws IOException {
		var zip = zip(Map.of( //
				"vault.cryptomator", CONFIG, //
				"d/AB/CDEF/0.c9r", CIPHERTEXT //
		));
		var destination = tmp.resolve("MyVault");

		var result = VaultTemplateExtractor.extractAndMove(zip, destination);

		Assertions.assertEquals(destination, result);
		Assertions.assertArrayEquals(CONFIG, Files.readAllBytes(destination.resolve("vault.cryptomator")));
		Assertions.assertArrayEquals(CIPHERTEXT, Files.readAllBytes(destination.resolve("d/AB/CDEF/0.c9r")));
	}

	@Test
	@DisplayName("a vault nested in a single top-level folder is moved to the destination")
	public void testVaultInSubfolder(@TempDir Path tmp) throws IOException {
		var zip = zip(Map.of( //
				"TemplateVault/vault.cryptomator", CONFIG, //
				"TemplateVault/d/AB/CDEF/0.c9r", CIPHERTEXT //
		));
		var destination = tmp.resolve("MyVault");

		VaultTemplateExtractor.extractAndMove(zip, destination);

		Assertions.assertArrayEquals(CONFIG, Files.readAllBytes(destination.resolve("vault.cryptomator")));
		Assertions.assertArrayEquals(CIPHERTEXT, Files.readAllBytes(destination.resolve("d/AB/CDEF/0.c9r")));
	}

	@Test
	@DisplayName("an existing destination is not overwritten")
	public void testDestinationExists(@TempDir Path tmp) throws IOException {
		var destination = tmp.resolve("MyVault");
		Files.createDirectory(destination);
		var zip = zip(Map.of("vault.cryptomator", CONFIG));

		Assertions.assertThrows(FileAlreadyExistsException.class, () -> VaultTemplateExtractor.extractAndMove(zip, destination));
	}

	@Test
	@DisplayName("data that is not a zip archive fails as malformed")
	public void testNotAZip(@TempDir Path tmp) {
		var notAZip = "this is not a zip archive".getBytes(StandardCharsets.UTF_8);

		Assertions.assertThrows(MalformedTemplateException.class, () -> VaultTemplateExtractor.extractAndMove(notAZip, tmp.resolve("MyVault")));
	}

	@Test
	@DisplayName("an archive without a vault config fails as malformed")
	public void testNoVaultConfig(@TempDir Path tmp) throws IOException {
		var zip = zip(Map.of("readme.txt", CONFIG));

		Assertions.assertThrows(MalformedTemplateException.class, () -> VaultTemplateExtractor.extractAndMove(zip, tmp.resolve("MyVault")));
	}

	@Test
	@DisplayName("an archive with more than one vault config fails as malformed")
	public void testMultipleVaultConfigs(@TempDir Path tmp) throws IOException {
		var zip = zip(Map.of( //
				"vault.cryptomator", CONFIG, //
				"nested/vault.cryptomator", CONFIG //
		));

		Assertions.assertThrows(MalformedTemplateException.class, () -> VaultTemplateExtractor.extractAndMove(zip, tmp.resolve("MyVault")));
	}

	@Test
	@DisplayName("an archive exceeding the size limit fails as malformed")
	public void testExceedsSizeLimit(@TempDir Path tmp) throws IOException {
		var big = new byte[2 * 1024 * 1024 + 1];
		var zip = zip(Map.of( //
				"vault.cryptomator", CONFIG, //
				"d/AB/CDEF/0.c9r", big //
		));

		Assertions.assertThrows(MalformedTemplateException.class, () -> VaultTemplateExtractor.extractAndMove(zip, tmp.resolve("MyVault")));
	}

	@Test
	@DisplayName("an archive exceeding the entry-count limit fails as malformed")
	public void testExceedsEntryLimit(@TempDir Path tmp) throws IOException {
		var entries = new LinkedHashMap<String, byte[]>();
		entries.put("vault.cryptomator", CONFIG);
		for (int i = 0; i < 31; i++) {
			entries.put("file" + i + ".c9r", CIPHERTEXT);
		}
		var zip = zip(entries);

		Assertions.assertThrows(MalformedTemplateException.class, () -> VaultTemplateExtractor.extractAndMove(zip, tmp.resolve("MyVault")));
	}

	@Test
	@DisplayName("a zip-slip entry does not escape the destination")
	public void testZipSlip(@TempDir Path tmp) throws IOException {
		var zip = zip(Map.of( //
				"vault.cryptomator", CONFIG, //
				"../escaped.txt", CIPHERTEXT //
		));
		var destination = tmp.resolve("nested").resolve("MyVault");

		try {
			VaultTemplateExtractor.extractAndMove(zip, destination);
		} catch (IOException e) {
			// acceptable: extraction refused the malicious entry
		}

		Assertions.assertTrue(Files.notExists(tmp.resolve("nested").resolve("escaped.txt")), "entry escaped the destination directory");
		Assertions.assertTrue(Files.notExists(tmp.resolve("escaped.txt")), "entry escaped the temp tree");
	}

	private static byte[] zip(Map<String, byte[]> entries) throws IOException {
		var ordered = new LinkedHashMap<>(entries);
		var baos = new ByteArrayOutputStream();
		try (var zos = new ZipOutputStream(baos)) {
			for (var entry : ordered.entrySet()) {
				zos.putNextEntry(new ZipEntry(entry.getKey()));
				zos.write(entry.getValue());
				zos.closeEntry();
			}
		}
		return baos.toByteArray();
	}

}

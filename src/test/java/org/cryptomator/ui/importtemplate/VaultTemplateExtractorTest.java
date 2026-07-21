package org.cryptomator.ui.importtemplate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
	@DisplayName("a vault at the archive root is unpacked and located")
	public void testVaultAtArchiveRoot(@TempDir Path tmp) throws IOException {
		var zip = zip(Map.of( //
				"vault.cryptomator", CONFIG, //
				"d/AB/CDEF/0.c9r", CIPHERTEXT //
		));

		var vaultRoot = VaultTemplateExtractor.extractTo(zip, tmp);

		Assertions.assertArrayEquals(CONFIG, Files.readAllBytes(vaultRoot.resolve("vault.cryptomator")));
		Assertions.assertArrayEquals(CIPHERTEXT, Files.readAllBytes(vaultRoot.resolve("d/AB/CDEF/0.c9r")));
	}

	@Test
	@DisplayName("a vault nested in a single top-level folder is located")
	public void testVaultInSubfolder(@TempDir Path tmp) throws IOException {
		var zip = zip(Map.of( //
				"TemplateVault/vault.cryptomator", CONFIG, //
				"TemplateVault/d/AB/CDEF/0.c9r", CIPHERTEXT //
		));

		var vaultRoot = VaultTemplateExtractor.extractTo(zip, tmp);

		Assertions.assertEquals("TemplateVault", vaultRoot.getFileName().toString());
		Assertions.assertArrayEquals(CONFIG, Files.readAllBytes(vaultRoot.resolve("vault.cryptomator")));
	}

	@Test
	@DisplayName("data that is not a zip archive fails as malformed")
	public void testNotAZip(@TempDir Path tmp) {
		var notAZip = "this is not a zip archive".getBytes(StandardCharsets.UTF_8);

		Assertions.assertThrows(MalformedTemplateException.class, () -> VaultTemplateExtractor.extractTo(notAZip, tmp));
	}

	@Test
	@DisplayName("an archive without a vault config fails as malformed")
	public void testNoVaultConfig(@TempDir Path tmp) throws IOException {
		var zip = zip(Map.of("readme.txt", CONFIG));

		Assertions.assertThrows(MalformedTemplateException.class, () -> VaultTemplateExtractor.extractTo(zip, tmp));
	}

	@Test
	@DisplayName("an archive with more than one vault config fails as malformed")
	public void testMultipleVaultConfigs(@TempDir Path tmp) throws IOException {
		var zip = zip(Map.of( //
				"vault.cryptomator", CONFIG, //
				"nested/vault.cryptomator", CONFIG //
		));

		Assertions.assertThrows(MalformedTemplateException.class, () -> VaultTemplateExtractor.extractTo(zip, tmp));
	}

	@Test
	@DisplayName("an archive exceeding the size limit fails as malformed")
	public void testExceedsSizeLimit(@TempDir Path tmp) throws IOException {
		var big = new byte[(int) VaultTemplateExtractor.MAX_TOTAL_BYTES + 1];
		var zip = zip(Map.of( //
				"vault.cryptomator", CONFIG, //
				"d/AB/CDEF/0.c9r", big //
		));

		Assertions.assertThrows(MalformedTemplateException.class, () -> VaultTemplateExtractor.extractTo(zip, tmp));
	}

	@Test
	@DisplayName("an archive with exactly the maximum number of entries is accepted")
	public void testAtEntryLimit(@TempDir Path tmp) throws IOException {
		var zip = zip(flatEntries(VaultTemplateExtractor.MAX_ENTRIES));

		var vaultRoot = VaultTemplateExtractor.extractTo(zip, tmp);

		Assertions.assertArrayEquals(CONFIG, Files.readAllBytes(vaultRoot.resolve("vault.cryptomator")));
	}

	@Test
	@DisplayName("an archive exceeding the entry-count limit fails as malformed")
	public void testExceedsEntryLimit(@TempDir Path tmp) throws IOException {
		var zip = zip(flatEntries(VaultTemplateExtractor.MAX_ENTRIES + 1));

		Assertions.assertThrows(MalformedTemplateException.class, () -> VaultTemplateExtractor.extractTo(zip, tmp));
	}

	@Test
	@DisplayName("a zip-slip entry does not escape the extraction directory")
	public void testZipSlip(@TempDir Path tmp) throws IOException {
		var zip = zip(Map.of( //
				"vault.cryptomator", CONFIG, //
				"../escaped.txt", CIPHERTEXT //
		));
		var targetDir = tmp.resolve("nested");
		Files.createDirectory(targetDir);

		try {
			VaultTemplateExtractor.extractTo(zip, targetDir);
		} catch (IOException e) {
			// acceptable: extraction refused the malicious entry
		}

		Assertions.assertTrue(Files.notExists(tmp.resolve("escaped.txt")), "entry escaped the extraction directory");
	}

	/**
	 * @param count total number of entries below the zip root, the vault config included
	 */
	private static Map<String, byte[]> flatEntries(int count) {
		var entries = new LinkedHashMap<String, byte[]>();
		entries.put("vault.cryptomator", CONFIG);
		for (int i = 0; i < count - 1; i++) {
			entries.put("file" + i + ".c9r", CIPHERTEXT);
		}
		return entries;
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

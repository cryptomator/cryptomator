package org.cryptomator.ui.importtemplate;

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
	@DisplayName("a vault at the archive root is unpacked into the target directory")
	public void testVaultAtArchiveRoot(@TempDir Path tmp) throws IOException {
		var zip = zip(Map.of( //
				"vault.cryptomator", CONFIG, //
				"d/AB/CDEF/0.c9r", CIPHERTEXT //
		));

		VaultTemplateExtractor.extractTo(zip, tmp);

		Assertions.assertArrayEquals(CONFIG, Files.readAllBytes(tmp.resolve("vault.cryptomator")));
		Assertions.assertArrayEquals(CIPHERTEXT, Files.readAllBytes(tmp.resolve("d/AB/CDEF/0.c9r")));
	}

	@Test
	@DisplayName("a vault nested in a top-level folder fails as malformed")
	public void testVaultInSubfolder(@TempDir Path tmp) throws IOException {
		// requirement: the vault starts in the archive root, an enclosing folder is not a supported template
		var zip = zip(Map.of( //
				"TemplateVault/vault.cryptomator", CONFIG, //
				"TemplateVault/d/AB/CDEF/0.c9r", CIPHERTEXT //
		));

		Assertions.assertThrows(MalformedTemplateException.class, () -> VaultTemplateExtractor.extractTo(zip, tmp));
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
	@DisplayName("a further vault config below the root is ordinary content, the root one identifies the vault")
	public void testFurtherVaultConfigBelowRoot(@TempDir Path tmp) throws IOException {
		// with the vault fixed at the archive root a nested config is simply extracted along with everything
		// else and counts against the entry limit
		var zip = zip(Map.of( //
				"vault.cryptomator", CONFIG, //
				"nested/vault.cryptomator", CIPHERTEXT //
		));

		VaultTemplateExtractor.extractTo(zip, tmp);

		Assertions.assertArrayEquals(CONFIG, Files.readAllBytes(tmp.resolve("vault.cryptomator")));
		Assertions.assertArrayEquals(CIPHERTEXT, Files.readAllBytes(tmp.resolve("nested/vault.cryptomator")));
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

		VaultTemplateExtractor.extractTo(zip, tmp);

		Assertions.assertArrayEquals(CONFIG, Files.readAllBytes(tmp.resolve("vault.cryptomator")));
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

	@Test
	@DisplayName("an unpacked vault is moved to the destination, creating missing parents")
	public void testMoveToDestination(@TempDir Path tmp) throws IOException {
		var source = unpackedVault(tmp.resolve("unpacked"));
		var destination = tmp.resolve("parent").resolve("MyVault"); // parent does not exist yet

		VaultTemplateExtractor.moveToDestination(source, destination);

		Assertions.assertArrayEquals(CONFIG, Files.readAllBytes(destination.resolve("vault.cryptomator")));
		Assertions.assertArrayEquals(CIPHERTEXT, Files.readAllBytes(destination.resolve("d/AB/0.c9r")));
		Assertions.assertTrue(Files.notExists(source), "the vault should no longer be at its old location");
	}

	@Test
	@DisplayName("an existing destination is not overwritten")
	public void testMoveToExistingDestination(@TempDir Path tmp) throws IOException {
		var source = unpackedVault(tmp.resolve("unpacked"));
		var destination = Files.createDirectory(tmp.resolve("MyVault"));

		Assertions.assertThrows(FileAlreadyExistsException.class, () -> VaultTemplateExtractor.moveToDestination(source, destination));
	}

	private static Path unpackedVault(Path dir) throws IOException {
		Files.createDirectories(dir.resolve("d/AB"));
		Files.write(dir.resolve("vault.cryptomator"), CONFIG);
		Files.write(dir.resolve("d/AB/0.c9r"), CIPHERTEXT);
		return dir;
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

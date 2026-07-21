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
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class VaultTemplateTest {

	private static final byte[] CIPHERTEXT = "some-ciphertext".getBytes(StandardCharsets.UTF_8);
	private static final String API_BASE_URL = "https://hub.example.com/api";

	@Test
	@DisplayName("a hub template exposes the hub it belongs to")
	public void testHubUrl() throws IOException {
		var zip = zip(Map.of("vault.cryptomator", hubVaultConfig(API_BASE_URL)));

		try (var inTest = VaultTemplate.extract(zip)) {
			Assertions.assertEquals(API_BASE_URL + "/", inTest.hubUrl());
		}
	}

	@Test
	@DisplayName("a template whose config declares no hub has no hub url")
	public void testNoHubHeader() throws IOException {
		var zip = zip(Map.of("vault.cryptomator", vaultConfig(null)));

		try (var inTest = VaultTemplate.extract(zip)) {
			Assertions.assertNull(inTest.hubUrl());
		}
	}

	@Test
	@DisplayName("an archive whose vault config is not a decodable token is rejected")
	public void testUndecodableVaultConfig() throws IOException {
		var zip = zip(Map.of("vault.cryptomator", "not-a-jwt".getBytes(StandardCharsets.UTF_8)));

		Assertions.assertThrows(MalformedTemplateException.class, () -> VaultTemplate.extract(zip));
	}

	@Test
	@DisplayName("closing discards the temporary directory, also after a completed import")
	public void testCloseCleansUpAfterMove(@TempDir Path tmp) throws IOException {
		var zip = zip(Map.of( //
				"vault.cryptomator", hubVaultConfig(API_BASE_URL), //
				"d/AB/CDEF/0.c9r", CIPHERTEXT //
		));
		var workDir = Files.createDirectory(tmp.resolve("work"));
		var destination = tmp.resolve("MyVault");

		try (var inTest = VaultTemplate.extract(zip, workDir)) {
			Assertions.assertFalse(isEmpty(workDir), "sanity: the temporary directory should exist before close()");
			inTest.moveTo(destination);
		}

		Assertions.assertTrue(isEmpty(workDir), "temporary extraction directory was not cleaned up");
		Assertions.assertTrue(Files.exists(destination.resolve("vault.cryptomator")), "the moved vault must survive close()");
		Assertions.assertArrayEquals(CIPHERTEXT, Files.readAllBytes(destination.resolve("d/AB/CDEF/0.c9r")));
	}

	@Test
	@DisplayName("closing without importing discards the temporary directory")
	public void testCloseCleansUpWithoutMove(@TempDir Path tmp) throws IOException {
		var zip = zip(Map.of("vault.cryptomator", hubVaultConfig(API_BASE_URL)));
		var workDir = Files.createDirectory(tmp.resolve("work"));

		try (var _ = VaultTemplate.extract(zip, workDir)) {
			Assertions.assertFalse(isEmpty(workDir), "sanity: the temporary directory should exist before close()");
		}

		Assertions.assertTrue(isEmpty(workDir), "temporary extraction directory was not cleaned up");
	}

	@Test
	@DisplayName("a rejected template leaves no temporary directory behind")
	public void testNoTempDirLeakOnFailure(@TempDir Path tmp) throws IOException {
		var zip = zip(Map.of("readme.txt", CIPHERTEXT)); // no vault config
		var workDir = Files.createDirectory(tmp.resolve("work"));

		Assertions.assertThrows(MalformedTemplateException.class, () -> VaultTemplate.extract(zip, workDir));

		Assertions.assertTrue(isEmpty(workDir), "a failed extraction leaked a temporary directory");
	}

	private static boolean isEmpty(Path dir) throws IOException {
		try (var children = Files.list(dir)) {
			return children.findAny().isEmpty();
		}
	}

	private static byte[] hubVaultConfig(String apiBaseUrl) {
		return vaultConfig("""
				,
				"hub": {
					"clientId":"cryptomator",\
					"authEndpoint":"https://login.example.com/auth",\
					"tokenEndpoint":"https://login.example.com/token",\
					"authSuccessUrl":"https://hub.example.com/app/unlock-success",\
					"authErrorUrl":"https://hub.example.com/app/unlock-error",\
					"apiBaseUrl":"%s"
				}""".formatted(apiBaseUrl));
	}

	/**
	 * Builds a vault config token. The signature is not verifiable without the masterkey, which is exactly the state
	 * the import dialog operates in - so a dummy signature is sufficient here.
	 */
	private static byte[] vaultConfig(String extraHeaderFields) {
		var header = """
			{ "kid":"hub+https://hub.example.com/api/vaults/123",\
			  "typ":"JWT",\
			  "alg":"HS256"\
			  %s
			  }""".formatted(extraHeaderFields == null ? "" : extraHeaderFields);
		var payload = """
			{ "format":8,\
			  "cipherCombo":"SIV_GCM",\
			  "shorteningThreshold":220\
			  }""";
		var encoder = Base64.getUrlEncoder().withoutPadding();
		var token = encoder.encodeToString(header.getBytes(StandardCharsets.UTF_8)) //
				+ "." + encoder.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) //
				+ "." + encoder.encodeToString("signature".getBytes(StandardCharsets.UTF_8));
		return token.getBytes(StandardCharsets.US_ASCII);
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

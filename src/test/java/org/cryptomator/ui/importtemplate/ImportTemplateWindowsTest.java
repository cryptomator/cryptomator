package org.cryptomator.ui.importtemplate;

import org.cryptomator.JavaFXUtil;
import org.cryptomator.ui.dialogs.Dialogs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javafx.stage.Stage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class ImportTemplateWindowsTest {

	private static final byte[] CIPHERTEXT = "some-ciphertext".getBytes(StandardCharsets.UTF_8);
	private static final String API_BASE_URL = "https://hub.example.com/api";

	private ExecutorService executor;
	private Stage primaryStage;
	private ImportTemplateWindows inTest;

	@BeforeAll
	public static void initJavaFx() throws InterruptedException {
		// extractAndShowImportTemplateWindow completes on the FX thread
		var isRunning = JavaFXUtil.startPlatform();
		Assumptions.assumeTrue(isRunning);
	}

	@BeforeEach
	public void setup() {
		executor = Executors.newSingleThreadExecutor();
		primaryStage = mock(Stage.class);
		inTest = spy(new ImportTemplateWindows(mock(ImportTemplateComponent.Factory.class), mock(Dialogs.class), primaryStage, executor));
	}

	@AfterEach
	public void teardown() {
		executor.shutdownNow();
	}

	@Test
	@DisplayName("extractAsync yields the unpacked template")
	public void testExtractAsync() throws Exception {
		var zip = zip(Map.of("vault.cryptomator", hubVaultConfig()));

		var template = inTest.extractAsync(zip).toCompletableFuture().get(5, TimeUnit.SECONDS);

		try (template) {
			Assertions.assertEquals(API_BASE_URL + "/", template.hubUrl());
		}
	}

	@Test
	@DisplayName("extractAsync hands a dependent stage a CompletionException wrapping the malformed cause")
	public void testExtractAsyncWrapsMalformedCause() throws Exception {
		var zip = zip(Map.of("readme.txt", CIPHERTEXT)); // no vault config

		// this wrapping is load-bearing: extractAndShowImportTemplateWindow unwraps exactly one layer to decide
		// between the malformed dialog and the generic error window
		var seen = inTest.extractAsync(zip) //
				.handle((template, throwable) -> throwable) //
				.toCompletableFuture().get(5, TimeUnit.SECONDS);

		Assertions.assertInstanceOf(CompletionException.class, seen);
		Assertions.assertInstanceOf(MalformedTemplateException.class, seen.getCause());
	}

	@Test
	@DisplayName("a malformed template is reported by the malformed-template dialog")
	public void testMalformedTemplateShowsDialog() throws Exception {
		var zip = zip(Map.of("readme.txt", CIPHERTEXT)); // no vault config
		doReturn(primaryStage).when(inTest).showMalformedTemplateDialog();

		var result = inTest.extractAndShowImportTemplateWindow("MyVault", zip).toCompletableFuture().get(5, TimeUnit.SECONDS);

		verify(inTest).showMalformedTemplateDialog();
		Assertions.assertEquals(primaryStage, result);
	}

	@Test
	@DisplayName("a failure that is not the template's fault does not claim the template is malformed")
	public void testEnvironmentalFailureIsNotReportedAsMalformed() {
		var cause = new IOException("no space left on device");
		doReturn(CompletableFuture.failedFuture(new CompletionException(cause))).when(inTest).extractAsync(org.mockito.ArgumentMatchers.any());

		var future = inTest.extractAndShowImportTemplateWindow("MyVault", new byte[0]).toCompletableFuture();

		var thrown = Assertions.assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
		Assertions.assertEquals(cause, thrown.getCause());
		verify(inTest, never()).showMalformedTemplateDialog();
	}

	private static byte[] hubVaultConfig() {
		var header = """
				{"kid":"hub+https://hub.example.com/api/vaults/123","typ":"JWT","alg":"HS256",\
				"hub":{"clientId":"cryptomator","authEndpoint":"https://login.example.com/auth",\
				"tokenEndpoint":"https://login.example.com/token",\
				"authSuccessUrl":"https://hub.example.com/app/unlock-success",\
				"authErrorUrl":"https://hub.example.com/app/unlock-error",\
				"apiBaseUrl":"%s"}}""".formatted(API_BASE_URL);
		var payload = "{\"format\":8,\"cipherCombo\":\"SIV_GCM\",\"shorteningThreshold\":220}";
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

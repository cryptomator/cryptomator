package org.cryptomator.ui.common;

import org.cryptomator.common.Environment;
import org.cryptomator.common.ErrorCode;
import org.cryptomator.common.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ErrorController implements FxController {

	private static final String SEARCH_URL_FORMAT = "https://github.com/cryptomator/cryptomator/discussions/categories/errors?discussions_q=category:Errors+%s";
	private static final String REPORT_URL_FORMAT = "https://github.com/cryptomator/cryptomator/discussions/new?category=Errors&title=Error+%s&body=%s";
	private static final String SEARCH_ERRORCODE_DELIM = " OR ";
	private static final String REPORT_BODY_TEMPLATE = """
			OS: %s / %s
			App: %s / %s
			
			<!-- ✏️ Please describe what happened as accurately as possible. -->
			
			<!-- 📋 Please also copy and paste the detail text from the error window. -->
			""";

	private final Application application;
	private final String stackTrace;
	private final ErrorCode errorCode;
	private final Scene previousScene;
	private final Stage window;
	private final Environment environment;

	private BooleanProperty copiedDetails = new SimpleBooleanProperty();

	@Inject
	ErrorController(Application application, @Named("stackTrace") String stackTrace, ErrorCode errorCode, @Nullable Scene previousScene, Stage window, Environment environment) {
		this.application = application;
		this.stackTrace = stackTrace;
		this.errorCode = errorCode;
		this.previousScene = previousScene;
		this.window = window;
		this.environment = environment;
	}

	@FXML
	public void back() {
		if (previousScene != null) {
			window.setScene(previousScene);
		}
	}

	@FXML
	public void close() {
		window.close();
	}

	@FXML
	public void searchError() {
		var searchTerm = URLEncoder.encode(getErrorCode().replace(ErrorCode.DELIM, SEARCH_ERRORCODE_DELIM), StandardCharsets.UTF_8);
		application.getHostServices().showDocument(SEARCH_URL_FORMAT.formatted(searchTerm));
	}

	@FXML
	public void reportError() {
		var title = URLEncoder.encode(getErrorCode(), StandardCharsets.UTF_8);
		var enhancedTemplate = String.format(REPORT_BODY_TEMPLATE, //
				System.getProperty("os.name"), //
				System.getProperty("os.version"), //
				environment.getAppVersion().orElse("undefined"), //
				environment.getBuildNumber().orElse("undefined"));
		var body = URLEncoder.encode(enhancedTemplate, StandardCharsets.UTF_8);
		application.getHostServices().showDocument(REPORT_URL_FORMAT.formatted(title, body));
	}

	@FXML
	public void copyDetails() {
		ClipboardContent clipboardContent = new ClipboardContent();
		clipboardContent.putString(getDetailText());
		Clipboard.getSystemClipboard().setContent(clipboardContent);

		copiedDetails.set(true);
		CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS, Platform::runLater).execute(() -> {
			copiedDetails.set(false);
		});
	}

	/* Getter/Setter */

	public boolean isPreviousScenePresent() {
		return previousScene != null;
	}

	public String getStackTrace() {
		return stackTrace;
	}

	public String getErrorCode() {
		return errorCode.toString();
	}

	public String getDetailText() {
		return "```\nError Code " + getErrorCode() + "\n" + getStackTrace() + "\n```";
	}

	public BooleanProperty copiedDetailsProperty() {
		return copiedDetails;
	}

	public boolean getCopiedDetails() {
		return copiedDetails.get();
	}
}

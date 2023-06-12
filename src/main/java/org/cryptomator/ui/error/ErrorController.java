package org.cryptomator.ui.error;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.cryptomator.common.Environment;
import org.cryptomator.common.ErrorCode;
import org.cryptomator.common.Nullable;
import org.cryptomator.ui.common.FxController;

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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ErrorController implements FxController {

	private static final String ERROR_CODES_URL = "https://gist.githubusercontent.com/cryptobot/accba9fb9555e7192271b85606f97230/raw/errorcodes.json";
	private static final String SEARCH_URL_FORMAT = "https://github.com/cryptomator/cryptomator/discussions/categories/errors?discussions_q=category:Errors+%s";
	private static final String REPORT_URL_FORMAT = "https://github.com/cryptomator/cryptomator/discussions/new?category=Errors&title=Error+%s&body=%s";
	private static final String SEARCH_ERRORCODE_DELIM = " OR ";
	private static final String REPORT_BODY_TEMPLATE = """
			OS: %s / %s
			App: %s / %s
			
			<!-- ✏ Please describe what happened as accurately as possible. -->
			
			<!-- 📋 Please also copy and paste the detail text from the error window. -->
			
			<!-- ℹ Text enclosed like this (chevrons, exclamation mark, two dashes) is not visible to others! -->
			
			<!-- ❗ If the description or the detail text is missing, the discussion will be deleted. -->
			""";

	private final Application application;
	private final String stackTrace;
	private final ErrorCode errorCode;
	private final Scene previousScene;
	private final Stage window;
	private final Environment environment;

	private final BooleanProperty copiedDetails = new SimpleBooleanProperty();
	private final BooleanProperty lookUpSolutionVisibility = new SimpleBooleanProperty();
	private final BooleanProperty isLoadingHttpResponse = new SimpleBooleanProperty();
	private	ErrorDiscussion matchingErrorDiscussion;

	@Inject
	ErrorController(Application application, @Named("stackTrace") String stackTrace, ErrorCode errorCode, @Nullable Scene previousScene, Stage window, Environment environment, ExecutorService executorService) {
		this.application = application;
		this.stackTrace = stackTrace;
		this.errorCode = errorCode;
		this.previousScene = previousScene;
		this.window = window;
		this.environment = environment;

		isLoadingHttpResponse.set(true);
		HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
		HttpRequest httpRequest = HttpRequest.newBuilder()
				.uri(URI.create(ERROR_CODES_URL))
				.build();
		httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofInputStream())
				.thenAcceptAsync(this::loadHttpResponse, executorService)
				.whenCompleteAsync((r,e) -> isLoadingHttpResponse.set(false), Platform::runLater);
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
	public void showSolution() {
		if(matchingErrorDiscussion != null){
			application.getHostServices().showDocument(matchingErrorDiscussion.url);
		}
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
				environment.getAppVersion(), //
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

	private void loadHttpResponse(HttpResponse<InputStream> response){
		if (response.statusCode() == 200) {
			Map<String,ErrorDiscussion> map = new Gson().fromJson(
					new InputStreamReader(response.body(),StandardCharsets.UTF_8),
					new TypeToken<Map<String,ErrorDiscussion>>(){}.getType());

			if(!map.values().stream().filter(this::isPartialMatchFilter).findFirst().isEmpty()) {
				Comparator<ErrorDiscussion> comp = this::compareExactMatch;
				matchingErrorDiscussion = map.values().stream().sorted(comp
						.thenComparing(this::compareSecondLevelMatch)
						.thenComparing(this::compareThirdLevelMatch)
						.thenComparing(this::compareIsAnswered)
						.thenComparing(this::compareUpvoteCount)
				).findFirst().get();
				lookUpSolutionVisibility.set(true);
			}
		}
	}

	public boolean isPartialMatchFilter(ErrorDiscussion errorDiscussion) {
		return errorDiscussion.title.contains(" " + errorCode.methodCode());
	}

	public int compareUpvoteCount(ErrorDiscussion ed1, ErrorDiscussion ed2) {
		return Integer.compare(ed2.upvoteCount, ed1.upvoteCount);
	}

	public int compareIsAnswered(ErrorDiscussion ed1, ErrorDiscussion ed2) {
		if (ed1.answer!=null && ed2.answer==null) {
			return -1;
		} else if (ed1.answer==null && ed2.answer!=null) {
			return 1;
		} else {
			return 0;
		}
	}
	public int compareExactMatch(ErrorDiscussion ed1, ErrorDiscussion ed2) {
		if (ed1.title.contains(getErrorCode()) && !ed2.title.contains(getErrorCode())) {
			return -1;
		} else if (!ed1.title.contains(getErrorCode()) && ed2.title.contains(getErrorCode())) {
			return 1;
		} else {
			return 0;
		}
	}

	public int compareSecondLevelMatch(ErrorDiscussion ed1, ErrorDiscussion ed2) {
		String value = " " + errorCode.methodCode() + ErrorCode.DELIM + errorCode.rootCauseCode();
		if (ed1.title.contains(value) && !ed2.title.contains(value)) {
			return -1;
		} else if (!ed1.title.contains(value) && ed2.title.contains(value)) {
			return 1;
		} else {
			return 0;
		}
	}

	public int compareThirdLevelMatch(ErrorDiscussion ed1, ErrorDiscussion ed2) {
		String value = " " + errorCode.methodCode();
		if (ed1.title.contains(value) && !ed2.title.contains(value)) {
			return -1;
		} else if (!ed1.title.contains(value) && ed2.title.contains(value)) {
			return 1;
		} else {
			return 0;
		}
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

	public BooleanProperty lookUpSolutionVisibilityProperty() {
		return lookUpSolutionVisibility;
	}

	public boolean getLookUpSolutionVisibility() {
		return lookUpSolutionVisibility.get();
	}

	public BooleanProperty isLoadingHttpResponseProperty() {
		return isLoadingHttpResponse;
	}

	public boolean getIsLoadingHttpResponse() {
		return isLoadingHttpResponse.get();
	}

}
package org.cryptomator.ui.error;

import javafx.application.Application;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.cryptomator.common.Environment;
import org.cryptomator.common.ErrorCode;

public class ErrorHandler {

	private final Application application;
	private final ErrorCode errorCode;
	private final String stackTrace;
	private static final String SEARCH_ERRORCODE_DELIM = " OR ";
	private static final String SEARCH_URL_FORMAT = "https://github.com/cryptomator/cryptomator/discussions/categories/errors?discussions_q=category:Errors+%s";
	private static final String REPORT_URL_FORMAT = "https://github.com/cryptomator/cryptomator/discussions/new?category=Errors&title=Error+%s&body=%s";
	private static final String REPORT_BODY_TEMPLATE = """
            OS: %s / %s
            App: %s / %s
                            
            <!-- ✏ Please describe what happened as accurately as possible. -->
                            
            <!-- 📋 Please also copy and paste the detail text from the error window. -->
                            
            <!-- ℹ Text enclosed like this (chevrons, exclamation mark, two dashes) is not visible to others! -->
                            
            <!-- ❗ If the description or the detail text is missing, the discussion will be deleted. -->
            """;

	public ErrorHandler(Application application, ErrorCode errorCode, String stackTrace) {
		this.application = application;
		this.errorCode = errorCode;
		this.stackTrace = stackTrace;
	}

	public void showErrorSolution(ErrorDiscussion discussion) {
		if (discussion != null) {
			application.getHostServices().showDocument(discussion.url);
		}
	}

	public void searchError(String errorCode) {
		var searchTerm = URLEncoder.encode(errorCode.replace(ErrorCode.DELIM, SEARCH_ERRORCODE_DELIM), StandardCharsets.UTF_8);
		application.getHostServices().showDocument(ErrorHandler.SEARCH_URL_FORMAT.formatted(searchTerm));
	}

	public void reportError(String errorCode, Environment environment) {
		var title = URLEncoder.encode(errorCode, StandardCharsets.UTF_8);
		var enhancedTemplate = String.format(REPORT_BODY_TEMPLATE, System.getProperty("os.version"),
				environment.getAppVersion(),
				environment.getBuildNumber().orElse("undefined"));
		var body = URLEncoder.encode(enhancedTemplate, StandardCharsets.UTF_8);
		application.getHostServices().showDocument(REPORT_URL_FORMAT.formatted(title, body));
	}
}

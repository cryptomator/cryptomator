package org.cryptomator.ui.error;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.cryptomator.common.Environment;
import org.cryptomator.common.ErrorCode;
import org.cryptomator.common.Nullable;
import org.cryptomator.ui.common.FxController;

import java.lang.reflect.Type;
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
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.util.Set;

public class ErrorController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(ErrorController.class);
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

	private BooleanProperty copiedDetails = new SimpleBooleanProperty();
	private BooleanProperty lookUpSolutionVisibility = new SimpleBooleanProperty();
	private final HttpClient httpClient;

	List<ErrorDiscussion> errorDiscussionList;

	@Inject
	ErrorController(Application application, @Named("stackTrace") String stackTrace, ErrorCode errorCode, @Nullable Scene previousScene, Stage window, Environment environment) {
		this.application = application;
		this.stackTrace = stackTrace;
		this.errorCode = errorCode;
		this.previousScene = previousScene;
		this.window = window;
		this.environment = environment;
		this.httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();



		HttpClient httpClient = HttpClient.newHttpClient();
		HttpRequest httpRequest = HttpRequest.newBuilder()
				.uri(URI.create(ERROR_CODES_URL))
				.build();

		CompletableFuture<HttpResponse<String>> future = httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString());

		future.thenAcceptAsync(response -> {
			int statusCode = response.statusCode();
			if (statusCode == 200) {
				String jsonString = response.body();
				//LOG.debug("jpkED - jsonString : " + jsonString);
				//System.out.println(jsonString);

				//JsonObject jsonObject = (JsonObject)new JsonParser().parse(jsonString);

				jsonString = "[{\"id\":\"D_kwDOAPryk84ASwC1\",\"upvoteCount\":4,\"title\":\"Error GH1B:GH1B:NJFJ\",\"url\":\"https://github.com/cryptomator/cryptomator/discussions/2710\",\"answer\":{\"url\":\"https://github.com/cryptomator/cryptomator/discussions/2710#discussioncomment-5191708\",\"upvoteCount\":3},\"comments\":8},{\"id\":\"D_kwDOAPryk84ASw_I\",\"upvoteCount\":1,\"title\":\"Error GH1B:GH1B:NJFJ\",\"url\":\"https://github.com/cryptomator/cryptomator/discussions/2716\",\"answer\":null,\"comments\":2},{\"id\":\"D_kwDOAPryk84ASxAg\",\"upvoteCount\":1,\"title\":\"Error GH1B:GH1B:NJFJ\",\"url\":\"https://github.com/cryptomator/cryptomator/discussions/2717\",\"answer\":{\"url\":\"https://github.com/cryptomator/cryptomator/discussions/2717#discussioncomment-5181996\",\"upvoteCount\":2},\"comments\":3},{\"id\":\"D_kwDOAPryk84ASxDK\",\"upvoteCount\":1,\"title\":\"Error GH1B:GH1B:NJFJ\",\"url\":\"https://github.com/cryptomator/cryptomator/discussions/2718\",\"answer\":null,\"comments\":0},{\"id\":\"D_kwDOAPryk84ATj-U\",\"upvoteCount\":1,\"title\":\"ErrorCode N05M:GEAO:GEAO\",\"url\":\"https://github.com/cryptomator/cryptomator/discussions/2877\",\"answer\":null,\"comments\":1},{\"id\":\"D_kwDOAPryk84ATl2o\",\"upvoteCount\":1,\"title\":\"Error GH1B:GH1B:NJFJ\",\"url\":\"https://github.com/cryptomator/cryptomator/discussions/2883\",\"answer\":{\"url\":\"https://github.com/cryptomator/cryptomator/discussions/2883#discussioncomment-5769879\",\"upvoteCount\":1},\"comments\":1},{\"id\":\"D_kwDOAPryk84ATqCM\",\"upvoteCount\":1,\"title\":\"Error Code 4VHF:1S9S:5EOP\",\"url\":\"https://github.com/cryptomator/cryptomator/discussions/2887\",\"answer\":null,\"comments\":0},{\"id\":\"D_kwDOAPryk84ATqDF\",\"upvoteCount\":1,\"title\":\"Error H1VR:OTAS:OTAS\",\"url\":\"https://github.com/cryptomator/cryptomator/discussions/2888\",\"answer\":null,\"comments\":0},{\"id\":\"D_kwDOAPryk84ATrPD\",\"upvoteCount\":1,\"title\":\"Error S4DB:IV2H:I3UI\",\"url\":\"https://github.com/cryptomator/cryptomator/discussions/2891\",\"answer\":{\"url\":\"https://github.com/cryptomator/cryptomator/discussions/2891#discussioncomment-5797329\",\"upvoteCount\":1},\"comments\":1},{\"id\":\"D_kwDOAPryk84ATtJZ\",\"upvoteCount\":1,\"title\":\"Error 3MAT:BDUS:BDUS\",\"url\":\"https://github.com/cryptomator/cryptomator/discussions/2894\",\"answer\":null,\"comments\":0},{\"id\":\"D_kwDOAPryk84ATuDc\",\"upvoteCount\":1,\"title\":\"Error QPDR:AFGD:AFGD\",\"url\":\"https://github.com/cryptomator/cryptomator/discussions/2895\",\"answer\":null,\"comments\":0}]";

				Gson gson = new Gson();
				Type listType = new TypeToken<List<ErrorDiscussion>>(){}.getType();
				errorDiscussionList = gson.fromJson(jsonString,listType);



				loadJsonToErrorDiscussionList();




				LOG.debug("jpkED - errorDiscussionList loaded | amount:"+errorDiscussionList.size()+"");

				//find exact matching
				List<ErrorDiscussion> newErrorDiscussionList = new ArrayList<>();
				newErrorDiscussionList.addAll(errorDiscussionList);
				newErrorDiscussionList.removeIf(errorDiscussion -> !errorDiscussion.getErrorCode().contains(getErrorCode()));
				if(newErrorDiscussionList.size()>0){
					errorDiscussionList.clear();
					errorDiscussionList.addAll(newErrorDiscussionList);
					LOG.debug("jpkED - exact match | amount:"+errorDiscussionList.size()+"");

					lookUpSolutionVisibility.set(true);
				}
				else{
					//find method code matching
					newErrorDiscussionList.clear();
					newErrorDiscussionList.addAll(errorDiscussionList);
					newErrorDiscussionList.removeIf(errorDiscussion -> !errorDiscussion.getErrorCode().contains(getErrorCodeMethodCode()));
					if(newErrorDiscussionList.size()>0){
						errorDiscussionList.clear();
						errorDiscussionList.addAll(newErrorDiscussionList);
						LOG.debug("jpkED - method match | amount:"+errorDiscussionList.size()+"");
					}
					else{
						errorDiscussionList.clear();
						LOG.debug("jpkED - nothing found");

					}
				}

				if(errorDiscussionList.size()!=0){
					//answered
					newErrorDiscussionList.clear();
					newErrorDiscussionList.addAll(errorDiscussionList);
					newErrorDiscussionList.removeIf(errorDiscussion -> errorDiscussion.answer == null);
					if(newErrorDiscussionList.size()>0){
						errorDiscussionList.clear();
						errorDiscussionList.addAll(newErrorDiscussionList);
						LOG.debug("jpkED - answered | amount:"+errorDiscussionList.size()+"");
					}
					Collections.sort(errorDiscussionList, this::errorDiscussionComparator);
					LOG.debug("jpkED - most upvote | id: " + errorDiscussionList.get(0).id + " | title: " + errorDiscussionList.get(0).title + " | upvoteCount:" + errorDiscussionList.get(0).upvoteCount);
				}
			}
		}).join();


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
		if(errorDiscussionList.size()!=0) {
			application.getHostServices().showDocument(errorDiscussionList.get(0).url);
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

	/* Getter/Setter */

	public boolean isPreviousScenePresent() {
		return previousScene != null;
	}

	public String getStackTrace() {
		return stackTrace;
	}

	public String getErrorCode() {
		//return "GH1B:GH1B:NJFJ";  // 31 exact match - 4 answered
		//return "GIJU:E215:E215";   // 1 exact match - 0 answered
		//return "6PHE:UG0C:UG0C";   // 0 exact match - 8 method match - 3 answered
		//return "0000:0000:0000";   // 0 match
		return errorCode.toString(); // 0 exact match - 8 method match - 3 answered
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

	public String getDetailTexts() {
		return "```\nError Code " + getErrorCode() + "\n" + getStackTrace() + "\n```";
	}

	private void filterListByMethodCode(){
		List<ErrorDiscussion> newErrorDiscussionList = new ArrayList<>();
		for (int i = 0; i < errorDiscussionList.size(); i++) {
			ErrorDiscussion errorDiscussion = errorDiscussionList.get(i);
			if(errorDiscussion.getMethodCode().equals(getErrorCodeMethodCode())){
				newErrorDiscussionList.add(errorDiscussion);
			}
		}
		if(newErrorDiscussionList.size()!=0){
			errorDiscussionList = newErrorDiscussionList;
			lookUpSolutionVisibility.set(true);
			LOG.debug("jpkED - found matching method code | amount:"+errorDiscussionList.size()+"");
		}
		else{
			errorDiscussionList = newErrorDiscussionList; // to clear results
			LOG.debug("jpkED - no matching method code found");
		}
	}

	private int errorDiscussionComparator(ErrorDiscussion t, ErrorDiscussion t1) {
		return Integer.compare(t1.upvoteCount, t.upvoteCount);
	}

	private String getErrorCodeMethodCode(){
		return getErrorCode().substring(0,4);
	}

	private void loadJsonToErrorDiscussionList(){
		String errorCodesUrl = "https://gist.githubusercontent.com/cryptobot/accba9fb9555e7192271b85606f97230/raw/errorcodes.json";
		try (InputStream is = new URL(errorCodesUrl).openStream()) { //TODO: HttpClient(){  async request

			JsonObject jsonObject = (JsonObject)new JsonParser().parse(new InputStreamReader(is,"UTF-8"));

			LOG.debug("jpkED - jsonObject | "+jsonObject.toString());

			Set<String> keys = jsonObject.keySet();
			errorDiscussionList = new ArrayList<>();
			for (int i = 0; i < jsonObject.size(); i++) {
				errorDiscussionList.add(new Gson().fromJson(jsonObject.get(keys.stream().toList().get(i)),ErrorDiscussion.class));
			}
			LOG.debug("jpkED - errorDiscussionList loaded | amount:"+errorDiscussionList.size()+"");
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}
}
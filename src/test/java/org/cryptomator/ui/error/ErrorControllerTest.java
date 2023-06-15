package org.cryptomator.ui.error;

import org.cryptomator.common.Environment;
import org.cryptomator.common.ErrorCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.concurrent.ExecutorService;

class ErrorControllerTest {
	Application application;
	String stackTrace;
	ErrorCode errorCode;
	Scene previousScene;
	Stage window;
	Environment environment;
	ExecutorService executorService;
	ErrorController errorController;

	@BeforeEach
	public void beforeEach() {
		application = Mockito.mock(Application.class);
		stackTrace = "This is a stackTrace mock";
		errorCode = Mockito.mock(ErrorCode.class);
		previousScene = Mockito.mock(Scene.class);
		window = Mockito.mock(Stage.class);
		environment = Mockito.mock(Environment.class);
		executorService = Mockito.mock(ExecutorService.class);
		errorController = new ErrorController(
				application,
				stackTrace,
				errorCode,
				previousScene,
				window,
				environment,
				executorService);
	}

	private ErrorDiscussion createErrorDiscussion(String title, int upvoteCount, ErrorDiscussion.Answer answer){
		ErrorDiscussion ed = new ErrorDiscussion();
		ed.title =title;
		ed.upvoteCount = upvoteCount;
		ed.answer = answer;
		return ed;
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
		10, 5, -1 
		8, 15, 1 
		10, 10, 0
		""")
	public void testCompareUpvoteCount(int leftUpvoteCount, int rightUpvoteCount, int expectedResult) {
		var left = createErrorDiscussion("", leftUpvoteCount, null);
		var right = createErrorDiscussion("", rightUpvoteCount, null);
		int result = errorController.compareUpvoteCount(left, right);
		Assertions.assertEquals(expectedResult,Integer.signum(result));
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
		false, false, 0
		true, true, 0 
		true, false, -1 
		false, true, 1 
		""")
	public void testCompareIsAnswered(boolean leftIsAnswered, boolean rightIsAnswered, int expectedResult) {
		var answer = new ErrorDiscussion.Answer();
		var left = createErrorDiscussion("", 0, leftIsAnswered ? answer : null);
		var right = createErrorDiscussion("", 0, rightIsAnswered ? answer : null);
		int result = errorController.compareIsAnswered(left, right);
		Assertions.assertEquals(expectedResult, result);
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
		Error 0000:0000:0000, Error 0000:0000:0000, 0
		Error 6HU1:12H1:HU7J, Error 0000:0000:0000, -1
		Error 0000:0000:0000, Error 6HU1:12H1:HU7J, 1
		""")
	public void testCompareExactMatch(String leftTitle, String rightTitle, int expectedResult) {
		Mockito.when(errorCode.toString()).thenReturn("6HU1:12H1:HU7J");
		var left = createErrorDiscussion(leftTitle, 0, null);
		var right = createErrorDiscussion(rightTitle, 0,null);
		int result = errorController.compareExactMatch(left,right);
		Assertions.assertEquals(expectedResult, result);
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
		Error 6HU1:12H1:0000, Error 6HU1:12H1:0000, 0
		Error 0000:0000:0000, Error 0000:0000:0000, 0
		Error 6HU1:12H1:0000, Error 0000:0000:0000, -1
		Error 0000:0000:0000, Error 6HU1:12H1:0000, 1
		""")
	public void testCompareSecondLevelMatch(String leftTitle, String rightTitle, int expectedResult) {
		Mockito.when(errorCode.methodCode()).thenReturn("6HU1");
		Mockito.when(errorCode.rootCauseCode()).thenReturn("12H1");
		var left = createErrorDiscussion(leftTitle, 0, null);
		var right = createErrorDiscussion(rightTitle, 0,null);
		int result = errorController.compareSecondLevelMatch(left,right);
		Assertions.assertEquals(expectedResult, result);
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
		Error 6HU1:0000:0000, Error 6HU1:0000:0000, 0
		Error 0000:0000:0000, Error 0000:0000:0000, 0
		Error 6HU1:0000:0000, Error 0000:0000:0000, -1
		Error 0000:0000:0000, Error 6HU1:0000:0000, 1
		""")
	public void testCompareThirdLevelMatch(String leftTitle, String rightTitle, int expectedResult) {
		Mockito.when(errorCode.methodCode()).thenReturn("6HU1");
		var left = createErrorDiscussion(leftTitle, 0, null);
		var right = createErrorDiscussion(rightTitle, 0,null);
		int result = errorController.compareThirdLevelMatch(left,right);
		Assertions.assertEquals(expectedResult, result);
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
		Error 6HU1:0000:0000, true
		Error 0000:0000:0000, false
		""")
	public void testIsPartialMatchFilter(String title, boolean expectedResult) {
		Mockito.when(errorCode.methodCode()).thenReturn("6HU1");
		var ed = createErrorDiscussion(title, 0, null);
		boolean result = errorController.isPartialMatchFilter(ed);
		Assertions.assertEquals(expectedResult, result);
	}
}
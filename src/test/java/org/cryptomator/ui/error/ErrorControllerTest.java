package org.cryptomator.ui.error;

import org.cryptomator.common.Environment;
import org.cryptomator.common.ErrorCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

	@Test
	@DisplayName("If the upvoteCounts of both ErrorDiscussions are equal, it returns 0")
	void testCompareUpvoteCount1() {
		ErrorDiscussion ed1 = createErrorDiscussion("",10,null);
		ErrorDiscussion ed2 = createErrorDiscussion("",10,null);
		int result = errorController.compareUpvoteCount(ed1, ed2);
		Assertions.assertEquals(0, result);
	}


	@Test
	@DisplayName("If the upvoteCount of the first ErrorDiscussion is greater than the second, it returns < 0.")
	void testCompareUpvoteCount2() {
		ErrorDiscussion ed1 = createErrorDiscussion("",10,null);
		ErrorDiscussion ed2 = createErrorDiscussion("",5,null);
		int result = errorController.compareUpvoteCount(ed1, ed2);
		Assertions.assertTrue(result < 0);
	}

	@Test
	@DisplayName("If the upvoteCount of the second ErrorDiscussion is greater than the first, it returns > 0.")
	void testCompareUpvoteCount3() {
		ErrorDiscussion ed1 = createErrorDiscussion("",8,null);
		ErrorDiscussion ed2 = createErrorDiscussion("",15,null);
		int result = errorController.compareUpvoteCount(ed1, ed2);
		Assertions.assertTrue(result > 0);
	}

	@Test
	@DisplayName("If both ErrorDiscussions has an answer, it returns 0.")
	void testCompareIsAnswered1() {
		ErrorDiscussion ed1 = createErrorDiscussion("",0, new ErrorDiscussion.Answer());
		ErrorDiscussion ed2 = createErrorDiscussion("",0, new ErrorDiscussion.Answer());
		int result = errorController.compareIsAnswered(ed1,ed2);
		Assertions.assertEquals( 0, result);
	}

	@Test
	@DisplayName("If both ErrorDiscussions doesn't have an answer, it returns 0.")
	void testCompareIsAnswered2() {
		ErrorDiscussion ed1 = createErrorDiscussion("",0, null);
		ErrorDiscussion ed2 = createErrorDiscussion("",0, null);
		int result = errorController.compareIsAnswered(ed1,ed2);
		Assertions.assertEquals(0, result);
	}

	@Test
	@DisplayName("If the first ErrorDiscussion has an answer and the second doesn't, it returns < 0.")
	void testCompareIsAnswered3() {
		ErrorDiscussion ed1 = createErrorDiscussion("",0, new ErrorDiscussion.Answer());
		ErrorDiscussion ed2 = createErrorDiscussion("",0,null);
		int result = errorController.compareIsAnswered(ed1,ed2);
		Assertions.assertTrue(result < 0);
	}

	@Test
	@DisplayName("If the second ErrorDiscussion has an answer and the first doesn't, it returns > 0.")
	void testCompareIsAnswered4() {
		ErrorDiscussion ed1 = createErrorDiscussion("",0, null);
		ErrorDiscussion ed2 = createErrorDiscussion("",0, new ErrorDiscussion.Answer());
		int result = errorController.compareIsAnswered(ed1,ed2);
		Assertions.assertTrue(result > 0);
	}

	@ParameterizedTest
	@ValueSource(strings={"0000:0000:0000","6HU1:12H1:HU7J"})
	@DisplayName("If both ErrorDiscussions has a title that contains the full ErrorCode or both doesn't, it returns 0.")
	void testCompareExactMatch1(String errorCodeParameter) {
		Mockito.when(errorCode.toString()).thenReturn(errorCodeParameter);
		ErrorDiscussion ed1 = createErrorDiscussion("Error 0000:0000:0000",0, null);
		ErrorDiscussion ed2 = createErrorDiscussion("Error 0000:0000:0000",0, null);
		int result = errorController.compareExactMatch(ed1,ed2);
		Assertions.assertEquals(0, result);
	}

	@Test
	@DisplayName("If the first ErrorDiscussion has a title that contains the full ErrorCode and the second doesn't, it returns < 0.")
	void testCompareExactMatch2() {
		Mockito.when(errorCode.toString()).thenReturn("6HU1:12H1:HU7J");
		ErrorDiscussion ed1 = createErrorDiscussion("Error 6HU1:12H1:HU7J",0, null);
		ErrorDiscussion ed2 = createErrorDiscussion("Error 0000:0000:0000",0, null);
		int result = errorController.compareExactMatch(ed1,ed2);
		Assertions.assertTrue(result < 0);
	}
	@Test
	@DisplayName("If the second ErrorDiscussion has a title that contains the full ErrorCode and the first doesn't, it returns > 0.")
	void testCompareExactMatch3() {
		Mockito.when(errorController.getErrorCode()).thenReturn("6HU1:12H1:HU7J");
		ErrorDiscussion ed1 = createErrorDiscussion("Error 0000:0000:0000",0, null);
		ErrorDiscussion ed2 = createErrorDiscussion("Error 6HU1:12H1:HU7J",0, null);
		int result = errorController.compareExactMatch(ed1,ed2);
		Assertions.assertTrue(result > 0);
	}

	@Nested
	public class CompareSecondLevelMatch{
		@BeforeEach
		void beforeEach(){
			Mockito.when(errorCode.methodCode()).thenReturn("6HU1");
			Mockito.when(errorCode.rootCauseCode()).thenReturn("12H1");
		}

		@Test
		@DisplayName("If both ErrorDiscussions has a title that contains the first two blocks of the ErrorCode, it returns 0.")
		void testCompareSecondLevelMatch1() {
			ErrorDiscussion ed1 = createErrorDiscussion("Error 6HU1:12H1:0000",0, null);
			ErrorDiscussion ed2 = createErrorDiscussion("Error 6HU1:12H1:0000",0, null);
			int result = errorController.compareSecondLevelMatch(ed1,ed2);
			Assertions.assertEquals(0, result);
		}

		@Test
		@DisplayName("If both ErrorDiscussions doesn't have a title that contains the first two blocks of the ErrorCode, it returns 0.")
		void testCompareSecondLevelMatch2() {
			ErrorDiscussion ed1 = createErrorDiscussion("Error 6HU3:12H1:0000",0, null);
			ErrorDiscussion ed2 = createErrorDiscussion("Error 6HU1:1221:0000",0, null);
			int result = errorController.compareSecondLevelMatch(ed1,ed2);
			Assertions.assertEquals(0, result);
		}

		@Test
		@DisplayName("If the first ErrorDiscussion has a title that contains the first two blocks of the ErrorCode and the second doesn't, it returns < 0.")
		void testCompareSecondLevelMatch3() {
			ErrorDiscussion ed1 = createErrorDiscussion("Error 6HU1:12H1:0000",0, null);
			ErrorDiscussion ed2 = createErrorDiscussion("Error 6HU1:1221:0000",0, null);
			int result = errorController.compareSecondLevelMatch(ed1,ed2);
			Assertions.assertTrue(result < 0);
		}
		@Test
		@DisplayName("If the second ErrorDiscussion has a title that contains the first two blocks of the ErrorCode and the first doesn't, it returns > 0.")
		void testCompareSecondLevelMatch4() {
			ErrorDiscussion ed1 = createErrorDiscussion("Error 6HU1:1211:0000",0, null);
			ErrorDiscussion ed2 = createErrorDiscussion("Error 6HU1:12H1:0000",0, null);
			int result = errorController.compareSecondLevelMatch(ed1,ed2);
			Assertions.assertTrue(result > 0);
		}
	}

	@Test
	@DisplayName("If both ErrorDiscussions has a title that contains the first block of the ErrorCode, it returns 0.")
	void testCompareThirdLevelMatch1() {
		Mockito.when(errorCode.methodCode()).thenReturn("6HU1");
		ErrorDiscussion ed1 = createErrorDiscussion("Error 6HU1:12H1:0000",0, null);
		ErrorDiscussion ed2 = createErrorDiscussion("Error 6HU1:1211:0000",0, null);
		int result = errorController.compareThirdLevelMatch(ed1,ed2);
		Assertions.assertEquals(0, result);
	}

	@Test
	@DisplayName("If both ErrorDiscussions doesn't have a title that contains the first block of the ErrorCode, it returns 0.")
	void testCompareThirdLevelMatch2() {
		Mockito.when(errorCode.methodCode()).thenReturn("6HU1");
		ErrorDiscussion ed1 = createErrorDiscussion("Error 6HUB:12H1:0000",0, null);
		ErrorDiscussion ed2 = createErrorDiscussion("Error 6HUA:1211:0000",0, null);
		int result = errorController.compareThirdLevelMatch(ed1,ed2);
		Assertions.assertEquals(0, result);
	}

	@Test
	@DisplayName("If the first ErrorDiscussion has a title that contains the first block of the ErrorCode and the second doesn't, it returns < 0.")
	void testCompareThirdLevelMatch3() {
		Mockito.when(errorCode.methodCode()).thenReturn("6HU1");
		ErrorDiscussion ed1 = createErrorDiscussion("Error 6HU1:12H1:0000",0, null);
		ErrorDiscussion ed2 = createErrorDiscussion("Error 6HUA:1211:0000",0, null);
		int result = errorController.compareThirdLevelMatch(ed1,ed2);
		Assertions.assertTrue(result < 0);
	}

	@Test
	@DisplayName("If the second ErrorDiscussion has a title that contains the first block of the ErrorCode and the first doesn't, it returns > 0.")
	void testCompareThirdLevelMatch4() {
		Mockito.when(errorCode.methodCode()).thenReturn("6HUA");
		ErrorDiscussion ed1 = createErrorDiscussion("Error 6HU1:12H1:0000",0, null);
		ErrorDiscussion ed2 = createErrorDiscussion("Error 6HUA:1211:0000",0, null);
		int result = errorController.compareThirdLevelMatch(ed1,ed2);
		Assertions.assertTrue(result > 0);
	}

	@Test
	@DisplayName("If the title of the ErrorDiscussion contains the first block of the ErrorCode, it returns true.")
	void testIsPartialMatchFilter1(){
		Mockito.when(errorCode.methodCode()).thenReturn("6HUA");
		ErrorDiscussion ed1 = createErrorDiscussion("Error 6HUA:12H1:0000",0, null);
		boolean result = errorController.isPartialMatchFilter(ed1);
		Assertions.assertTrue(result);
	}

	@Test
	@DisplayName("If the title of the ErrorDiscussion doesn't contains the first block of the ErrorCode, it returns false.")
	void testIsPartialMatchFilter2(){
		Mockito.when(errorCode.methodCode()).thenReturn("6HUA");
		ErrorDiscussion ed1 = createErrorDiscussion("Error 6HU1:12H1:0000",0, null);
		boolean result = errorController.isPartialMatchFilter(ed1);
		Assertions.assertFalse(result);
	}
}
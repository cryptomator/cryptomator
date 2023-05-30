package org.cryptomator.ui.error;

public class ErrorDiscussion {

	int upvoteCount;
	String title;
	String url;
	Answer answer;

	class Answer{
		private String url;
		private int upvoteCount;
	}

}

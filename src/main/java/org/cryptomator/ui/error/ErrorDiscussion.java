package org.cryptomator.ui.error;

public class ErrorDiscussion {

	String id;
	int upvoteCount;
	String title;
	String url;
	int comments;
	Answer answer;

	String getUpvoteCount(){return  upvoteCount+"";}

	String getErrorCode(){return title.substring(6);}
	String getMethodCode(){
		return title.substring(6,10);
	}
	String getRootCauseCode(){
		return title.substring(11,15);
	}

	String getThrowableCode(){
		return title.substring(16,20);
	}

	class Answer{
		private String url;
		private int upvoteCount;
	}

}

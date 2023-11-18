package org.cryptomator.ui.error;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorDiscussion {

	@JsonProperty
	int upvoteCount;
	@JsonProperty
	String title;
	@JsonProperty
	String url;
	@JsonProperty
	Answer answer;

	@JsonIgnoreProperties(ignoreUnknown = true)
	static class Answer {

	}

	public String getTitle(){
		return this.title;
	}
}

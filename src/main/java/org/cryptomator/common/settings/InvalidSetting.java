package org.cryptomator.common.settings;

import java.net.URI;
import java.util.Objects;
import java.util.ResourceBundle;

public enum InvalidSetting implements AbstractInvalidSetting {

	;

	private final static String DEFAULT_TRACKER = "https://github.com/cryptomator/cryptomator/issues/";
	private final static ResourceBundle BUNDLE = ResourceBundle.getBundle("i18n.strings");
	private final static String UNKNOWN_KEY_FORMAT = "<Unknown key: %s>";

	private final URI issue;
	private final String resourceKey;

	InvalidSetting(URI issue, String resourceKey) {
		this.issue = issue;
		this.resourceKey = Objects.requireNonNull(resourceKey);
	}

	InvalidSetting(int issueId, String resourceKey) {
		this(onDefaultTracker(issueId), Objects.requireNonNull(resourceKey));
	}

	@Override
	public URI getIssueURI() {
		return this.issue;
	}

	@Override
	public String getMessage() {
		if (!BUNDLE.containsKey(this.resourceKey)) {
			return UNKNOWN_KEY_FORMAT.formatted(this.resourceKey);
		}
		return BUNDLE.getString(this.resourceKey);
	}

	private static URI onDefaultTracker(int id) {
		return URI.create(DEFAULT_TRACKER + id);
	}
}
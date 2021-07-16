package org.cryptomator.common.settings;

import java.net.URI;
import java.util.Objects;
import java.util.ResourceBundle;

public enum InvalidSetting {

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

	/**
	 * Returns the corresponding issue URI of this setting.<br>
	 * The issue URI usually resolves to a page on the
	 * <a href="https://github.com/cryptomator/cryptomator/issues">Cryptomator Bugtracker.</a>
	 *
	 * @return the issue URI or {@code null} if none is provided.
	 */
	public URI getIssueURI() {
		return this.issue;
	}

	/**
	 * Returns a (preferably localized) message, that helps the user understand the
	 * issue in their configuration and how to fix it.
	 *
	 * @return the non-null description of the issue.
	 */
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
package org.cryptomator.common.settings;

import java.net.URI;

public interface AbstractInvalidSetting {

	/**
	 * Returns the corresponding issue URI of this setting.<br>
	 * The issue URI usually resolves to a page on the
	 * <a href="https://github.com/cryptomator/cryptomator/issues">Cryptomator Bugtracker.</a>
	 *
	 * @return the issue URI or {@code null} if none is provided.
	 */
	URI getIssueURI();

	/**
	 * Returns a (preferably localized) message, that helps the user understand the
	 * issue in their configuration and how to fix it.
	 *
	 * @return the non-null description of the issue.
	 */
	String getMessage();
}
package org.cryptomator.ui.importtemplate;

import java.io.IOException;

/**
 * Indicates that a vault template cannot be imported because the archive itself is unusable - as opposed to an
 * {@link IOException} arising from the destination (already exists, not writable, ...).
 * <p>
 * Callers distinguish the two to decide what to show the user: a malformed template is a dead end, whereas a
 * destination problem is recoverable by picking a different location.
 */
public class MalformedTemplateException extends IOException {

	private static final long serialVersionUID = 1L;

	public MalformedTemplateException(String message) {
		super(message);
	}

	public MalformedTemplateException(String message, Throwable cause) {
		super(message, cause);
	}
}

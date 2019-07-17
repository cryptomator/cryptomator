package org.cryptomator.common.settings;

import java.util.Arrays;

public enum WebDavUrlScheme {
	DAV("dav", "dav:// (Gnome, Nautilus, ...)"),
	WEBDAV("webdav", "webdav:// (KDE, Dolphin, ...)");

	private final String prefix;
	private final String displayName;

	WebDavUrlScheme(String prefix, String displayName) {this.prefix = prefix;
		this.displayName = displayName;
	}

	public String getPrefix() {
		return prefix;
	}

	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Finds a WebDavUrlScheme by prefix.
	 *
	 * @param prefix Prefix of the WebDavUrlScheme
	 * @return WebDavUrlScheme with the given <code>prefix</code>.
	 * @throws IllegalArgumentException if not WebDavUrlScheme with the given <code>prefix</code> was found.
	 */
	public static WebDavUrlScheme forPrefix(String prefix) throws IllegalArgumentException {
		return Arrays.stream(values()) //
				.filter(impl -> impl.prefix.equals(prefix)) //
				.findAny() //
				.orElseThrow(IllegalArgumentException::new);
	}
}

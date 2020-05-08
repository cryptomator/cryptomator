package org.cryptomator.common.settings;

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
}

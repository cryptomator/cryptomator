package org.cryptomator.frontend.webdav;

import static java.lang.String.format;
import static org.cryptomator.frontend.FrontendId.FRONTEND_ID_PATTERN;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cryptomator.frontend.FrontendId;

class ContextPaths {

	private static final Pattern SERVLET_PATH_WITH_FRONTEND_ID_PATTERN = Pattern.compile("^/(" + FRONTEND_ID_PATTERN + ")(/.*)?$");
	private static final int FRONTEND_ID_GROUP = 1;

	public static String from(FrontendId id, String name) {
		return format("/%s/%s", id, name);
	}

	public static String removeFrontendId(String path) {
		return path.replaceAll("/" + FRONTEND_ID_PATTERN + "/", "/[...]/");
	}

	public static Optional<FrontendId> extractFrontendId(String path) {
		Matcher matcher = SERVLET_PATH_WITH_FRONTEND_ID_PATTERN.matcher(path);
		if (matcher.matches()) {
			return Optional.of(FrontendId.from(matcher.group(FRONTEND_ID_GROUP)));
		} else {
			return Optional.empty();
		}
	}

}

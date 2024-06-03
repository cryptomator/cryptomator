package org.cryptomator.common;

import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SubstitutingProperties extends PropertiesDecorator {

	private static final Pattern TEMPLATE = Pattern.compile("@\\{(\\w+)}");

	private final Map<String, String> env;

	public SubstitutingProperties(Properties props, Map<String, String> systemEnvironment) {
		super(props);
		this.env = systemEnvironment;
	}

	@Override
	public String getProperty(String key) {
		var value = delegate.getProperty(key);
		if (key.startsWith("cryptomator.") && value != null) {
			return process(value);
		} else {
			return value;
		}
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		var result = getProperty(key);
		return result != null ? result : defaultValue;
	}

	@VisibleForTesting
	String process(String value) {
		return TEMPLATE.matcher(value).replaceAll(match -> //
				switch (match.group(1)) {
					case "appdir" -> resolveFrom("APPDIR", Source.ENV);
					case "appdata" -> resolveFrom("APPDATA", Source.ENV);
					case "localappdata" -> resolveFrom("LOCALAPPDATA", Source.ENV);
					case "userhome" -> resolveFrom("user.home", Source.PROPS);
					default -> {
						LoggerFactory.getLogger(SubstitutingProperties.class).warn("Unknown variable {} in property value {}.", match.group(), value);
						yield match.group();
					}
				});
	}

	private String resolveFrom(String key, Source src) {
		var val = switch (src) {
			case ENV -> env.get(key);
			case PROPS -> delegate.getProperty(key);
		};
		if (val == null) {
			LoggerFactory.getLogger(SubstitutingProperties.class).warn("Variable {} used for substitution not found in {}. Replaced with empty string.", key, src);
			return "";
		} else {
			return Matcher.quoteReplacement(val);
		}
	}

	private enum Source {
		ENV,
		PROPS;
	}

}

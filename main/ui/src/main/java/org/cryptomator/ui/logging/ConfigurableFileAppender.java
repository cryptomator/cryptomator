/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui.logging;

import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender;
import org.apache.logging.log4j.core.appender.FileManager;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.Booleans;
import org.apache.logging.log4j.util.Strings;

/**
 * A preconfigured FileAppender only relying on a configurable system property, e.g. <code>-Dcryptomator.logPath=/var/log/cryptomator.log</code>.<br/>
 * Other than the normal {@link org.apache.logging.log4j.core.appender.FileAppender} paths can be resolved relative to the users home directory.
 */
@Plugin(name = "ConfigurableFile", category = "Core", elementType = "appender", printObject = true)
public class ConfigurableFileAppender extends AbstractOutputStreamAppender<FileManager> {

	private static final long serialVersionUID = -6548221568069606389L;
	private static final int DEFAULT_BUFFER_SIZE = 8192;
	private static final Pattern DRIVE_LETTER_WITH_PRECEEDING_SLASH = Pattern.compile("^/[A-Z]:", Pattern.CASE_INSENSITIVE);

	protected ConfigurableFileAppender(String name, Layout<? extends Serializable> layout, Filter filter, FileManager manager) {
		super(name, layout, filter, true, true, manager);
		LOGGER.info("Logging to " + manager.getFileName());
	}

	@PluginFactory
	public static AbstractAppender createAppender(@PluginAttribute("name") final String name, @PluginAttribute("pathPropertyName") final String pathPropertyName, @PluginAttribute("append") final String append,
			@PluginElement("Layout") Layout<? extends Serializable> layout) {
		if (name == null) {
			LOGGER.error("No name provided for HomeDirectoryAwareFileAppender");
			return null;
		}

		if (pathPropertyName == null) {
			LOGGER.error("No pathPropertyName provided for HomeDirectoryAwareFileAppender with name " + name);
			return null;
		}

		final String fileName = System.getProperty(pathPropertyName);
		if (Strings.isEmpty(fileName)) {
			LOGGER.warn("No log file location provided in system property \"" + pathPropertyName + "\"");
			return null;
		}

		final Path filePath = parsePath(fileName);
		if (filePath == null) {
			LOGGER.warn("Invalid path \"" + fileName + "\"");
			return null;
		}

		if (!Files.exists(filePath.getParent())) {
			try {
				Files.createDirectories(filePath.getParent());
			} catch (IOException e) {
				LOGGER.error("Could not create parent directories for log file located at " + filePath.toString(), e);
				return null;
			}
		}

		final boolean shouldAppend = Booleans.parseBoolean(append, true);
		if (layout == null) {
			layout = PatternLayout.createDefaultLayout();
		}

		final FileManager manager = FileManager.getFileManager(filePath.toString(), shouldAppend, false, true, null, layout, DEFAULT_BUFFER_SIZE);
		return new ConfigurableFileAppender(name, layout, null, manager);
	}

	private static Path parsePath(String path) {
		if (path.startsWith("~/")) {
			// home-dir-relative Path:
			final Path userHome = FileSystems.getDefault().getPath(SystemUtils.USER_HOME);
			return userHome.resolve(path.substring(2));
		} else if (path.startsWith("/")) {
			// absolute Path:
			return FileSystems.getDefault().getPath(path);
		} else {
			// relative Path:
			try {
				String jarFileLocation = ConfigurableFileAppender.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
				if (SystemUtils.IS_OS_WINDOWS && DRIVE_LETTER_WITH_PRECEEDING_SLASH.matcher(jarFileLocation).find()) {
					// on windows we need to remove a preceeding slash from "/C:/foo/bar":
					jarFileLocation = jarFileLocation.substring(1);
				}
				final Path workingDir = FileSystems.getDefault().getPath(jarFileLocation).getParent();
				return workingDir.resolve(path);
			} catch (URISyntaxException e) {
				LOGGER.error("Unable to resolve working directory ", e);
				return null;
			}
		}
	}

}

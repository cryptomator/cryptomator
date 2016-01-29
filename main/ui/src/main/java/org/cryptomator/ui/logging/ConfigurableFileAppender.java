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
import org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender;
import org.apache.logging.log4j.core.appender.FileManager;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.util.Strings;

/**
 * A preconfigured FileAppender only relying on a configurable system property, e.g. <code>-DlogPath=/var/log/cryptomator.log</code>.<br/>
 * Other than the normal {@link org.apache.logging.log4j.core.appender.FileAppender} paths can be resolved relative to the users home directory.
 */
@Plugin(name = "ConfigurableFile", category = "Core", elementType = "appender", printObject = true)
public class ConfigurableFileAppender extends AbstractOutputStreamAppender<FileManager> {

	private static final long serialVersionUID = -6548221568069606389L;
	private static final int DEFAULT_BUFFER_SIZE = 8192;
	private static final String DEFAULT_FILE_NAME = "cryptomator.log";
	private static final Pattern DRIVE_LETTER_WITH_PRECEEDING_SLASH = Pattern.compile("^/[A-Z]:", Pattern.CASE_INSENSITIVE);

	protected ConfigurableFileAppender(String name, Layout<? extends Serializable> layout, Filter filter, FileManager manager) {
		super(name, layout, filter, true, true, manager);
		LOGGER.info("Logging to " + manager.getFileName());
	}

	@PluginFactory
	public static ConfigurableFileAppender createAppender(@PluginAttribute("name") final String name, @PluginAttribute("pathPropertyName") final String pathPropertyName,
			@PluginElement("Layout") Layout<? extends Serializable> layout) {

		if (name == null) {
			LOGGER.error("No name provided for HomeDirectoryAwareFileAppender");
			return null;
		}

		if (pathPropertyName == null) {
			LOGGER.error("No pathPropertyName provided for HomeDirectoryAwareFileAppender with name " + name);
			return null;
		}

		String fileName = System.getProperty(pathPropertyName);
		if (Strings.isEmpty(fileName)) {
			fileName = DEFAULT_FILE_NAME;
		}

		final Path filePath;
		if (fileName.startsWith("~/")) {
			// home-dir-relative Path:
			final Path userHome = FileSystems.getDefault().getPath(SystemUtils.USER_HOME);
			filePath = userHome.resolve(fileName.substring(2));
		} else if (fileName.startsWith("/")) {
			// absolute Path:
			filePath = FileSystems.getDefault().getPath(fileName);
		} else if (SystemUtils.IS_OS_WINDOWS && fileName.startsWith("%appdata%/")) {
			final String appdata = System.getenv("APPDATA");
			final Path appdataPath = appdata != null ? FileSystems.getDefault().getPath(appdata) : FileSystems.getDefault().getPath(SystemUtils.USER_HOME);
			filePath = appdataPath.resolve(fileName.substring(10));
		} else {
			// relative Path:
			try {
				String jarFileLocation = ConfigurableFileAppender.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
				if (SystemUtils.IS_OS_WINDOWS && DRIVE_LETTER_WITH_PRECEEDING_SLASH.matcher(jarFileLocation).find()) {
					// on windows we need to remove a preceeding slash from "/C:/foo/bar":
					jarFileLocation = jarFileLocation.substring(1);
				}
				final Path workingDir = FileSystems.getDefault().getPath(jarFileLocation).getParent();
				filePath = workingDir.resolve(fileName);
			} catch (URISyntaxException e) {
				LOGGER.error("Unable to resolve working directory ", e);
				return null;
			}
		}

		if (layout == null) {
			layout = PatternLayout.createDefaultLayout();
		}

		if (!Files.exists(filePath.getParent())) {
			try {
				Files.createDirectories(filePath.getParent());
			} catch (IOException e) {
				LOGGER.error("Could not create parent directories for log file located at " + filePath.toString(), e);
				return null;
			}
		}

		final FileManager manager = FileManager.getFileManager(filePath.toString(), false, false, true, null, layout, DEFAULT_BUFFER_SIZE);
		return new ConfigurableFileAppender(name, layout, null, manager);
	}

}

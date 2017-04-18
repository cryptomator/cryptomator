/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.logging;

import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender;
import org.apache.logging.log4j.core.appender.FileManager;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.util.Strings;

/**
 * A preconfigured FileAppender only relying on a configurable system property, e.g. <code>-Dcryptomator.logPath=/var/log/cryptomator.log</code>.<br/>
 * Other than the normal {@link org.apache.logging.log4j.core.appender.FileAppender} paths can be resolved relative to the users home directory.
 */
@Plugin(name = ConfigurableFileAppender.PLUGIN_NAME, category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class ConfigurableFileAppender extends AbstractOutputStreamAppender<FileManager> {

	static final String PLUGIN_NAME = "ConfigurableFile";
	private static final Pattern DRIVE_LETTER_WITH_PRECEEDING_SLASH = Pattern.compile("^/[A-Z]:", Pattern.CASE_INSENSITIVE);

	private ConfigurableFileAppender(String name, Layout<? extends Serializable> layout, Filter filter, boolean ignoreExceptions, boolean immediateFlush, FileManager manager) {
		super(name, layout, filter, ignoreExceptions, immediateFlush, manager);
		LOGGER.info("Logging to " + manager.getFileName());
	}

	@PluginBuilderFactory
	public static <B extends Builder<B>> B newBuilder() {
		return new Builder<B>().asBuilder();
	}

	/**
	 * Builds ConfigurableFileAppender instances.
	 * 
	 * @param <B>
	 *            The type to build
	 */
	public static class Builder<B extends Builder<B>> extends AbstractOutputStreamAppender.Builder<B> //
			implements org.apache.logging.log4j.core.util.Builder<ConfigurableFileAppender> {

		@Required(message = "No system property name containing the log file path provided.")
		@PluginBuilderAttribute("pathPropertyName")
		private String pathPropertyName;

		@PluginBuilderAttribute
		private boolean append = true;

		@Override
		public ConfigurableFileAppender build() {
			final String pathProperty = System.getProperty(pathPropertyName);
			if (Strings.isEmpty(pathProperty)) {
				LOGGER.warn("No log file location provided in system property \"" + pathPropertyName + "\"");
				return null;
			}

			final Path filePath = parsePath(pathProperty);
			if (filePath == null) {
				LOGGER.warn("Invalid path \"" + pathProperty + "\"");
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

			FileManager manager = FileManager.getFileManager(filePath.toString(), append, false, isBufferedIo(), true, null, getOrCreateLayout(), getBufferSize(), getConfiguration());
			return new ConfigurableFileAppender(getName(), getLayout(), getFilter(), isIgnoreExceptions(), isImmediateFlush(), manager);
		}

		public B withPathPropertyName(String pathPropertyName) {
			this.pathPropertyName = pathPropertyName;
			return asBuilder();
		}

		public B withAppend(boolean append) {
			this.append = append;
			return asBuilder();
		}

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

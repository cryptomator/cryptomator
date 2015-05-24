package org.cryptomator.ui.logging;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

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

@Plugin(name = "HomeDirectoryAwareFile", category = "Core", elementType = "appender", printObject = true)
public class HomeDirectoryAwareFileAppender extends AbstractOutputStreamAppender<FileManager> {

	private static final long serialVersionUID = -6548221568069606389L;
	private static final int DEFAULT_BUFFER_SIZE = 8192;

	protected HomeDirectoryAwareFileAppender(String name, Layout<? extends Serializable> layout, Filter filter, FileManager manager) {
		super(name, layout, filter, true, true, manager);
		LOGGER.warn("Logging to " + manager.getFileName());
	}

	@PluginFactory
	public static HomeDirectoryAwareFileAppender createAppender(@PluginAttribute("name") final String name, @PluginAttribute("fileName") final String fileName,
			@PluginElement("Layout") Layout<? extends Serializable> layout) {

		if (name == null) {
			LOGGER.error("No name provided for FileAppender");
			return null;
		}

		final Path filePath;
		if (fileName == null) {
			LOGGER.error("No filename provided for FileAppender with name " + name);
			return null;
		} else if (fileName.startsWith("~/")) {
			// home-dir-relative Path:
			final Path userHome = FileSystems.getDefault().getPath(SystemUtils.USER_HOME);
			filePath = userHome.resolve(fileName.substring(2));
		} else if (fileName.startsWith("/")) {
			// absolute Path:
			filePath = FileSystems.getDefault().getPath(fileName);
		} else {
			// relative Path:
			try {
				final URI jarFileLocation = HomeDirectoryAwareFileAppender.class.getProtectionDomain().getCodeSource().getLocation().toURI();
				final Path workingDir = FileSystems.getDefault().getPath(jarFileLocation.getPath()).getParent();
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
		return new HomeDirectoryAwareFileAppender(name, layout, null, manager);
	}

}

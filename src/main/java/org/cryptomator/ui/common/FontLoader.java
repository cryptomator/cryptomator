package org.cryptomator.ui.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.text.Font;
import java.io.IOException;
import java.io.InputStream;

public class FontLoader {

	private static final Logger LOG = LoggerFactory.getLogger(FontLoader.class);
	private static final double DEFAULT_FONT_SIZE = 12;

	public static Font load(String resourcePath) throws FontLoaderException {
		try (InputStream in = FontLoader.class.getResourceAsStream(resourcePath)) {
			if (in == null) {
				throw new FontLoaderException(resourcePath);
			} else {
				return load(resourcePath, in);
			}
		} catch (IOException e) {
			throw new FontLoaderException(resourcePath, e);
		}
	}

	private static Font load(String resourcePath, InputStream in) throws FontLoaderException {
		Font font = Font.loadFont(in, DEFAULT_FONT_SIZE);
		if (font != null) {
			LOG.debug("Loaded family: {}", font.getFamily());
			return font;
		} else {
			throw new FontLoaderException(resourcePath);
		}
	}

	public static class FontLoaderException extends IOException {

		private FontLoaderException(String resourceName) {
			super("Failed to load font: " + resourceName);
		}

		private FontLoaderException(String resourceName, Throwable cause) {
			super("Failed to load font: " + resourceName, cause);
		}

	}

}

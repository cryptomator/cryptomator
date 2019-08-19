package org.cryptomator.ui.controls;

import com.google.common.base.Preconditions;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Inspired by de.jensd:fontawesomefx-fontawesome
 */
public class FontAwesome5IconView extends Text {

	private static final Logger LOG = LoggerFactory.getLogger(FontAwesome5IconView.class);
	private static final FontAwesome5Icon DEFAULT_GLYPH = FontAwesome5Icon.ANCHOR;
	private static final double DEFAULT_GLYPH_SIZE = 12.0;
	private static final String FONT_PATH = "/css/fontawesome5-pro-solid.otf";
	private static final Font FONT;

	private ObjectProperty<FontAwesome5Icon> glyph = new SimpleObjectProperty<>(this, "glyph", DEFAULT_GLYPH);
	private DoubleProperty glyphSize = new SimpleDoubleProperty(this, "glyphSize", DEFAULT_GLYPH_SIZE);

	static {
		try (InputStream in = FontAwesome5IconView.class.getResourceAsStream(FONT_PATH)) {
			Preconditions.checkNotNull(in, "Resource not found: " + FONT_PATH);
			FONT = Font.loadFont(in, DEFAULT_GLYPH_SIZE);
			if (FONT != null) {
				LOG.debug("Loaded family: {}", FONT.getFamily());
			} else {
				throw new IllegalStateException("Failed to load font.");
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public FontAwesome5IconView() {
		getStyleClass().addAll("glyph-icon");
		glyphProperty().addListener(this::glyphChanged);
		glyphSizeProperty().addListener(this::glyphSizeChanged);
		setFont(FONT);
		setGlyph(DEFAULT_GLYPH);
		setGlyphSize(DEFAULT_GLYPH_SIZE);
	}

	private void glyphChanged(@SuppressWarnings("unused") ObservableValue<? extends FontAwesome5Icon> observable, @SuppressWarnings("unused") FontAwesome5Icon oldValue, FontAwesome5Icon newValue) {
		setText(newValue.unicode());
	}

	private void glyphSizeChanged(@SuppressWarnings("unused") ObservableValue<? extends Number> observable, @SuppressWarnings("unused") Number oldValue, Number newValue) {
		setFont(new Font(FONT.getFamily(), newValue.doubleValue()));
	}

	/* Getter/Setter */

	public ObjectProperty<FontAwesome5Icon> glyphProperty() {
		return glyph;
	}

	public void setGlyph(FontAwesome5Icon glyph) {
		this.glyph.set(glyph == null ? DEFAULT_GLYPH : glyph);
	}

	public FontAwesome5Icon getGlyph() {
		return glyph.get();
	}

	public DoubleProperty glyphSizeProperty() {
		return glyphSize;
	}

	public void setGlyphSize(double glyphSize) {
		this.glyphSize.set(glyphSize);
	}

	public double getGlyphSize() {
		return glyphSize.get();
	}
}

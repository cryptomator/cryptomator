package org.cryptomator.ui.controls;

import org.cryptomator.ui.common.FontLoader;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import java.io.UncheckedIOException;

/**
 * Inspired by de.jensd:fontawesomefx-fontawesome
 */
public class FontAwesome5IconView extends Text {

	private static final FontAwesome5Icon DEFAULT_GLYPH = FontAwesome5Icon.ANCHOR;
	private static final double DEFAULT_GLYPH_SIZE = 12.0;
	private static final String FONT_PATH = "/css/fontawesome5-free-solid.otf";
	private static final Font FONT;

	private ObjectProperty<FontAwesome5Icon> glyph = new SimpleObjectProperty<>(this, "glyph", DEFAULT_GLYPH);
	private DoubleProperty glyphSize = new SimpleDoubleProperty(this, "glyphSize", DEFAULT_GLYPH_SIZE);

	static {
		try {
			FONT = FontLoader.load(FONT_PATH);
		} catch (FontLoader.FontLoaderException e) {
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

	public FontAwesome5Icon getGlyph() {
		return glyph.get();
	}

	public void setGlyph(FontAwesome5Icon glyph) {
		this.glyph.set(glyph == null ? DEFAULT_GLYPH : glyph);
	}

	public DoubleProperty glyphSizeProperty() {
		return glyphSize;
	}

	public double getGlyphSize() {
		return glyphSize.get();
	}

	public void setGlyphSize(double glyphSize) {
		this.glyphSize.set(glyphSize);
	}
}

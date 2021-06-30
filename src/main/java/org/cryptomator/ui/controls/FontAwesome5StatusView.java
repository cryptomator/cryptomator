package org.cryptomator.ui.controls;

import com.tobiasdiez.easybind.EasyBind;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;

/**
 * Similar to the {@link FontAwesome5IconView}, except that if the spinner glyph is selected, an animated facsimile is used.
 */
public class FontAwesome5StatusView extends Label {

	private static final double DEFAULT_GLYPH_SIZE = 12.0;
	private static final FontAwesome5Icon DEFAULT_GLYPH = FontAwesome5Icon.ANCHOR;

	private FontAwesome5IconView staticIcon;
	private FontAwesome5Spinner animatedSpinner;

	private ObjectProperty<FontAwesome5Icon> glyph = new SimpleObjectProperty<>(this, "glyph", DEFAULT_GLYPH);
	private DoubleProperty glyphSize = new SimpleDoubleProperty(this, "glyphSize", DEFAULT_GLYPH_SIZE);

	public FontAwesome5StatusView() {
		this.staticIcon = new FontAwesome5IconView();
		this.animatedSpinner = new FontAwesome5Spinner();
		setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

		staticIcon.glyphProperty().bind(glyph);
		staticIcon.glyphSizeProperty().bind(glyphSize);
		animatedSpinner.glyphSizeProperty().bind(glyphSize);

		EasyBind.subscribe(glyphProperty(), this::spinnerOrIcon);
		EasyBind.subscribe(glyphSize, this::shrinkToGlyphSize);
	}

	private void shrinkToGlyphSize(Number newValue) {
		double sizeInPx = newValue.doubleValue() * 1.333;
		setMaxSize(sizeInPx, sizeInPx);
	}

	private void spinnerOrIcon(FontAwesome5Icon icon) {
		if (icon == FontAwesome5Icon.SPINNER) {
			this.setGraphic(animatedSpinner);
		} else {
			this.setGraphic(staticIcon);
		}
	}

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

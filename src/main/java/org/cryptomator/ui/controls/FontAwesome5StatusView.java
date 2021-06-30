package org.cryptomator.ui.controls;

import com.tobiasdiez.easybind.EasyBind;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;

/**
 * Similar to the {@link FontAwesome5IconView}, except that if the spinner glyph is selected, an animated facsimile is used.
 */
public class FontAwesome5StatusView extends StackPane {

	private static final double DEFAULT_GLYPH_SIZE = 12.0;
	private static final FontAwesome5Icon DEFAULT_GLYPH = FontAwesome5Icon.ANCHOR;

	private final FontAwesome5IconView staticIcon;
	private final FontAwesome5Spinner animatedSpinner;
	private final BooleanBinding isSpinnerGlyph;

	private final ObjectProperty<FontAwesome5Icon> glyph = new SimpleObjectProperty<>(this, "glyph", DEFAULT_GLYPH);
	private final DoubleProperty glyphSize = new SimpleDoubleProperty(this, "glyphSize", DEFAULT_GLYPH_SIZE);


	public FontAwesome5StatusView() {
		this.staticIcon = new FontAwesome5IconView();
		this.animatedSpinner = new FontAwesome5Spinner();
		this.isSpinnerGlyph = glyphProperty().isEqualTo(FontAwesome5Icon.SPINNER);
		setAlignment(Pos.CENTER);
		getChildren().addAll(staticIcon, animatedSpinner);

		staticIcon.glyphProperty().bind(glyph);
		staticIcon.glyphSizeProperty().bind(glyphSize);
		animatedSpinner.glyphSizeProperty().bind(glyphSize);

		EasyBind.subscribe(isSpinnerGlyph, this::showSpinner);
	}

	private void showSpinner(boolean isSpinner) {
		animatedSpinner.setVisible(isSpinner);
		staticIcon.setVisible(!isSpinner);
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

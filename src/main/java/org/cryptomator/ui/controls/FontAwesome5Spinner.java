package org.cryptomator.ui.controls;

import com.tobiasdiez.easybind.EasyBind;

import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.ProgressIndicator;
import javafx.util.Duration;

/**
 * A progress indicator in the shape of {@link FontAwesome5Icon#SPINNER}. The single spinner segements are defined in the css in the `progress-indicator` class.
 * <p>
 * See also https://docs.oracle.com/javase/8/javafx/api/javafx/scene/doc-files/cssref.html#progressindicator
 */
public class FontAwesome5Spinner extends ProgressIndicator {

	private static final double DEFAULT_GLYPH_SIZE = 12.0;

	private final FontAwesome5IconView boundingBox;
	private final RotateTransition animation;
	private final DoubleProperty glyphSize = new SimpleDoubleProperty(this, "glyphSize", DEFAULT_GLYPH_SIZE);

	public FontAwesome5Spinner() {
		this.boundingBox = new FontAwesome5IconView();
		boundingBox.setGlyph(FontAwesome5Icon.SPINNER);
		boundingBox.glyphSizeProperty().bind(glyphSize);

		this.animation = new RotateTransition(Duration.millis(100), this);
		animation.setInterpolator(Interpolator.DISCRETE);
		animation.setByAngle(45);
		animation.setCycleCount(1);
		animation.setOnFinished(event -> {
			double currentAngle = this.getRotate();
			animation.setFromAngle(currentAngle);
			animation.play();
		});

		EasyBind.subscribe(this.visibleProperty(), this::reset);
		EasyBind.subscribe(boundingBox.glyphSizeProperty(), this::shrinkToGlyphSize);
	}

	private void shrinkToGlyphSize(Number newValue) {
		setMaxSize(boundingBox.getBoundsInLocal().getWidth(), boundingBox.getBoundsInLocal().getHeight());
	}

	private void reset(boolean flag) {
		if (flag) {
			setRotate(0);
			animation.playFromStart();
		} else {
			animation.stop();
		}
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

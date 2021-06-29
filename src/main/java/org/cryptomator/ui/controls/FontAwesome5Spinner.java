package org.cryptomator.ui.controls;

import com.tobiasdiez.easybind.EasyBind;

import javafx.animation.AnimationTimer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.ProgressIndicator;
import java.time.Duration;

/**
 * A progress indicator in the shape of {@link FontAwesome5Icon#SPINNER}. The single spinner segements are defined in the css in the `progress-indicator` class.
 * <p>
 * See also https://docs.oracle.com/javase/8/javafx/api/javafx/scene/doc-files/cssref.html#progressindicator
 */
public class FontAwesome5Spinner extends ProgressIndicator {

	private static final double DEFAULT_GLYPH_SIZE = 12.0;

	private final Animator animation;
	private DoubleProperty glyphSize = new SimpleDoubleProperty(this, "glyphSize", DEFAULT_GLYPH_SIZE);

	public FontAwesome5Spinner() {
		this.animation = new Animator(this);
		EasyBind.subscribe(this.visibleProperty(), this::startStopAnimation);
		EasyBind.subscribe(glyphSize, this::shrinkToGlyphSize);
	}

	private void shrinkToGlyphSize(Number newValue) {
		double sizeInPx	= newValue.doubleValue() * 1.333;
		setMaxSize(sizeInPx,sizeInPx);
	}

	private void startStopAnimation(boolean flag) {
		if (flag) {
			animation.start();
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

	private static class Animator extends AnimationTimer {

		private static final long STATIC_TIMEFRAME = Duration.ofMillis(100).toNanos();
		//defined in css!
		private static int SEGMENT_COUNT = 8;
		private static final int ROTATION_ANGLE = 360 / SEGMENT_COUNT;

		private final ProgressIndicator toRotate;

		private long lastChange = 0;
		private int rotation_count = 0;

		Animator(ProgressIndicator toRotate) {
			this.toRotate = toRotate;
		}

		@Override
		public void handle(long now) {
			if (now - lastChange > STATIC_TIMEFRAME) {
				lastChange = now;

				toRotate.setRotate(ROTATION_ANGLE * rotation_count);

				rotation_count++;
				if (rotation_count == SEGMENT_COUNT) {
					rotation_count = 0;
				}
			}
		}
	}

}

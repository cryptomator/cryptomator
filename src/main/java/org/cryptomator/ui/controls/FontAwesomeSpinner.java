package org.cryptomator.ui.controls;

import com.tobiasdiez.easybind.EasyBind;

import javafx.animation.AnimationTimer;
import javafx.scene.control.ProgressIndicator;
import java.time.Duration;

/**
 * A progress indicator in the shape of {@link FontAwesome5Icon#SPINNER}. The single spinner segements are defined in the css in the `progress-indicator` class.
 * <p>
 * See also https://docs.oracle.com/javase/8/javafx/api/javafx/scene/doc-files/cssref.html#progressindicator
 */
public class FontAwesomeSpinner extends ProgressIndicator {

	private final Animator animation;

	public FontAwesomeSpinner() {
		this.animation = new Animator(this);
		EasyBind.subscribe(this.visibleProperty(), this::startStopAnimation);
	}

	private void startStopAnimation(boolean flag) {
		if (flag) {
			animation.start();
		} else {
			animation.stop();
		}
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

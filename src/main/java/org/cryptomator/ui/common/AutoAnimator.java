package org.cryptomator.ui.common;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;

import javafx.animation.Animation;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;

/**
 * Animation which starts and stops automatically based on an observable condition.
 * <p>
 * During creation the consumer can optionally define actions to be executed everytime before the animation starts and after it stops.
 */
public class AutoAnimator {

	private final Animation animation;
	private final ObservableValue<Boolean> condition;
	private final Runnable beforeStart;
	private final Runnable afterStop;
	private final Subscription sub;

	AutoAnimator(Animation animation, ObservableValue<Boolean> condition, Runnable beforeStart, Runnable afterStop) {
		this.animation = animation;
		this.condition = condition;
		this.beforeStart = beforeStart;
		this.afterStop = afterStop;
		this.sub = EasyBind.subscribe(condition, this::togglePlay);
	}

	public void playFromStart() {
		beforeStart.run();
		animation.playFromStart();
	}

	public void stop() {
		animation.stop();
		afterStop.run();
	}

	private void togglePlay(boolean play) {
		if (play) {
			this.playFromStart();
		} else {
			this.stop();
		}
	}

	public static Builder animate(Animation animation) {
		return new Builder(animation);
	}

	public static class Builder {

		private final Animation animation;
		private ObservableValue<Boolean> condition = new SimpleBooleanProperty(true);
		private Runnable beforeStart = () -> {};
		private Runnable afterStop = () -> {};

		private Builder(Animation animation) {
			this.animation = animation;
		}

		public Builder onCondition(ObservableValue<Boolean> condition) {
			this.condition = condition;
			return this;
		}

		public Builder beforeStart(Runnable beforeStart) {
			this.beforeStart = beforeStart;
			return this;
		}

		public Builder afterStop(Runnable afterStop) {
			this.afterStop = afterStop;
			return this;
		}

		public AutoAnimator build() {
			return new AutoAnimator(animation, condition, beforeStart, afterStop);
		}

	}
}

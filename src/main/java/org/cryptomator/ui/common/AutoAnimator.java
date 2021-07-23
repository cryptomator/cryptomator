package org.cryptomator.ui.common;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;

import javafx.animation.Animation;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;

/**
 * TODO: doc doc doc
 * 		-- the duckumentation dock
 *                __
 *              <(o )___
 *               ( ._> /
 *                `---'   hjw
 */
public class AutoAnimator<T extends Animation> {

	private final T animation;
	private final ObservableValue<Boolean> condition;
	private final Runnable beforeStart;
	private final Runnable afterStop;

	private Subscription sub;

	AutoAnimator(T animation, ObservableValue<Boolean> condition, Runnable beforeStart, Runnable afterStop) {
		this.animation = animation;
		this.condition = condition;
		this.beforeStart = beforeStart;
		this.afterStop = afterStop;

		activateCondition();
	}

	public void playFromStart() {
		beforeStart.run();
		animation.playFromStart();
	}

	public void stop() {
		animation.stop();
		afterStop.run();
	}

	/**
	 * Deactivates activation on the condition.
	 * No-op if condition is already deactivated.
	 */
	public void deactivateCondition() {
		if (sub != null) {
			sub.unsubscribe();
		}
	}

	/**
	 * Activates the condition
	 * No-op if condition is already activated.
	 */
	public void activateCondition() {
		if (sub == null) {
			this.sub = EasyBind.subscribe(condition, this::togglePlay);
		}
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

		private Animation animation;
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

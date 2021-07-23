package org.cryptomator.ui.controls;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;
import org.cryptomator.ui.common.Animations;

import javafx.animation.Animation;

/**
 * TODO: mit Sebi über SInn sprechen.
 *  Es sollte ursrünglich _keine_ neue Klasse definiert werden.
 */
public class AnimatedSpinnerView extends FontAwesome5IconView {

	public final Animation discrete360Rotation;
	private final Subscription rotateSubscription;

	public AnimatedSpinnerView() {
		this.discrete360Rotation = Animations.createDiscrete360Rotation(this);
		this.rotateSubscription = EasyBind.subscribe(visibleProperty(), isVisible -> {
			if (isVisible) {
				discrete360Rotation.playFromStart();
			} else {
				discrete360Rotation.stop();
				this.setRotate(0);
			}
		});
	}

}

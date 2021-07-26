package org.cryptomator.ui.controls;

import org.cryptomator.ui.common.Animations;
import org.cryptomator.ui.common.AutoAnimator;

import javafx.beans.NamedArg;
import javafx.beans.value.ObservableValue;

/**
 * An animated progress spinner using the {@link FontAwesome5IconView} with the spinner glyph.
 *
 * By default, the animation is always played if the icon is visible. To animate on other conditions, use the "spinning" property.
 */
public class FontAwesome5Spinner extends FontAwesome5IconView{

	private final AutoAnimator animator;

	public FontAwesome5Spinner(@NamedArg("spinning") ObservableValue<Boolean> spinning) {
		setGlyph(FontAwesome5Icon.SPINNER);
		var animation = Animations.createDiscrete360Rotation(this);
		this.animator = AutoAnimator.animate(animation)
				.afterStop(() -> setRotate(0))
				.onCondition(spinning == null? visibleProperty():spinning)
				.build();
	}

}

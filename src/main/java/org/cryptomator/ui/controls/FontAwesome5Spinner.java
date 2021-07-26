package org.cryptomator.ui.controls;

import org.cryptomator.ui.common.Animations;
import org.cryptomator.ui.common.AutoAnimator;

import javafx.beans.NamedArg;
import javafx.beans.value.ObservableValue;
import java.util.Optional;

/**
 * An animated progress spinner using the {@link FontAwesome5IconView} with the spinner glyph.
 * <p>
 * Using the default constructor, the animation is always played if the icon is visible. To animate on other conditions, use the constructor with the "spinning" property.
 */
public class FontAwesome5Spinner extends FontAwesome5IconView {

	private AutoAnimator animator;

	public FontAwesome5Spinner() {
		new FontAwesome5Spinner(Optional.empty());
	}

	public FontAwesome5Spinner(@NamedArg("spinning") ObservableValue<Boolean> spinning) {
		new FontAwesome5Spinner(Optional.of(spinning));
	}

	private FontAwesome5Spinner(Optional<ObservableValue<Boolean>> animateCondition) {
		setGlyph(FontAwesome5Icon.SPINNER);
		var animation = Animations.createDiscrete360Rotation(this);
		this.animator = AutoAnimator.animate(animation) //
				.afterStop(() -> setRotate(0)) //
				.onCondition(animateCondition.orElse(visibleProperty())) //
				.build();
	}

}

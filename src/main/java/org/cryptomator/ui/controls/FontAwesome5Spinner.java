package org.cryptomator.ui.controls;

import org.cryptomator.ui.common.Animations;
import org.cryptomator.ui.common.AutoAnimator;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * An animated progress spinner using the {@link FontAwesome5IconView} with the spinner glyph.
 * <p>
 * Using the default constructor, the animation is always played if the icon is visible. To animate on other conditions, use the constructor with the "spinning" property.
 */
public class FontAwesome5Spinner extends FontAwesome5IconView {

	protected final BooleanProperty spinning = new SimpleBooleanProperty(this, "spinning", true);

	private AutoAnimator animator;

	public FontAwesome5Spinner() {
		setGlyph(FontAwesome5Icon.SPINNER);
		var animation = Animations.createDiscrete360Rotation(this);
		this.animator = AutoAnimator.animate(animation) //
				.afterStop(() -> setRotate(0)) //
				.onCondition(spinning.and(visibleProperty())) //
				.build();
	}

	/* Getter/Setter */

	public BooleanProperty spinningProperty() {
		return spinning;
	}

	public boolean isSpinning() {
		return spinning.get();
	}

	public void setSpinning(boolean spinning) {
		this.spinning.set(spinning);
	}

	// using the parent class method here to resolve the Broken Hierarchy code smell
	@Override
	public void setGlyph(FontAwesome5Icon glyph) {
		super.setGlyph(glyph);
	}
}

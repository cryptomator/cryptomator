package org.cryptomator.ui.controls;

import com.tobiasdiez.easybind.EasyBind;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class PasswordStrengthIndicator extends HBox {

	static final String STYLECLASS = "password-strength-indicator";
	static final String SEGMENT_CLASS = "segment";
	static final String ACTIVE_SEGMENT_CLASS = "active";
	static final String STRENGTH_0_CLASS = "strength-0";
	static final String STRENGTH_1_CLASS = "strength-1";
	static final String STRENGTH_2_CLASS = "strength-2";
	static final String STRENGTH_3_CLASS = "strength-3";
	static final String STRENGTH_4_CLASS = "strength-4";

	private final Region s0;
	private final Region s1;
	private final Region s2;
	private final Region s3;
	private final Region s4;
	private final IntegerProperty strength = new SimpleIntegerProperty();
	private final BooleanBinding isStrength0 = strength.isEqualTo(0);
	private final BooleanBinding isStrength1 = strength.isEqualTo(1);
	private final BooleanBinding isStrength2 = strength.isEqualTo(2);
	private final BooleanBinding isStrength3 = strength.isEqualTo(3);
	private final BooleanBinding isStrength4 = strength.isEqualTo(4);
	private final BooleanBinding isMinimumStrength0 = strength.greaterThanOrEqualTo(0);
	private final BooleanBinding isMinimumStrength1 = strength.greaterThanOrEqualTo(1);
	private final BooleanBinding isMinimumStrength2 = strength.greaterThanOrEqualTo(2);
	private final BooleanBinding isMinimumStrength3 = strength.greaterThanOrEqualTo(3);
	private final BooleanBinding isMinimumStrength4 = strength.greaterThanOrEqualTo(4);

	public PasswordStrengthIndicator() {
		this.s0 = new Region();
		this.s1 = new Region();
		this.s2 = new Region();
		this.s3 = new Region();
		this.s4 = new Region();

		getChildren().addAll(s0, s1, s2, s3, s4);
		setHgrow(s0, Priority.ALWAYS);
		setHgrow(s1, Priority.ALWAYS);
		setHgrow(s2, Priority.ALWAYS);
		setHgrow(s3, Priority.ALWAYS);
		setHgrow(s4, Priority.ALWAYS);

		getStyleClass().add(STYLECLASS);
		s0.getStyleClass().add(SEGMENT_CLASS);
		s1.getStyleClass().add(SEGMENT_CLASS);
		s2.getStyleClass().add(SEGMENT_CLASS);
		s3.getStyleClass().add(SEGMENT_CLASS);
		s4.getStyleClass().add(SEGMENT_CLASS);

		EasyBind.includeWhen(s0.getStyleClass(), ACTIVE_SEGMENT_CLASS, isMinimumStrength0);
		EasyBind.includeWhen(s1.getStyleClass(), ACTIVE_SEGMENT_CLASS, isMinimumStrength1);
		EasyBind.includeWhen(s2.getStyleClass(), ACTIVE_SEGMENT_CLASS, isMinimumStrength2);
		EasyBind.includeWhen(s3.getStyleClass(), ACTIVE_SEGMENT_CLASS, isMinimumStrength3);
		EasyBind.includeWhen(s4.getStyleClass(), ACTIVE_SEGMENT_CLASS, isMinimumStrength4);
		EasyBind.includeWhen(getStyleClass(), STRENGTH_0_CLASS, isStrength0);
		EasyBind.includeWhen(getStyleClass(), STRENGTH_1_CLASS, isStrength1);
		EasyBind.includeWhen(getStyleClass(), STRENGTH_2_CLASS, isStrength2);
		EasyBind.includeWhen(getStyleClass(), STRENGTH_3_CLASS, isStrength3);
		EasyBind.includeWhen(getStyleClass(), STRENGTH_4_CLASS, isStrength4);
	}

	/* Observables */

	public IntegerProperty strengthProperty() {
		return strength;
	}

	public int getStrength() {
		return strength.get();
	}

	public void setStrength(int strength) {
		this.strength.set(strength);
	}
}

package org.cryptomator.ui.common;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.RotateTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.scene.Node;
import javafx.stage.Window;
import javafx.util.Duration;
import java.util.stream.IntStream;

public class Animations {

	public static Timeline createShakeWindowAnimation(Window window) {
		WritableValue<Double> writableWindowX = new WritableValue<>() {
			@Override
			public Double getValue() {
				return window.getX();
			}

			@Override
			public void setValue(Double value) {
				window.setX(value);
			}
		};
		return new Timeline( //
				new KeyFrame(Duration.ZERO, new KeyValue(writableWindowX, window.getX())), //
				new KeyFrame(new Duration(100), new KeyValue(writableWindowX, window.getX() - 22.0)), //
				new KeyFrame(new Duration(200), new KeyValue(writableWindowX, window.getX() + 18.0)), //
				new KeyFrame(new Duration(300), new KeyValue(writableWindowX, window.getX() - 14.0)), //
				new KeyFrame(new Duration(400), new KeyValue(writableWindowX, window.getX() + 10.0)), //
				new KeyFrame(new Duration(500), new KeyValue(writableWindowX, window.getX() - 6.0)), //
				new KeyFrame(new Duration(600), new KeyValue(writableWindowX, window.getX() + 2.0)), //
				new KeyFrame(new Duration(700), new KeyValue(writableWindowX, window.getX())) //
		);
	}

	public static Subscription spinOnCondition(Node node, ObservableValue<Boolean> observableCondition) {
		var spinAnimation = Animations.createDiscrete360Rotation();
		spinAnimation.setNode(node);
		return EasyBind.subscribe(observableCondition, test -> {
			if (test) {
				spinAnimation.playFromStart();
			} else {
				spinAnimation.stop();
				node.setRotate(0);
			}
		});
	}

	public static SequentialTransition createDiscrete360Rotation() {
		var animation = new SequentialTransition(IntStream.range(0, 8).mapToObj(i -> Animations.createDiscrete45Rotation()).toArray(Animation[]::new));
		animation.setCycleCount(Animation.INDEFINITE);
		return animation;
	}

	private static RotateTransition createDiscrete45Rotation() {
		var animation = new RotateTransition(Duration.millis(100));
		animation.setInterpolator(Interpolator.DISCRETE);
		animation.setByAngle(45);
		animation.setCycleCount(1);
		return animation;
	}

}

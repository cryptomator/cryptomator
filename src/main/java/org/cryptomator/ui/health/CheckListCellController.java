package org.cryptomator.ui.health;

import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;

public class CheckListCellController implements FxController {


	private final ObjectProperty<Check> check;
	private final ObservableValue<Boolean> checkRunnable;
	private final ObservableValue<String> checkName;

	/* FXML */
	public CheckBox checkbox;

	@Inject
	public CheckListCellController() {
		check = new SimpleObjectProperty<>();
		checkRunnable = check.flatMap(Check::stateProperty).map(Check.CheckState.RUNNABLE::equals).orElse(false);
		checkName = check.map(Check::getName).orElse("");
	}

	public void initialize() {
		check.addListener((observable, oldVal, newVal) -> {
			if (oldVal != null) {
				Bindings.unbindBidirectional(checkbox.selectedProperty(), oldVal.chosenForExecutionProperty());
			}
			if (newVal != null) {
				Bindings.bindBidirectional(checkbox.selectedProperty(), newVal.chosenForExecutionProperty());
			}
		});
	}

	public ObjectProperty<Check> checkProperty() {
		return check;
	}

	public Check getCheck() {
		return check.get();
	}

	public void setCheck(Check c) {
		check.set(c);
	}

	public ObservableValue<String> checkNameProperty() {
		return checkName;
	}

	public String getCheckName() {
		return checkName.getValue();
	}

	public ObservableValue<Boolean> checkRunnableProperty() {
		return checkRunnable;
	}

	public boolean isCheckRunnable() {
		return checkRunnable.getValue();
	}
}

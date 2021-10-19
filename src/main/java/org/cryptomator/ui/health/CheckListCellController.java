package org.cryptomator.ui.health;

import com.tobiasdiez.easybind.EasyBind;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.CheckBox;

public class CheckListCellController implements FxController {


	private final ObjectProperty<Check> check;
	private final Binding<String> checkName;
	private final Binding<Boolean> checkRunnable;

	/* FXML */
	public CheckBox checkbox;

	@Inject
	public CheckListCellController() {
		check = new SimpleObjectProperty<>();
		checkRunnable = EasyBind.wrapNullable(check).mapObservable(Check::stateProperty).map(Check.CheckState.RUNNABLE::equals).orElse(false);
		checkName = EasyBind.wrapNullable(check).map(Check::getName).orElse("");
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

	public Binding<String> checkNameProperty() {
		return checkName;
	}

	public String getCheckName() {
		return checkName.getValue();
	}

	public Binding<Boolean> checkRunnableProperty() {
		return checkRunnable;
	}

	public boolean isCheckRunnable() {
		return checkRunnable.getValue();
	}
}

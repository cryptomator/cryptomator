package org.cryptomator.ui.health;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.beans.binding.Binding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.CheckBox;
import java.util.ArrayList;
import java.util.List;

public class CheckListCellController implements FxController {


	private final ObjectProperty<Check> check;
	private final Binding<String> checkName;
	private final Binding<Boolean> checkRunnable;
	private final List<Subscription> subscriptions;

	/* FXML */
	public CheckBox forRunSelectedCheckBox;

	@Inject
	public CheckListCellController() {
		check = new SimpleObjectProperty<>();
		checkRunnable = EasyBind.wrapNullable(check).mapObservable(Check::stateProperty).map(Check.CheckState.RUNNABLE::equals).orElse(false);
		checkName = EasyBind.wrapNullable(check).map(Check::getName).orElse("");
		subscriptions = new ArrayList<>();
	}

	public void initialize() {
		subscriptions.add(EasyBind.subscribe(check, c -> {
			forRunSelectedCheckBox.selectedProperty().unbind();
			if (c != null) {
				forRunSelectedCheckBox.selectedProperty().bindBidirectional(c.chosenForExecutionProperty());
			}
		}));
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

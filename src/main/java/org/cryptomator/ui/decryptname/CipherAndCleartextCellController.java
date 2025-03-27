package org.cryptomator.ui.decryptname;

import org.cryptomator.common.ObservableUtil;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import java.util.ResourceBundle;

public class CipherAndCleartextCellController implements FxController {

	private final ObjectProperty<CipherAndCleartext> item = new SimpleObjectProperty<>();
	private final ObservableValue<String> ciphertext;
	private final ObservableValue<String> cleartext;
	private final ObjectProperty<FontAwesome5Icon> icon = new SimpleObjectProperty<>();
	private final ResourceBundle resourceBundle;

	@FXML
	public HBox root;

	@Inject
	public CipherAndCleartextCellController(ResourceBundle resourceBundle) {
		this.resourceBundle = resourceBundle;
		this.ciphertext = ObservableUtil.mapWithDefault(item, i -> i.ciphertext().getFileName().toString(), "");
		this.cleartext = ObservableUtil.mapWithDefault(item, CipherAndCleartext::cleartextName, "");
	}

	@FXML
	public void initialize() {
		icon.bind(Bindings.createObjectBinding(this::selectIcon, root.hoverProperty()));
	}

	private String selectText() {
		var cipherAndClear = item.get();
		if (cipherAndClear != null) {
			if (root.isHover()) {
				return cipherAndClear.cleartextName();
			} else {
				return cipherAndClear.ciphertext().getFileName().toString();
			}
		}
		return "";
	}

	private FontAwesome5Icon selectIcon() {
		if (root.isHover()) {
			return FontAwesome5Icon.LOCK_OPEN;
		} else {
			return FontAwesome5Icon.LOCK;
		}
	}

	public void setCipherAndCleartextEntry(@NotNull CipherAndCleartext item) {
		this.item.set(item);
		var tooltip = new Tooltip("Click to copy");
		Tooltip.install(root,tooltip);
	}

	//observability getter
	public ObservableValue<String> ciphertextProperty() {
		return ciphertext;
	}

	public String getCiphertext() {
		return ciphertext.getValue();
	}

	public ObservableValue<String> cleartextProperty() {
		return cleartext;
	}

	public String getCleartext() {
		return cleartext.getValue();
	}

	public ObservableValue<FontAwesome5Icon> iconProperty() {
		return icon;
	}

	public FontAwesome5Icon getIcon() {
		return icon.getValue();
	}

}

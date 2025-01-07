package org.cryptomator.ui.dialogs;

import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlLoaderFactory;
import org.cryptomator.ui.controls.FontAwesome5Icon;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.IllegalFormatException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class SimpleDialog {

	private final ResourceBundle resourceBundle;

	private final Stage dialogStage;

	SimpleDialog(Builder builder) throws IOException {
		this.resourceBundle = builder.resourceBundle;
		dialogStage = new Stage();
		dialogStage.initOwner(builder.owner);
		dialogStage.initModality(Modality.WINDOW_MODAL);
		dialogStage.setTitle(resolveText(builder.titleKey, builder.titleArgs));
		dialogStage.setResizable(false);

		FxmlLoaderFactory loaderFactory = FxmlLoaderFactory.forController(new SimpleDialogController(), Scene::new, builder.resourceBundle);
		FXMLLoader loader = loaderFactory.load(FxmlFile.SIMPLE_DIALOG.getRessourcePathString());
		Parent root = loader.getRoot();
		SimpleDialogController controller = loader.getController();

		controller.setMessage(resolveText(builder.messageKey, null));
		controller.setDescription(resolveText(builder.descriptionKey, null));
		controller.setIcon(builder.icon);
		controller.setOkButtonText(resolveText(builder.okButtonKey, null));
		controller.setCancelButtonText(resolveText(builder.cancelButtonKey, null));

		controller.setOkAction(() -> builder.okAction.accept(dialogStage));
		controller.setCancelAction(() -> builder.cancelAction.accept(dialogStage));

		dialogStage.setScene(new Scene(root));
	}

	public void showAndWait() {
		dialogStage.showAndWait();
	}

	private String resolveText(String key, String[] args) {
		if (key == null || key.isEmpty() || !resourceBundle.containsKey(key)) {
			throw new IllegalArgumentException(String.format("Invalid key: '%s'. Key not found in ResourceBundle.", key));
		}
		String text = resourceBundle.getString(key);
		try {
			return args != null && args.length > 0 ? String.format(text, (Object[]) args) : text;
		} catch (IllegalFormatException e) {
			throw new IllegalArgumentException("Formatting error: Check if arguments match placeholders in the text.", e);
		}
	}

	public static class Builder {

		private Stage owner;
		private final ResourceBundle resourceBundle;
		private String titleKey;
		private String[] titleArgs;
		private String messageKey;
		private String descriptionKey;
		private String okButtonKey;
		private String cancelButtonKey;

		private FontAwesome5Icon icon;
		private Consumer<Stage> okAction = Stage::close;
		private Consumer<Stage> cancelAction = Stage::close;

		public Builder(ResourceBundle resourceBundle) {
			this.resourceBundle = resourceBundle;
		}

		public Builder setOwner(Stage owner) {
			this.owner = owner;
			return this;
		}

		public Builder setTitleKey(String titleKey, String... args) {
			this.titleKey = titleKey;
			this.titleArgs = args;
			return this;
		}

		public Builder setMessageKey(String messageKey) {
			this.messageKey = messageKey;
			return this;
		}

		public Builder setDescriptionKey(String descriptionKey) {
			this.descriptionKey = descriptionKey;
			return this;
		}

		public Builder setIcon(FontAwesome5Icon icon) {
			this.icon = icon;
			return this;
		}

		public Builder setOkButtonKey(String okButtonKey) {
			this.okButtonKey = okButtonKey;
			return this;
		}

		public Builder setCancelButtonKey(String cancelButtonKey) {
			this.cancelButtonKey = cancelButtonKey;
			return this;
		}

		public Builder setOkAction(Consumer<Stage> okAction) {
			this.okAction = okAction;
			return this;
		}

		public Builder setCancelAction(Consumer<Stage> cancelAction) {
			this.cancelAction = cancelAction;
			return this;
		}

		public SimpleDialog build() {
			Objects.requireNonNull(titleKey,"SimpleDialog titleKey must be set.");
			Objects.requireNonNull(messageKey,"SimpleDialog messageKey must be set.");
			Objects.requireNonNull(descriptionKey,"SimpleDialog descriptionKey must be set.");
			Objects.requireNonNull(okButtonKey,"SimpleDialog okButtonKey must be set.");
			Objects.requireNonNull(cancelButtonKey,"SimpleDialog cancelButtonKey must be set.");

			try {
				return new SimpleDialog(this);
			} catch (IOException e) {
				throw new UncheckedIOException("Failed to create SimpleDialog.", e);
			}
		}
	}
}
package org.cryptomator.ui.controls;

import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlLoaderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class CustomDialog {

	private static final Logger LOG = LoggerFactory.getLogger(CustomDialog.class);

	private final Stage dialogStage;

	CustomDialog(Builder builder){
		dialogStage = new Stage();
		dialogStage.initOwner(builder.owner);
		dialogStage.initModality(Modality.WINDOW_MODAL);
		dialogStage.setTitle(resolveText(builder.resourceBundle, builder.titleKey, builder.titleArgs));

		try{
			FxmlLoaderFactory loaderFactory = FxmlLoaderFactory.forController(new CustomDialogController(), Scene::new, builder.resourceBundle);
			FXMLLoader loader = loaderFactory.load(FxmlFile.CUSTOM_DIALOG.getRessourcePathString());
			Parent root = loader.getRoot();
			CustomDialogController controller = loader.getController();

			controller.setMessage(resolveText(builder.resourceBundle, builder.messageKey, null));
			controller.setDescription(resolveText(builder.resourceBundle, builder.descriptionKey, null));
			controller.setIcon(builder.icon);
			controller.setOkButtonText(resolveText(builder.resourceBundle, builder.okButtonKey, null));
			controller.setCancelButtonText(resolveText(builder.resourceBundle, builder.cancelButtonKey, null));

			controller.setOkAction(() -> builder.okAction.accept(dialogStage));
			controller.setCancelAction(() -> builder.cancelAction.accept(dialogStage));

			dialogStage.setScene(new Scene(root));
			dialogStage.showAndWait();
		} catch (Exception e) {
			LOG.error("Failed to build and show dialog stage.", e);
		}

	}

	private String resolveText(ResourceBundle resourceBundle, String key, String[] args) {
		String text = resourceBundle.getString(key);
		return args != null && args.length > 0 ? String.format(text, (Object[]) args) : text;
	}

	public static class Builder {
		private Stage owner;
		private ResourceBundle resourceBundle;
		private String titleKey;
		private String[] titleArgs;
		private String messageKey;
		private String descriptionKey;
		private String okButtonKey;
		private String cancelButtonKey;

		private FontAwesome5Icon icon;
		private Consumer<Stage> okAction = Stage::close;
		private Consumer<Stage> cancelAction = Stage::close;

		public Builder setOwner(Stage owner) {
			this.owner = owner;
			return this;
		}
		public Builder resourceBundle(ResourceBundle resourceBundle) {
			this.resourceBundle = resourceBundle;
			return this;
		}

		public Builder titleKey(String titleKey, String... args) {
			this.titleKey = titleKey;
			this.titleArgs = args;
			return this;
		}

		public Builder messageKey(String messageKey) {
			this.messageKey = messageKey;
			return this;
		}

		public Builder descriptionKey(String descriptionKey) {
			this.descriptionKey = descriptionKey;
			return this;
		}

		public Builder icon(FontAwesome5Icon icon) {
			this.icon = icon;
			return this;
		}

		public Builder okButtonKey(String okButtonKey) {
			this.okButtonKey = okButtonKey;
			return this;
		}

		public Builder cancelButtonKey(String cancelButtonKey) {
			this.cancelButtonKey = cancelButtonKey;
			return this;
		}

		public Builder okAction(Consumer<Stage> okAction) {
			this.okAction = okAction;
			return this;
		}

		public Builder cancelAction(Consumer<Stage> cancelAction) {
			this.cancelAction = cancelAction;
			return this;
		}

		public CustomDialog build(){
			return new CustomDialog(this);
		}
	}
}
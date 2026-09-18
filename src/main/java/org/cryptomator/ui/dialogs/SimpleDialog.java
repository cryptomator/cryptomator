package org.cryptomator.ui.dialogs;

import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlLoaderFactory;
import org.cryptomator.ui.common.StageFactory;
import org.cryptomator.ui.controls.FontAwesome5Icon;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBar;
import javafx.scene.layout.Region;
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
		dialogStage = builder.stageFactory.create();
		dialogStage.initOwner(builder.owner);
		dialogStage.initModality(Modality.WINDOW_MODAL);
		dialogStage.setTitle(resolveText(builder.titleKey, builder.titleArgs));
		dialogStage.setResizable(false);

		FxmlLoaderFactory loaderFactory = FxmlLoaderFactory.forController( //
				new SimpleDialogController(resolveText(builder.messageKey, null), //
						resolveText(builder.descriptionKey, builder.descriptionArgs), //
						builder.icon, //
						resolveText(builder.okButtonKey, null), //
						builder.cancelButtonKey != null ? resolveText(builder.cancelButtonKey, null) : null, //
						() -> builder.okAction.accept(dialogStage), //
						() -> builder.cancelAction.accept(dialogStage)), //
				builder.sceneFactory, builder.resourceBundle);

		dialogStage.setScene(loaderFactory.createScene(FxmlFile.SIMPLE_DIALOG));
		dialogStage.setMinWidth(400);
		fitDialogToButtonLabels(dialogStage);
	}

	public void showAndWait() {
		dialogStage.showAndWait();
	}

	/**
	 * JavaFX {@link ButtonBar} shrinks buttons toward {@code buttonMinWidth} when the parent is too narrow,
	 * which crops translated labels (see issue #3338). Grow the dialog with the widest label instead.
	 */
	static void fitDialogToButtonLabels(Stage stage) {
		Scene scene = stage.getScene();
		if (scene == null || !(scene.getRoot() instanceof Region root)) {
			return;
		}
		root.setMaxWidth(Double.MAX_VALUE);
		root.setMinWidth(Region.USE_COMPUTED_SIZE);
		root.applyCss();

		if (root.lookup(".button-bar") instanceof ButtonBar buttonBar) {
			buttonBar.setMinWidth(Region.USE_PREF_SIZE);
			double minButtonWidth = buttonBar.getButtonMinWidth();
			for (Node node : buttonBar.getButtons()) {
				if (node instanceof Region button && node.isManaged() && node.isVisible()) {
					button.setMinWidth(Math.max(minButtonWidth, button.prefWidth(-1)));
				}
			}
		}
		stage.sizeToScene();
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
		private final StageFactory stageFactory;
		private final DefaultSceneFactory sceneFactory;
		private String titleKey;
		private String[] titleArgs;
		private String messageKey;
		private String descriptionKey;
		private String[] descriptionArgs;
		private String okButtonKey;
		private String cancelButtonKey;
		private FontAwesome5Icon icon;
		private Consumer<Stage> okAction = Stage::close;
		private Consumer<Stage> cancelAction = Stage::close;

		public Builder(ResourceBundle resourceBundle, StageFactory stageFactory, DefaultSceneFactory sceneFactory) {
			this.resourceBundle = resourceBundle;
			this.stageFactory = stageFactory;
			this.sceneFactory = sceneFactory;
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

		public Builder setDescriptionKey(String descriptionKey, String... args) {
			this.descriptionKey = descriptionKey;
			this.descriptionArgs = args;
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
			Objects.requireNonNull(titleKey, "SimpleDialog titleKey must be set.");
			Objects.requireNonNull(messageKey, "SimpleDialog messageKey must be set.");
			Objects.requireNonNull(descriptionKey, "SimpleDialog descriptionKey must be set.");
			Objects.requireNonNull(okButtonKey, "SimpleDialog okButtonKey must be set.");

			try {
				return new SimpleDialog(this);
			} catch (IOException e) {
				throw new UncheckedIOException("Failed to create SimpleDialog.", e);
			}
		}
	}
}
package org.cryptomator.ui.common;

import org.cryptomator.ui.fxapp.FxApplicationScoped;

import javax.inject.Inject;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.util.function.Consumer;

@FxApplicationScoped
public class StageFactory {

	private final Consumer<Stage> initializer;

	@Inject
	public StageFactory(StageInitializer initializer) {
		this.initializer = initializer;
	}

	public Stage create() {
		Stage stage = new Stage(StageStyle.DECORATED);
		initializer.accept(stage);
		return stage;
	}

}

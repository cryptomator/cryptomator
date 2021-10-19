package org.cryptomator.ui.common;

import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.util.function.Consumer;

public class StageFactory {

	private final Consumer<Stage> initializer;

	public StageFactory(Consumer<Stage> initializer) {
		this.initializer = initializer;
	}

	public Stage create() {
		return create(StageStyle.DECORATED);
	}

	public Stage create(StageStyle stageStyle) {
		Stage stage = new Stage(stageStyle);
		initializer.accept(stage);
		return stage;
	}

}

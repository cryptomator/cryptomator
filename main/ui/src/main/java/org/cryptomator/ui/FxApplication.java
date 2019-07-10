package org.cryptomator.ui;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.UncheckedIOException;

@FxApplicationScoped
public class FxApplication extends Application {

	private static final Logger LOG = LoggerFactory.getLogger(FxApplication.class);

	private final Stage primaryStage;
	private final FXMLLoaderFactory fxmlLoaders;

	@Inject
	FxApplication(@Named("mainWindow") Stage primaryStage, FXMLLoaderFactory fxmlLoaders) {
		this.primaryStage = primaryStage;
		this.fxmlLoaders = fxmlLoaders;
	}

	public void start() {
		try {
			LOG.info("Starting GUI...");
			start(primaryStage);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void start(Stage stage) throws IOException {
		Parent root = fxmlLoaders.load("/fxml/main_window.fxml").getRoot();
		stage.setScene(new Scene(root));
		stage.show();
	}

}

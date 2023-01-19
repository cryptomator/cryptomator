package org.cryptomator.ui.mainwindow;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tobiasdiez.easybind.EasyBind;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.integrations.revealpath.RevealFailedException;
import org.cryptomator.integrations.revealpath.RevealPathService;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.VaultService;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.cryptomator.ui.stats.VaultStatisticsComponent;
import org.cryptomator.ui.wrongfilealert.WrongFileAlertComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@MainWindowScoped
public class VaultDetailUnlockedController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(VaultDetailUnlockedController.class);
	private static final String ACTIVE_CLASS = "active";
	private final ReadOnlyObjectProperty<Vault> vault;
	private final FxApplicationWindows appWindows;
	private final VaultService vaultService;
	private final WrongFileAlertComponent.Builder wrongFileAlert;
	private final Stage mainWindow;
	private final ResourceBundle resourceBundle;
	private final LoadingCache<Vault, VaultStatisticsComponent> vaultStats;
	private final VaultStatisticsComponent.Builder vaultStatsBuilder;
	private final BooleanProperty draggingOver = new SimpleBooleanProperty();
	private final BooleanProperty ciphertextPathsCopied = new SimpleBooleanProperty();

	public Button dropZone;

	@Inject
	public VaultDetailUnlockedController(ObjectProperty<Vault> vault, FxApplicationWindows appWindows, VaultService vaultService, VaultStatisticsComponent.Builder vaultStatsBuilder, WrongFileAlertComponent.Builder wrongFileAlert, @MainWindow Stage mainWindow, ResourceBundle resourceBundle) {
		this.vault = vault;
		this.appWindows = appWindows;
		this.vaultService = vaultService;
		this.wrongFileAlert = wrongFileAlert;
		this.mainWindow = mainWindow;
		this.resourceBundle = resourceBundle;
		this.vaultStats = CacheBuilder.newBuilder().weakValues().build(CacheLoader.from(this::buildVaultStats));
		this.vaultStatsBuilder = vaultStatsBuilder;
	}

	public void initialize() {
		dropZone.setOnDragEntered(this::handleDragEvent);
		dropZone.setOnDragOver(this::handleDragEvent);
		dropZone.setOnDragDropped(this::handleDragEvent);
		dropZone.setOnDragExited(this::handleDragEvent);

		EasyBind.includeWhen(dropZone.getStyleClass(), ACTIVE_CLASS, draggingOver);
	}
	private void handleDragEvent(DragEvent event) {
		if (DragEvent.DRAG_OVER.equals(event.getEventType()) && event.getGestureSource() == null && event.getDragboard().hasFiles()) {
			event.acceptTransferModes(TransferMode.ANY);
			draggingOver.set(true);
		} else if (DragEvent.DRAG_DROPPED.equals(event.getEventType()) && event.getGestureSource() == null && event.getDragboard().hasFiles()) {
			List<Path> ciphertextPaths = event.getDragboard().getFiles().stream().map(File::toPath).map(this::getCiphertextPath).flatMap(Optional::stream).toList();
			if (ciphertextPaths.isEmpty()) {
				wrongFileAlert.build().showWrongFileAlertWindow();
			} else {
				revealOrCopyPaths(ciphertextPaths);
			}
			event.setDropCompleted(!ciphertextPaths.isEmpty());
			event.consume();
		} else if (DragEvent.DRAG_EXITED.equals(event.getEventType())) {
			draggingOver.set(false);
		}
	}

	private VaultStatisticsComponent buildVaultStats(Vault vault) {
		return vaultStatsBuilder.vault(vault).build();
	}

	@FXML
	public void revealAccessLocation() {
		vaultService.reveal(vault.get());
	}

	@FXML
	public void lock() {
		appWindows.startLockWorkflow(vault.get(), mainWindow);
	}

	@FXML
	public void showVaultStatistics() {
		vaultStats.getUnchecked(vault.get()).showVaultStatisticsWindow();
	}

	@FXML
	public void chooseItemAndReveal() {
		var fileChooser = new FileChooser();
		fileChooser.setTitle(resourceBundle.getString("main.vaultDetail.filePickerTitle"));
		fileChooser.setInitialDirectory(Path.of(vault.get().getAccessPoint()).toFile());
		var cleartextFile = fileChooser.showOpenDialog(mainWindow);
		if (cleartextFile != null) {
			var ciphertextPaths = getCiphertextPath(cleartextFile.toPath()).stream().toList();
			revealOrCopyPaths(ciphertextPaths);
		}
	}

	private boolean containsUnlockedVaultContent(Path path) {
		return path.startsWith(vault.get().getAccessPoint());
	}

	private Optional<Path> getCiphertextPath(Path path) {
		if (!containsUnlockedVaultContent(path)) {
			LOG.debug("Path does not start with access point of selected vault: {}", path);
			return Optional.empty();
		}
		try {
			var accessPoint = vault.get().getAccessPoint();
			var cleartextPath = path.toString().substring(accessPoint.length());
			if (!cleartextPath.startsWith("/")) {
				cleartextPath = "/" + cleartextPath;
			}
			return Optional.of(vault.get().getCiphertextPath(cleartextPath));
		} catch (IOException e) {
			LOG.warn("Unable to get ciphertext path from path: {}", path);
			return Optional.empty();
		}
	}

	private void revealOrCopyPaths(List<Path> paths) {
		if (!revealPaths(paths)) {
			LOG.warn("No service provider to reveal files found.");
			copyPathsToClipboard(paths);
		}
	}

	/**
	 * Reveals the paths over the {@link RevealPathService} in the file system
	 *
	 * @param paths List of Paths to reveal
	 * @return true, if at least one service provider was present, false otherwise
	 */
	private boolean revealPaths(List<Path> paths) {
		return RevealPathService.get().findAny().map(s -> {
			paths.forEach(path -> {
				try {
					s.reveal(path);
				} catch (RevealFailedException e) {
					LOG.error("Revealing ciphertext file failed.", e);
				}
			});
			return true;
		}).orElse(false);
	}

	private void copyPathsToClipboard(List<Path> paths) {
		StringBuilder clipboardString = new StringBuilder();
		paths.forEach(p -> clipboardString.append(p.toString()).append("\n"));
		Clipboard.getSystemClipboard().setContent(Map.of(DataFormat.PLAIN_TEXT, clipboardString.toString()));
		ciphertextPathsCopied.setValue(true);
		CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS, Platform::runLater).execute(() -> {
			ciphertextPathsCopied.set(false);
		});
	}

	/* Getter/Setter */

	public ReadOnlyObjectProperty<Vault> vaultProperty() {
		return vault;
	}

	public Vault getVault() {
		return vault.get();
	}

	public BooleanProperty ciphertextPathsCopiedProperty() {
		return ciphertextPathsCopied;
	}

	public boolean isCiphertextPathsCopied() {
		return ciphertextPathsCopied.get();
	}
}

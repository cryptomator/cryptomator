package org.cryptomator.ui.mainwindow;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.VaultService;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.cryptomator.ui.stats.VaultStatisticsComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@MainWindowScoped
public class VaultDetailUnlockedController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(VaultDetailUnlockedController.class);

	private final ReadOnlyObjectProperty<Vault> vault;
	private final FxApplicationWindows appWindows;
	private final VaultService vaultService;
	private final Stage mainWindow;
	private final LoadingCache<Vault, VaultStatisticsComponent> vaultStats;
	private final VaultStatisticsComponent.Builder vaultStatsBuilder;
	private final BooleanProperty draggingUnlockedVaultContentOver = new SimpleBooleanProperty();

	public StackPane dropZone;

	@Inject
	public VaultDetailUnlockedController(ObjectProperty<Vault> vault, FxApplicationWindows appWindows, VaultService vaultService, VaultStatisticsComponent.Builder vaultStatsBuilder, @MainWindow Stage mainWindow) {
		this.vault = vault;
		this.appWindows = appWindows;
		this.vaultService = vaultService;
		this.mainWindow = mainWindow;
		this.vaultStats = CacheBuilder.newBuilder().weakValues().build(CacheLoader.from(this::buildVaultStats));
		this.vaultStatsBuilder = vaultStatsBuilder;
	}

	public void initialize() {
		dropZone.setOnDragEntered(this::handleDragEvent);
		dropZone.setOnDragOver(this::handleDragEvent);
		dropZone.setOnDragDropped(this::handleDragEvent);
		dropZone.setOnDragExited(this::handleDragEvent);
	}

	private void handleDragEvent(DragEvent event) {
		if (DragEvent.DRAG_OVER.equals(event.getEventType()) && event.getGestureSource() == null && event.getDragboard().hasFiles()) {
			event.acceptTransferModes(TransferMode.ANY);
			draggingUnlockedVaultContentOver.set(event.getDragboard().getFiles().stream().map(File::toPath).anyMatch(this::containsUnlockedVaultContent));
		} else if (DragEvent.DRAG_DROPPED.equals(event.getEventType()) && event.getGestureSource() == null && event.getDragboard().hasFiles()) {
			Set<Path> ciphertextPaths = event.getDragboard().getFiles().stream().map(File::toPath).map(this::getCiphertextPath).flatMap(Optional::stream).collect(Collectors.toSet());
			if (!ciphertextPaths.isEmpty()) {
				ciphertextPaths.forEach(this::revealPath);
			}
			event.setDropCompleted(!ciphertextPaths.isEmpty());
			event.consume();
		} else if (DragEvent.DRAG_EXITED.equals(event.getEventType())) {
			draggingUnlockedVaultContentOver.set(false);
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

	private boolean containsUnlockedVaultContent(Path path) {
		return path.startsWith(vault.get().getAccessPoint());
	}

	private Optional<Path> getCiphertextPath(Path path) {
		if (!containsUnlockedVaultContent(path)) {
			LOG.debug("Path does not start with access point of selected vault: {}", path);
			return Optional.empty();
		}
		try {
			var cleartextPathString = path.toString().substring(vault.get().getAccessPoint().length());
			if (!cleartextPathString.startsWith("/")) {
				cleartextPathString = "/" + cleartextPathString;
			}
			var cleartextPath = Path.of(cleartextPathString);
			return Optional.of(vault.get().getCiphertextPath(cleartextPath));
		} catch (IOException e) {
			LOG.debug("Unable to get ciphertext path from path: {}", path);
			return Optional.empty();
		}
	}

	private void revealPath(Path path) {
		// Unable to use `application.get().getHostServices().showDocument()` here, because it does not reveal files, only folders.
		Desktop.getDesktop().browseFileDirectory(path.toFile());
	}

	/* Getter/Setter */

	public ReadOnlyObjectProperty<Vault> vaultProperty() {
		return vault;
	}

	public Vault getVault() {
		return vault.get();
	}

	public BooleanProperty draggingUnlockedVaultContentOverProperty() {
		return draggingUnlockedVaultContentOver;
	}

	public boolean isDraggingUnlockedVaultContentOver() {
		return draggingUnlockedVaultContentOver.get();
	}

}

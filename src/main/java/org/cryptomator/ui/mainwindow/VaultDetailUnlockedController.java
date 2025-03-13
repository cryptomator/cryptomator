package org.cryptomator.ui.mainwindow;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tobiasdiez.easybind.EasyBind;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.integrations.mount.Mountpoint;
import org.cryptomator.integrations.revealpath.RevealFailedException;
import org.cryptomator.integrations.revealpath.RevealPathService;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.StageFactory;
import org.cryptomator.ui.common.VaultService;
import org.cryptomator.ui.dialogs.SimpleDialog;
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
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
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
	private final Optional<RevealPathService> revealPathService;
	private final ResourceBundle resourceBundle;
	private final LoadingCache<Vault, VaultStatisticsComponent> vaultStats;
	private final VaultStatisticsComponent.Builder vaultStatsBuilder;
	private final ObservableValue<Boolean> accessibleViaPath;
	private final ObservableValue<Boolean> accessibleViaUri;
	private final ObservableValue<String> mountPoint;
	private final BooleanProperty draggingOver = new SimpleBooleanProperty();
	private final BooleanProperty ciphertextPathsCopied = new SimpleBooleanProperty();

	//FXML
	public Button dropZone;

	@Inject
	public VaultDetailUnlockedController(ObjectProperty<Vault> vault, FxApplicationWindows appWindows, VaultService vaultService, VaultStatisticsComponent.Builder vaultStatsBuilder, WrongFileAlertComponent.Builder wrongFileAlert, @MainWindow Stage mainWindow, Optional<RevealPathService> revealPathService, ResourceBundle resourceBundle) {
		this.vault = vault;
		this.appWindows = appWindows;
		this.vaultService = vaultService;
		this.wrongFileAlert = wrongFileAlert;
		this.mainWindow = mainWindow;
		this.revealPathService = revealPathService;
		this.resourceBundle = resourceBundle;
		this.vaultStats = CacheBuilder.newBuilder().weakValues().build(CacheLoader.from(this::buildVaultStats));
		this.vaultStatsBuilder = vaultStatsBuilder;
		var mp = vault.flatMap(Vault::mountPointProperty);
		this.accessibleViaPath = mp.map(m -> m instanceof Mountpoint.WithPath).orElse(false);
		this.accessibleViaUri = mp.map(m -> m instanceof Mountpoint.WithUri).orElse(false);
		this.mountPoint = mp.map(m -> {
			if (m instanceof Mountpoint.WithPath mwp) {
				return mwp.path().toString();
			} else {
				return m.uri().toASCIIString();
			}
		});
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
			if(SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_MAC) {
				event.acceptTransferModes(TransferMode.LINK);
			} else {
				event.acceptTransferModes(TransferMode.ANY);
			}
			draggingOver.set(true);
		} else if (DragEvent.DRAG_DROPPED.equals(event.getEventType()) && event.getGestureSource() == null && event.getDragboard().hasFiles()) {
			List<Path> ciphertextPaths = event.getDragboard().getFiles().stream().map(File::toPath).map(this::getCiphertextPath).flatMap(Optional::stream).toList();
			//TODO: differ between encrypted and decrypted files
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
	public void copyMountUri() {
		ClipboardContent clipboardContent = new ClipboardContent();
		clipboardContent.putString(mountPoint.getValue());
		Clipboard.getSystemClipboard().setContent(clipboardContent);
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
	public void chooseFileAndReveal() {
		Preconditions.checkState(accessibleViaPath.getValue());
		var fileChooser = new FileChooser();
		fileChooser.setTitle(resourceBundle.getString("main.vaultDetail.decryptedFilePickerTitle"));
		fileChooser.setInitialDirectory(Path.of(mountPoint.getValue()).toFile());
		var cleartextFile = fileChooser.showOpenDialog(mainWindow);
		if (cleartextFile != null) {
			var ciphertextPaths = getCiphertextPath(cleartextFile.toPath()).stream().toList();
			revealOrCopyPaths(ciphertextPaths);
		}
	}

	@FXML
	public void chooseEncryptedFileAndGetName() {
		var fileChooser = new FileChooser();
		fileChooser.setTitle(resourceBundle.getString("main.vaultDetail.encryptedFilePickerTitle"));

		fileChooser.setInitialDirectory(vault.getValue().getPath().toFile());
		var ciphertextNode = fileChooser.showOpenDialog(mainWindow);
		try {
			var nodeName = vault.get().getCleartextName(ciphertextNode.toPath());
			var alert = new Alert(Alert.AlertType.INFORMATION, "The answer is: %s".formatted(nodeName), ButtonType.OK);
			alert.showAndWait();
			//.filter(response -> response == ButtonType.OK)
			//.ifPresent(response -> formatSystem());
		} catch (Exception e) {
			LOG.warn("Failed to decrypt filename for {}", ciphertextNode, e);
			var alert = new Alert(Alert.AlertType.ERROR, "The exception is: %s".formatted(e.getClass()), ButtonType.OK);
			alert.showAndWait();
		}
	}

	private boolean startsWithVaultAccessPoint(Path path) {
		return path.startsWith(Path.of(mountPoint.getValue()));
	}

	private Optional<Path> getCiphertextPath(Path path) {
		if (!startsWithVaultAccessPoint(path)) {
			LOG.debug("Path does not start with access point of selected vault: {}", path);
			return Optional.empty();
		}
		try {
			return Optional.of(vault.get().getCiphertextPath(path));
		} catch (IOException e) {
			LOG.warn("Unable to get ciphertext path from path: {}", path, e);
			return Optional.empty();
		}
	}

	private void revealOrCopyPaths(List<Path> paths) {
		revealPathService.ifPresentOrElse(svc -> revealPaths(svc, paths), () -> {
			LOG.warn("No service provider to reveal files found.");
			copyPathsToClipboard(paths);
		});
	}

	private void revealPaths(RevealPathService service, List<Path> paths) {
		paths.forEach(path -> {
			try {
				LOG.debug("Revealing {}", path);
				service.reveal(path);
			} catch (RevealFailedException e) {
				LOG.error("Revealing ciphertext file failed.", e);
			}
		});
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

	public ObservableValue<Boolean> accessibleViaPathProperty() {
		return accessibleViaPath;
	}

	public boolean isAccessibleViaPath() {
		return accessibleViaPath.getValue();
	}

	public ObservableValue<Boolean> accessibleViaUriProperty() {
		return accessibleViaUri;
	}

	public boolean isAccessibleViaUri() {
		return accessibleViaUri.getValue();
	}

	public ObservableValue<String> mountPointProperty() {
		return mountPoint;
	}

	public String getMountPoint() {
		return mountPoint.getValue();
	}

	public BooleanProperty ciphertextPathsCopiedProperty() {
		return ciphertextPathsCopied;
	}

	public boolean isCiphertextPathsCopied() {
		return ciphertextPathsCopied.get();
	}
}

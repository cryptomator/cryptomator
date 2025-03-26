package org.cryptomator.ui.mainwindow;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tobiasdiez.easybind.EasyBind;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.Nullable;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.integrations.mount.Mountpoint;
import org.cryptomator.integrations.revealpath.RevealFailedException;
import org.cryptomator.integrations.revealpath.RevealPathService;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.VaultService;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.cryptomator.ui.controls.FontAwesome5IconView;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.cryptomator.ui.stats.VaultStatisticsComponent;
import org.cryptomator.ui.wrongfilealert.WrongFileAlertComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.application.Platform;
import javafx.beans.binding.ListExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

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
	private final BooleanProperty draggingOverLocateEncrypted = new SimpleBooleanProperty();
	private final BooleanProperty draggingOverDecryptName = new SimpleBooleanProperty();
	private final BooleanProperty ciphertextPathsCopied = new SimpleBooleanProperty();
	private final BooleanProperty decryptNameViewShowing = new SimpleBooleanProperty();
	private final ListProperty<Path> pathsToDecrypt = new SimpleListProperty<>(FXCollections.observableArrayList());

	@FXML
	public Button revealEncryptedDropZone;
	@FXML
	public Button decryptNameDropZone;

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
		revealEncryptedDropZone.setOnDragOver(e -> handleDragOver(e, draggingOverLocateEncrypted));
		revealEncryptedDropZone.setOnDragDropped(e -> handleDragDropped(e, this::getCiphertextPath, this::revealOrCopyPaths));
		revealEncryptedDropZone.setOnDragExited(_ -> draggingOverLocateEncrypted.setValue(false));

		decryptNameDropZone.setOnDragOver(e -> handleDragOver(e, draggingOverDecryptName));
		decryptNameDropZone.setOnDragDropped(e -> {
			decryptNameViewShowing.set(true);
			pathsToDecrypt.addAll(e.getDragboard().getFiles().stream().map(File::toPath).toList());
		});
		decryptNameDropZone.setOnDragExited(_ -> draggingOverDecryptName.setValue(false));
		initDecryptNameFeature();

		EasyBind.includeWhen(revealEncryptedDropZone.getStyleClass(), ACTIVE_CLASS, draggingOverLocateEncrypted);
		EasyBind.includeWhen(decryptNameDropZone.getStyleClass(), ACTIVE_CLASS, draggingOverDecryptName);
	}


	private void handleDragOver(DragEvent event, BooleanProperty prop) {
		if (event.getGestureSource() == null && event.getDragboard().hasFiles()) {
			if (SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_MAC) {
				event.acceptTransferModes(TransferMode.LINK);
			} else {
				event.acceptTransferModes(TransferMode.ANY);
			}
			prop.set(true);
		}
	}

	private <T> void handleDragDropped(DragEvent event, Function<Path, T> computation, Consumer<List<T>> positiveAction) {
		if (event.getGestureSource() == null && event.getDragboard().hasFiles()) {
			List<T> objects = event.getDragboard().getFiles().stream().map(File::toPath).map(computation).filter(Objects::nonNull).toList();
			if (objects.isEmpty()) {
				wrongFileAlert.build().showWrongFileAlertWindow();
			} else {
				positiveAction.accept(objects);
			}
			event.setDropCompleted(!objects.isEmpty());
			event.consume();
		}
	}

	@FXML
	public void chooseDecryptedFileAndReveal() {
		Preconditions.checkState(accessibleViaPath.getValue());
		var fileChooser = new FileChooser();
		fileChooser.setTitle(resourceBundle.getString("main.vaultDetail.locateEncrypted.filePickerTitle"));
		fileChooser.setInitialDirectory(Path.of(mountPoint.getValue()).toFile());
		var cleartextFile = fileChooser.showOpenDialog(mainWindow);
		if (cleartextFile != null) {
			var ciphertextPath = getCiphertextPath(cleartextFile.toPath());
			if (ciphertextPath != null) {
				revealOrCopyPaths(List.of(ciphertextPath));
			}
		}
	}

	@FXML
	public void showDecryptNameView() {
		decryptNameViewShowing.set(true);
	}

	private boolean startsWithVaultAccessPoint(Path path) {
		return path.startsWith(Path.of(mountPoint.getValue()));
	}

	@Nullable
	private Path getCiphertextPath(Path path) {
		if (!startsWithVaultAccessPoint(path)) {
			LOG.debug("Path does not start with mount point of selected vault: {}", path);
			return null;
		}
		try {
			return vault.get().getCiphertextPath(path);
		} catch (IOException e) {
			LOG.warn("Unable to get ciphertext path from path: {}", path, e);
			return null;
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

	record CipherToCleartext(String ciphertext, String cleartext) {

		@Override
		public String toString() {
			return ciphertext + " > " + cleartext;
		}
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

	public BooleanProperty decryptNameViewShowingProperty() {
		return decryptNameViewShowing;
	}

	public boolean isDecryptNameViewShowing() {
		return decryptNameViewShowing.get();
	}

	@FXML
	public void closeDecryptNameView() {
		decryptNameViewShowing.set(false);
	}

	//new stuff

	@FXML
	public void selectAndDecrypt() {
		var fileChooser = new FileChooser();
		fileChooser.setTitle(resourceBundle.getString("main.vaultDetail.decryptName.filePickerTitle"));

		fileChooser.setInitialDirectory(vault.getValue().getPath().toFile());
		var ciphertextNodes = fileChooser.showOpenMultipleDialog(mainWindow);
		if (ciphertextNodes != null) {
			pathsToDecrypt.clear();
			pathsToDecrypt.addAll(ciphertextNodes.stream().map(File::toPath).toList());
		}
	}

	@Nullable
	private CipherToCleartext getCleartextName(Path ciphertextNode) {
		try {
			return new CipherToCleartext(ciphertextNode.getFileName().toString(), vault.get().getCleartextName(ciphertextNode));
		} catch (IOException e) {
			LOG.warn("Failed to decrypt filename for {}", ciphertextNode, e);
			return null;
		}
	}

	public ListView<Path> decryptedNamesView;

	public ObservableValue<Boolean> decryptedPathsListEmptyProperty() {
		return pathsToDecrypt.emptyProperty();
	}

	public boolean isDecryptedPathsListEmpty() {
		return pathsToDecrypt.isEmpty();
	}

	private void initDecryptNameFeature() {
		decryptNameViewShowing.addListener((_,_,isShowing) -> {
			if(!isShowing) {
				pathsToDecrypt.clear();
			}
		});
		decryptedNamesView.setItems(pathsToDecrypt);
		decryptedNamesView.setCellFactory(this::createListCell);
	}

	private ListCell<Path> createListCell(ListView<Path> pathListView) {
		return new ListCell<Path>() {
			private final HBox root;
			private final FontAwesome5IconView icon;
			private final Label encryptedName;

			{
				setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
				encryptedName = new Label();
				icon = new FontAwesome5IconView();
				root = new HBox(icon, encryptedName);
				root.setSpacing(6.0);
				root.setPadding(new Insets(6));
			}

			@Override
			protected void updateItem(Path item, boolean empty) {
				super.updateItem(item, empty);

				if (item == null || empty) {
					setGraphic(null);
				} else {
					encryptedName.setText(item.toString());
					icon.setGlyph(FontAwesome5Icon.LOCK);
					setGraphic(root);
					getStyleClass().add("test-list-cell");
				}
			}
		};
	}

}


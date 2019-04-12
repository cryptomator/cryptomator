/*******************************************************************************
 * Copyright (c) 2014, 2017 Sebastian Stenzel
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *     Jean-Noël Charon - confirmation dialog on vault removal
 ******************************************************************************/
package org.cryptomator.ui.controllers;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Cell;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.FxApplicationScoped;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.ui.ExitUtil;
import org.cryptomator.ui.controls.DirectoryListCell;
import org.cryptomator.ui.l10n.Localization;
import org.cryptomator.ui.model.AppLaunchEvent;
import org.cryptomator.ui.model.AutoUnlocker;
import org.cryptomator.ui.model.Vault;
import org.cryptomator.ui.model.VaultFactory;
import org.cryptomator.ui.model.VaultList;
import org.cryptomator.ui.model.upgrade.UpgradeStrategies;
import org.cryptomator.ui.model.upgrade.UpgradeStrategy;
import org.cryptomator.ui.util.DialogBuilderUtil;
import org.cryptomator.ui.util.Tasks;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;
import org.fxmisc.easybind.monadic.MonadicBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import static org.cryptomator.ui.util.DialogBuilderUtil.buildErrorDialog;

@FxApplicationScoped
public class MainController implements ViewController {

	private static final Logger LOG = LoggerFactory.getLogger(MainController.class);
	private static final String ACTIVE_WINDOW_STYLE_CLASS = "active-window";
	private static final String INACTIVE_WINDOW_STYLE_CLASS = "inactive-window";

	private final Stage mainWindow;
	private final ExitUtil exitUtil;
	private final Localization localization;
	private final ExecutorService executorService;
	private final BlockingQueue<AppLaunchEvent> launchEventQueue;
	private final VaultFactory vaultFactoy;
	private final ViewControllerLoader viewControllerLoader;
	private final ObjectProperty<ViewController> activeController = new SimpleObjectProperty<>();
	private final ObservableList<Vault> vaults;
	private final BooleanBinding areAllVaultsLocked;
	private final ObjectProperty<Vault> selectedVault = new SimpleObjectProperty<>();
	private final ObjectExpression<Vault.State> selectedVaultState = ObjectExpression.objectExpression(EasyBind.select(selectedVault).selectObject(Vault::stateProperty));
	private final BooleanExpression isSelectedVaultValid = BooleanExpression.booleanExpression(EasyBind.monadic(selectedVault).map(Vault::isValidVaultDirectory).orElse(false));
	private final BooleanExpression canEditSelectedVault = selectedVaultState.isEqualTo(Vault.State.LOCKED);
	private final MonadicBinding<UpgradeStrategy> upgradeStrategyForSelectedVault;
	private final BooleanBinding isShowingSettings;
	private final Map<Vault, UnlockedController> unlockedVaults = new HashMap<>();

	private Subscription subs = Subscription.EMPTY;

	@Inject
	public MainController(@Named("mainWindow") Stage mainWindow, ExecutorService executorService, @Named("launchEventQueue") BlockingQueue<AppLaunchEvent> launchEventQueue, ExitUtil exitUtil, Localization localization,
						  VaultFactory vaultFactoy, ViewControllerLoader viewControllerLoader, UpgradeStrategies upgradeStrategies, VaultList vaults, AutoUnlocker autoUnlocker) {
		this.mainWindow = mainWindow;
		this.executorService = executorService;
		this.launchEventQueue = launchEventQueue;
		this.exitUtil = exitUtil;
		this.localization = localization;
		this.vaultFactoy = vaultFactoy;
		this.viewControllerLoader = viewControllerLoader;
		this.vaults = vaults;

		// derived bindings:
		this.isShowingSettings = Bindings.equal(SettingsController.class, EasyBind.monadic(activeController).map(ViewController::getClass));
		this.upgradeStrategyForSelectedVault = EasyBind.monadic(selectedVault).map(upgradeStrategies::getUpgradeStrategy);
		this.areAllVaultsLocked = Bindings.isEmpty(FXCollections.observableList(vaults, Vault::observables).filtered(Vault.NOT_LOCKED));

		EasyBind.subscribe(areAllVaultsLocked, exitUtil::updateTrayIcon);
		EasyBind.subscribe(areAllVaultsLocked, Platform::setImplicitExit);
		autoUnlocker.unlockAllSilently();

		try {
			Desktop.getDesktop().setPreferencesHandler(e -> {
				Platform.runLater(this::toggleShowSettings);
			});
		} catch (UnsupportedOperationException e) {
			LOG.info("Unable to setPreferencesHandler, probably not supported on this OS.");
		}
	}

	@FXML
	private ContextMenu vaultListCellContextMenu;

	@FXML
	private MenuItem changePasswordMenuItem;

	@FXML
	private ContextMenu addVaultContextMenu;

	@FXML
	private HBox root;

	@FXML
	private ListView<Vault> vaultList;

	@FXML
	private ToggleButton addVaultButton;

	@FXML
	private Button removeVaultButton;

	@FXML
	private ToggleButton settingsButton;

	@FXML
	private Pane contentPane;

	@FXML
	private Pane emptyListInstructions;

	@Override
	public void initialize() {
		vaultList.setItems(vaults);
		vaultList.getSelectionModel().clearSelection();
		vaultList.setOnKeyReleased(this::didPressKeyOnList);
		vaultList.setCellFactory(this::createDirecoryListCell);
		root.setOnKeyReleased(this::didPressKeyOnRoot);
		activeController.set(viewControllerLoader.load("/fxml/welcome.fxml"));
		selectedVault.bind(vaultList.getSelectionModel().selectedItemProperty());
		removeVaultButton.disableProperty().bind(canEditSelectedVault.not());
		emptyListInstructions.visibleProperty().bind(Bindings.isEmpty(vaults));
		changePasswordMenuItem.visibleProperty().bind(isSelectedVaultValid.and(Bindings.isNull(upgradeStrategyForSelectedVault)));

		subs = subs.and(EasyBind.subscribe(selectedVault, this::selectedVaultDidChange));
		subs = subs.and(EasyBind.subscribe(activeController, this::activeControllerDidChange));
		subs = subs.and(EasyBind.subscribe(isShowingSettings, settingsButton::setSelected));
		subs = subs.and(EasyBind.subscribe(addVaultContextMenu.showingProperty(), addVaultButton::setSelected));
	}

	@Override
	public Parent getRoot() {
		return root;
	}

	public void initStage(Stage stage) {
		stage.setScene(new Scene(getRoot()));
		stage.sizeToScene();
		stage.setTitle(localization.getString("app.name")); // set once before bind to avoid display bugs with Linux window managers
		stage.titleProperty().bind(windowTitle());
		stage.setResizable(false);
		loadFont("/css/ionicons.ttf");
		loadFont("/css/fontawesome-webfont.ttf");
		if (SystemUtils.IS_OS_MAC_OSX) {
			subs = subs.and(EasyBind.includeWhen(mainWindow.getScene().getRoot().getStyleClass(), ACTIVE_WINDOW_STYLE_CLASS, mainWindow.focusedProperty()));
			subs = subs.and(EasyBind.includeWhen(mainWindow.getScene().getRoot().getStyleClass(), INACTIVE_WINDOW_STYLE_CLASS, mainWindow.focusedProperty().not()));
			Application.setUserAgentStylesheet(getClass().getResource("/css/mac_theme.css").toString());
		} else if (SystemUtils.IS_OS_LINUX) {
			stage.getIcons().add(new Image(getClass().getResourceAsStream("/window_icon_512.png")));
			Application.setUserAgentStylesheet(getClass().getResource("/css/linux_theme.css").toString());
		} else if (SystemUtils.IS_OS_WINDOWS) {
			stage.getIcons().add(new Image(getClass().getResourceAsStream("/window_icon_32.png")));
			Application.setUserAgentStylesheet(getClass().getResource("/css/win_theme.css").toString());
		}
		exitUtil.initExitHandler(() -> Platform.runLater(this::gracefulShutdown));
		listenToFileOpenRequests(stage);
	}

	private void gracefulShutdown() {
		vaults.filtered(Vault.NOT_LOCKED).forEach(Vault::prepareForShutdown);
		if (!vaults.filtered(Vault.NOT_LOCKED).isEmpty()) {
			mainWindow.show(); // to keep the application open
			ButtonType tryAgainButtonType = new ButtonType(localization.getString("main.gracefulShutdown.button.tryAgain"));
			ButtonType forceShutdownButtonType = new ButtonType(localization.getString("main.gracefulShutdown.button.forceShutdown"));
			Alert gracefulShutdownDialog = DialogBuilderUtil.buildGracefulShutdownDialog(
					localization.getString("main.gracefulShutdown.dialog.title"), localization.getString("main.gracefulShutdown.dialog.header"), localization.getString("main.gracefulShutdown.dialog.content"),
					forceShutdownButtonType, ButtonType.CANCEL, forceShutdownButtonType, tryAgainButtonType);
			Optional<ButtonType> choice = gracefulShutdownDialog.showAndWait();
			choice.ifPresent(btnType -> {
				if (tryAgainButtonType.equals(btnType)) {
					gracefulShutdown();
				} else if (forceShutdownButtonType.equals(btnType)) {
					Platform.runLater(Platform::exit);
				} else {
					if (!vaults.filtered(Vault.NOT_LOCKED).isEmpty()) {
						showUnlockedView(vaults.get(0), false); //if there are still unlocked vaults, show one of them
					} else {
						showUnlockView(UnlockController.State.UNLOCKING); //otherwise show any vault
					}
				}
			});
		} else {
			Platform.runLater(Platform::exit);
		}
	}

	private void loadFont(String resourcePath) {
		try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
			Font.loadFont(in, 12.0);
		} catch (IOException e) {
			LOG.warn("Error loading font from path: " + resourcePath, e);
		}
	}

	private void listenToFileOpenRequests(Stage stage) {
		Tasks.create(launchEventQueue::take).onSuccess(event -> {
			stage.setIconified(false);
			stage.show();
			stage.toFront();
			stage.requestFocus();
			event.getPathsToOpen().forEach(path -> addVault(path, true));
		}).schedulePeriodically(executorService, Duration.ZERO, Duration.ZERO);
	}

	private ListCell<Vault> createDirecoryListCell(ListView<Vault> param) {
		final DirectoryListCell cell = new DirectoryListCell();
		cell.setVaultContextMenu(vaultListCellContextMenu);
		cell.setOnMouseClicked(this::didClickOnListCell);
		return cell;
	}

	// ****************************************
	// UI Events
	// ****************************************

	@FXML
	private void didClickAddVault() {
		if (addVaultContextMenu.isShowing()) {
			addVaultContextMenu.hide();
		} else {
			addVaultContextMenu.show(addVaultButton, Side.BOTTOM, 0.0, 0.0);
		}
	}

	@FXML
	private void didClickCreateNewVault() {
		final FileChooser fileChooser = new FileChooser();
		final File file = fileChooser.showSaveDialog(mainWindow);
		if (file == null) {
			return;
		}
		try {
			final Path vaultDir = file.toPath();
			if (Files.exists(vaultDir)) {
				try (Stream<Path> stream = Files.list(vaultDir)) {
					if (stream.filter(this::isNotHidden).findAny().isPresent()) {
						buildErrorDialog( //
								localization.getString("main.createVault.nonEmptyDir.title"), //
								localization.getString("main.createVault.nonEmptyDir.header"), //
								localization.getString("main.createVault.nonEmptyDir.content"), //
								ButtonType.OK).show();
						return;
					}
				}
			} else {
				Files.createDirectory(vaultDir);
			}
			addVault(vaultDir, true);
		} catch (IOException e) {
			LOG.error("Unable to create vault", e);
		}
	}

	private boolean isNotHidden(Path file) {
		return !file.getFileName().toString().startsWith(".");
	}

	@FXML
	private void didClickAddExistingVaults() {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Cryptomator Masterkey", "*.cryptomator"));
		final List<File> files = fileChooser.showOpenMultipleDialog(mainWindow);
		if (files != null) {
			for (final File file : files) {
				addVault(file.toPath(), true);
			}
		}
	}

	/**
	 * adds the given directory or selects it if it is already in the list of directories.
	 *
	 * @param path to a vault directory or masterkey file
	 */
	public void addVault(final Path path, boolean select) {
		final Path vaultPath;
		if (path != null && Files.isDirectory(path)) {
			vaultPath = path;
		} else if (path != null && Files.isReadable(path)) {
			vaultPath = path.getParent();
		} else {
			LOG.warn("Ignoring attempt to add vault with invalid path: {}", path);
			return;
		}

		final Vault vault = vaults.stream().filter(v -> v.getPath().equals(vaultPath)).findAny().orElseGet(() -> {
			VaultSettings vaultSettings = VaultSettings.withRandomId();
			vaultSettings.path().set(vaultPath);
			return vaultFactoy.get(vaultSettings);
		});

		if (!vaults.contains(vault)) {
			vaults.add(vault);
		}
		if (select) {
			vaultList.getSelectionModel().select(vault);
			activeController.get().focus();
		}
	}

	@FXML
	private void didClickRemoveSelectedEntry() {
		Alert confirmDialog = DialogBuilderUtil.buildConfirmationDialog( //
				localization.getString("main.directoryList.remove.confirmation.title"), //
				localization.getString("main.directoryList.remove.confirmation.header"), //
				localization.getString("main.directoryList.remove.confirmation.content"), //
				SystemUtils.IS_OS_MAC_OSX ? ButtonType.CANCEL : ButtonType.OK);

		Optional<ButtonType> choice = confirmDialog.showAndWait();
		if (ButtonType.OK.equals(choice.get())) {
			vaults.remove(selectedVault.get());
			if (vaults.isEmpty()) {
				activeController.set(viewControllerLoader.load("/fxml/welcome.fxml"));
			} else {
				activeController.get().focus();
			}
		}
	}

	@FXML
	private void didClickChangePassword() {
		showChangePasswordView();
	}

	@FXML
	private void didClickShowSettings() {
		toggleShowSettings();
	}

	private void toggleShowSettings() {
		if (isShowingSettings.get()) {
			showWelcomeView();
		} else {
			showPreferencesView();
		}
		vaultList.getSelectionModel().clearSelection();
	}

	// ****************************************
	// Binding Listeners
	// ****************************************

	private void activeControllerDidChange(ViewController newValue) {
		final Parent root = newValue.getRoot();
		contentPane.getChildren().clear();
		contentPane.getChildren().add(root);
	}

	private void selectedVaultDidChange(Vault newValue) {
		if (newValue == null) {
			return;
		}
		if (newValue.getState() != Vault.State.LOCKED) {
			this.showUnlockedView(newValue, false);
		} else if (!newValue.doesVaultDirectoryExist()) {
			this.showNotFoundView();
		} else if (newValue.isValidVaultDirectory() && upgradeStrategyForSelectedVault.isPresent()) {
			this.showUpgradeView();
		} else if (newValue.isValidVaultDirectory()) {
			this.showUnlockView(UnlockController.State.UNLOCKING);
		} else {
			this.showInitializeView();
		}
	}

	private void didPressKeyOnList(KeyEvent e) {
		if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
			activeController.get().focus();
		}
	}

	private void didPressKeyOnRoot(KeyEvent event) {
		boolean triggered;
		if (SystemUtils.IS_OS_MAC) {
			triggered = event.isMetaDown();
		} else {
			triggered = event.isControlDown() && !event.isAltDown();
		}
		if (triggered && event.getCode().isDigitKey()) {
			int digit = Integer.valueOf(event.getText());
			switch (digit) {
				case 0: {
					vaultList.getSelectionModel().clearSelection();
					showWelcomeView();
					return;
				}
				default: {
					vaultList.getSelectionModel().select(digit - 1);
					activeController.get().focus();
					return;
				}
			}
		}
	}

	private void didClickOnListCell(MouseEvent e) {
		if (MouseEvent.MOUSE_CLICKED.equals(e.getEventType()) && e.getSource() instanceof Cell && ((Cell<?>) e.getSource()).isSelected()) {
			activeController.get().focus();
		}
	}

	// ****************************************
	// Public Bindings
	// ****************************************

	public Binding<String> windowTitle() {
		return EasyBind.monadic(selectedVault).flatMap(Vault::name).orElse(localization.getString("app.name"));
	}

	// ****************************************
	// Subcontroller for right panel
	// ****************************************

	private void showWelcomeView() {
		activeController.set(viewControllerLoader.load("/fxml/welcome.fxml"));
	}

	private void showPreferencesView() {
		activeController.set(viewControllerLoader.load("/fxml/settings.fxml"));
	}

	private void showNotFoundView() {
		activeController.set(viewControllerLoader.load("/fxml/notfound.fxml"));
	}

	private void showInitializeView() {
		final InitializeController ctrl = viewControllerLoader.load("/fxml/initialize.fxml");
		ctrl.setVault(selectedVault.get());
		ctrl.setListener(this::didInitialize);
		activeController.set(ctrl);
	}

	public void didInitialize() {
		showUnlockView(UnlockController.State.INITIALIZED);
		activeController.get().focus();
	}

	private void showUpgradeView() {
		final UpgradeController ctrl = viewControllerLoader.load("/fxml/upgrade.fxml");
		ctrl.setVault(selectedVault.get());
		ctrl.setListener(this::didUpgrade);
		activeController.set(ctrl);
	}

	public void didUpgrade() {
		showUnlockView(UnlockController.State.UPGRADED);
		activeController.get().focus();
	}

	private void showUnlockView(UnlockController.State state) {
		final UnlockController ctrl = viewControllerLoader.load("/fxml/unlock.fxml");
		ctrl.setVault(selectedVault.get(), state);
		ctrl.setListener(this::didUnlock);
		activeController.set(ctrl);
	}

	public void didUnlock(Vault vault) {
		if (vault.equals(selectedVault.getValue())) {
			this.showUnlockedView(vault, vault.getVaultSettings().revealAfterMount().getValue());
		}
	}

	private void showUnlockedView(Vault vault, boolean reveal) {
		final UnlockedController ctrl = unlockedVaults.computeIfAbsent(vault, k -> viewControllerLoader.load("/fxml/unlocked.fxml"));
		ctrl.setVault(vault);
		ctrl.setListener(this::didLock);
		if (reveal) {
			ctrl.revealVault(vault);
		}
		activeController.set(ctrl);
	}

	public void didLock(UnlockedController ctrl) {
		unlockedVaults.remove(ctrl.getVault());
		if (ctrl.getVault().getId() == selectedVault.get().getId()) {
			showUnlockView(UnlockController.State.UNLOCKING);
		}
		activeController.get().focus();
	}

	private void showChangePasswordView() {
		final ChangePasswordController ctrl = viewControllerLoader.load("/fxml/change_password.fxml");
		ctrl.setVault(selectedVault.get());
		ctrl.setListener(this::didChangePassword);
		activeController.set(ctrl);
		Platform.runLater(ctrl::focus);
	}

	public void didChangePassword() {
		showUnlockView(UnlockController.State.PASSWORD_CHANGED);
		activeController.get().focus();
	}

}

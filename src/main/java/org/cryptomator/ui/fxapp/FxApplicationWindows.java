package org.cryptomator.ui.fxapp;

import com.google.common.base.Preconditions;
import dagger.Lazy;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.integrations.tray.TrayIntegrationProvider;
import org.cryptomator.ui.controls.CustomDialog;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.cryptomator.ui.error.ErrorComponent;
import org.cryptomator.ui.lock.LockComponent;
import org.cryptomator.ui.mainwindow.MainWindowComponent;
import org.cryptomator.ui.preferences.PreferencesComponent;
import org.cryptomator.ui.preferences.SelectedPreferencesTab;
import org.cryptomator.ui.quit.QuitComponent;
import org.cryptomator.ui.sharevault.ShareVaultComponent;
import org.cryptomator.ui.unlock.UnlockComponent;
import org.cryptomator.ui.unlock.UnlockWorkflow;
import org.cryptomator.ui.updatereminder.UpdateReminderComponent;
import org.cryptomator.ui.vaultoptions.SelectedVaultOptionsTab;
import org.cryptomator.ui.vaultoptions.VaultOptionsComponent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.awt.Desktop;
import java.awt.desktop.AppReopenedListener;
import java.awt.desktop.QuitResponse;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

@FxApplicationScoped
public class FxApplicationWindows {

	private static final Logger LOG = LoggerFactory.getLogger(FxApplicationWindows.class);

	private final Stage primaryStage;
	private final Optional<TrayIntegrationProvider> trayIntegration;
	private final Lazy<MainWindowComponent> mainWindow;
	private final Lazy<PreferencesComponent> preferencesWindow;
	private final QuitComponent.Builder quitWindowBuilder;
	private final UnlockComponent.Factory unlockWorkflowFactory;
	private final UpdateReminderComponent.Factory updateReminderWindowBuilder;
	private final LockComponent.Factory lockWorkflowFactory;
	private final ErrorComponent.Factory errorWindowFactory;
	private final ExecutorService executor;
	private final VaultOptionsComponent.Factory vaultOptionsWindow;
	private final ShareVaultComponent.Factory shareVaultWindow;
	private final FilteredList<Window> visibleWindows;
	private final CustomDialog.Builder customDialog;

	@Inject
	public FxApplicationWindows(@PrimaryStage Stage primaryStage, //
								Optional<TrayIntegrationProvider> trayIntegration, //
								Lazy<MainWindowComponent> mainWindow, //
								Lazy<PreferencesComponent> preferencesWindow, //
								QuitComponent.Builder quitWindowBuilder, //
								UnlockComponent.Factory unlockWorkflowFactory, //
								UpdateReminderComponent.Factory updateReminderWindowBuilder, //
								LockComponent.Factory lockWorkflowFactory, //
								ErrorComponent.Factory errorWindowFactory, //
								VaultOptionsComponent.Factory vaultOptionsWindow, //
								ShareVaultComponent.Factory shareVaultWindow, //
								ExecutorService executor, //
								CustomDialog.Builder customDialog) {
		this.primaryStage = primaryStage;
		this.trayIntegration = trayIntegration;
		this.mainWindow = mainWindow;
		this.preferencesWindow = preferencesWindow;
		this.quitWindowBuilder = quitWindowBuilder;
		this.unlockWorkflowFactory = unlockWorkflowFactory;
		this.updateReminderWindowBuilder = updateReminderWindowBuilder;
		this.lockWorkflowFactory = lockWorkflowFactory;
		this.errorWindowFactory = errorWindowFactory;
		this.executor = executor;
		this.vaultOptionsWindow = vaultOptionsWindow;
		this.shareVaultWindow = shareVaultWindow;
		this.visibleWindows = Window.getWindows().filtered(Window::isShowing);
		this.customDialog = customDialog;
	}

	public void initialize() {
		Preconditions.checkState(Desktop.isDesktopSupported(), "java.awt.Desktop not supported");
		Desktop desktop = Desktop.getDesktop();

		// register preferences shortcut
		if (desktop.isSupported(Desktop.Action.APP_PREFERENCES)) {
			desktop.setPreferencesHandler(evt -> showPreferencesWindow(SelectedPreferencesTab.ANY));
		}

		// register preferences shortcut
		if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
			desktop.setAboutHandler(evt -> showPreferencesWindow(SelectedPreferencesTab.ABOUT));
		}

		// register app reopen listener
		if (desktop.isSupported(Desktop.Action.APP_EVENT_REOPENED)) {
			desktop.addAppEventListener((AppReopenedListener) e -> showMainWindow());
		}

		// observe visible windows
		if (trayIntegration.isPresent()) {
			visibleWindows.addListener(this::visibleWindowsChanged);
		}
	}

	private void visibleWindowsChanged(ListChangeListener.Change<? extends Window> change) {
		int visibleWindows = change.getList().size();
		LOG.debug("visible windows: {}", visibleWindows);
		if (visibleWindows > 0) {
			trayIntegration.ifPresent(TrayIntegrationProvider::restoredFromTray);
		} else {
			trayIntegration.ifPresent(TrayIntegrationProvider::minimizedToTray);
		}
	}

	public CompletionStage<Stage> showMainWindow() {
		return CompletableFuture.supplyAsync(mainWindow.get()::showMainWindow, Platform::runLater).whenComplete(this::reportErrors);
	}

	public CompletionStage<Stage> showPreferencesWindow(SelectedPreferencesTab selectedTab) {
		return CompletableFuture.supplyAsync(() -> preferencesWindow.get().showPreferencesWindow(selectedTab), Platform::runLater).whenComplete(this::reportErrors);
	}

	public void showShareVaultWindow(Vault vault) {
		CompletableFuture.runAsync(() -> shareVaultWindow.create(vault).showShareVaultWindow(), Platform::runLater);
	}

	public CompletionStage<Stage> showVaultOptionsWindow(Vault vault, SelectedVaultOptionsTab tab) {
		return showMainWindow().thenApplyAsync((window) -> vaultOptionsWindow.create(vault).showVaultOptionsWindow(tab), Platform::runLater).whenComplete(this::reportErrors);
	}

	public void showQuitWindow(QuitResponse response, boolean forced) {
			CompletableFuture.runAsync(() -> quitWindowBuilder.build().showQuitWindow(response,forced), Platform::runLater);
	}

	public void checkAndShowUpdateReminderWindow() {
		CompletableFuture.runAsync(() -> updateReminderWindowBuilder.create().checkAndShowUpdateReminderWindow(), Platform::runLater);
	}

	public void showDokanySupportEndWindow() {
		CompletableFuture.runAsync(() -> {
			customDialog.setOwner(mainWindow.get().window())
					.setTitleKey("dokanySupportEnd.title")
					.setMessageKey("dokanySupportEnd.message")
					.setDescriptionKey("dokanySupportEnd.description")
					.setIcon(FontAwesome5Icon.QUESTION)
					.setOkButtonKey("generic.button.close")
					.setCancelButtonKey("dokanySupportEnd.preferencesBtn")
					.setOkAction(Stage::close) //
					.setCancelAction(v -> {
								showPreferencesWindow(SelectedPreferencesTab.VOLUME);
								v.close();
							}) //
					.build()
					.showAndWait();
		}, Platform::runLater);
	}

	public CompletionStage<Void> startUnlockWorkflow(Vault vault, @Nullable Stage owner) {
		return CompletableFuture.supplyAsync(() -> {
					Preconditions.checkState(vault.stateProperty().transition(VaultState.Value.LOCKED, VaultState.Value.PROCESSING), "Vault not locked.");
					LOG.debug("Start unlock workflow for {}", vault.getDisplayName());
					return unlockWorkflowFactory.create(vault, owner).unlockWorkflow();
				}, Platform::runLater) //
				.thenAcceptAsync(UnlockWorkflow::run, executor)
				.exceptionally(e -> {
					showErrorWindow(e, owner == null ? primaryStage : owner, null);
					return null;
				});
	}

	public CompletionStage<Void> startLockWorkflow(Vault vault, @Nullable Stage owner) {
		return CompletableFuture.supplyAsync(() -> {
					Preconditions.checkState(vault.stateProperty().transition(VaultState.Value.UNLOCKED, VaultState.Value.PROCESSING), "Vault not unlocked.");
					LOG.debug("Start lock workflow for {}", vault.getDisplayName());
					return lockWorkflowFactory.create(vault, owner).lockWorkflow();
				}, Platform::runLater) //
				.thenCompose(lockWorkflow -> CompletableFuture.runAsync(lockWorkflow, executor)) //
				.exceptionally(e -> {
					showErrorWindow(e, owner == null ? primaryStage : owner, null);
					return null;
				});
	}

	/**
	 * Displays the generic error scene in the given window.
	 *
	 * @param cause The exception to show
	 * @param window What window to display the scene in
	 * @param previousScene To what scene to return to when pressing "back". Back button will be hidden, if <code>null</code>
	 * @return A
	 */
	public CompletionStage<Stage> showErrorWindow(Throwable cause, Stage window, @Nullable Scene previousScene) {
		return CompletableFuture.supplyAsync(() -> errorWindowFactory.create(cause, window, previousScene).show(), Platform::runLater).whenComplete(this::reportErrors);
	}

	private void reportErrors(@Nullable Stage stage, @Nullable Throwable error) {
		if (error != null) {
			LOG.error("Failed to display stage", error);
		}
	}

}

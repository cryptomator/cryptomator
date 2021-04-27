package org.cryptomator.ui.health;

import dagger.Lazy;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptofs.VaultConfigLoadException;
import org.cryptomator.cryptofs.VaultKeyInvalidException;
import org.cryptomator.cryptofs.health.api.HealthCheck;
import org.cryptomator.cryptofs.health.dirid.DirIdCheck;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.api.MasterkeyLoadingFailedException;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.keyloading.KeyLoadingStrategy;
import org.cryptomator.ui.unlock.UnlockCancelledException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.stage.Stage;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

@HealthCheckScoped
public class StartController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(StartController.class);

	private final Stage window;
	private final Collection<HealthCheck> availableChecks;
	private final Map<HealthCheck, BooleanProperty> availableCheckListSelectProperties;
	private final Optional<VaultConfig.UnverifiedVaultConfig> unverifiedVaultConfig;
	private final KeyLoadingStrategy keyLoadingStrategy;
	private final ExecutorService executor;
	private final AtomicReference<Masterkey> masterkeyRef;
	private final AtomicReference<VaultConfig> vaultConfigRef;
	private final Collection<HealthCheck> selectedChecks;
	private final Lazy<Scene> checkScene;

	/* FXML */
	public ListView availableChecksList;
	public RadioButton customCheckSetButton;
	public RadioButton quickCheckSetButton;
	public RadioButton fullCheckSetButton;
	public ToggleGroup checksSetToggleGroup;

	@Inject
	public StartController(@HealthCheckWindow Vault vault, @HealthCheckWindow Stage window, @HealthCheckWindow KeyLoadingStrategy keyLoadingStrategy, ExecutorService executor, AtomicReference<Masterkey> masterkeyRef, AtomicReference<VaultConfig> vaultConfigRef, Collection<HealthCheck> selectedChecks, @FxmlScene(FxmlFile.HEALTH_CHECK) Lazy<Scene> checkScene) {
		this.window = window;
		this.unverifiedVaultConfig = vault.getUnverifiedVaultConfig(); //TODO: prevent workflow at all, if the vault config is emtpy
		this.keyLoadingStrategy = keyLoadingStrategy;
		this.executor = executor;
		this.masterkeyRef = masterkeyRef;
		this.vaultConfigRef = vaultConfigRef;
		this.selectedChecks = selectedChecks;
		this.checkScene = checkScene;
		this.availableChecks = HealthCheck.allChecks();
		this.availableCheckListSelectProperties = new HashMap<>();

		availableChecks.forEach(check -> availableCheckListSelectProperties.put(check, new SimpleBooleanProperty(false)));
	}

	public void initialize() {
		availableChecksList.setItems(FXCollections.observableList(availableChecks.stream().toList()));
		availableChecksList.setCellFactory(CheckBoxListCell.forListView(availableCheckListSelectProperties::get));
		availableChecksList.setEditable(false);

		var availableCheckIds = availableChecks.stream().map(HealthCheck::identifier).toList();

		if (PredefinedCheckSet.QUICK.getCheckIds().stream().anyMatch(checkId -> !availableCheckIds.contains(checkId))) {
			quickCheckSetButton.setVisible(false);
			quickCheckSetButton.setManaged(false);
		}
		if (PredefinedCheckSet.FULL.getCheckIds().stream().anyMatch(checkId -> !availableCheckIds.contains(checkId))) {
			fullCheckSetButton.setVisible(false);
			fullCheckSetButton.setManaged(false);
		}

		quickCheckSetButton.setUserData(PredefinedCheckSet.QUICK);
		fullCheckSetButton.setUserData(PredefinedCheckSet.FULL);
		customCheckSetButton.setUserData(PredefinedCheckSet.CUSTOM);
	}

	@FXML
	public void close() {
		LOG.trace("StartController.close()");
		window.close();
	}

	@FXML
	public void next() {
		switch ((PredefinedCheckSet) checksSetToggleGroup.getSelectedToggle().getUserData()) {
			case FULL -> selectedChecks.addAll(availableChecks);
			case QUICK -> selectedChecks.addAll(availableChecks.stream().filter(check -> PredefinedCheckSet.QUICK.getCheckIds().contains(check.identifier())).toList());
			case CUSTOM -> availableCheckListSelectProperties.forEach((check, selected) -> {
				if (selected.get()) {
					selectedChecks.add(check);
				}
			});
		}
		LOG.trace("StartController.next()");
		executor.submit(this::loadKey);
	}

	private void loadKey() {
		assert !Platform.isFxApplicationThread();
		assert unverifiedVaultConfig.isPresent();
		try (var masterkey = keyLoadingStrategy.masterkeyLoader().loadKey(unverifiedVaultConfig.orElseThrow().getKeyId())) {
			var unverifiedCfg = unverifiedVaultConfig.get();
			var verifiedCfg = unverifiedCfg.verify(masterkey.getEncoded(), unverifiedCfg.allegedVaultVersion());
			vaultConfigRef.set(verifiedCfg);
			var old = masterkeyRef.getAndSet(masterkey.clone());
			if (old != null) {
				old.destroy();
			}
			Platform.runLater(this::loadedKey);
		} catch (MasterkeyLoadingFailedException e) {
			if (keyLoadingStrategy.recoverFromException(e)) {
				// retry
				loadKey();
			} else {
				Platform.runLater(() -> loadingKeyFailed(e));
			}
		} catch (VaultKeyInvalidException e) {
			Platform.runLater(() -> loadingKeyFailed(e));
		} catch (VaultConfigLoadException e) {
			Platform.runLater(() -> loadingKeyFailed(e));
		}
	}

	private void loadedKey() {
		LOG.debug("Loaded valid key");
		window.setScene(checkScene.get());
	}

	private void loadingKeyFailed(Exception e) {
		if (e instanceof UnlockCancelledException) {
			// ok
		} else if (e instanceof VaultKeyInvalidException) {
			LOG.error("Invalid key");
			// TODO show error screen
		} else {
			LOG.error("Failed to load key.", e);
			// TODO show error screen
		}
	}

	public boolean isInvalidConfig() {
		return unverifiedVaultConfig.isEmpty();
	}

	public BooleanBinding anyCheckSetSelectedProperty() {
		return checksSetToggleGroup.selectedToggleProperty().isNotNull();
	}

	public boolean isAnyCheckSetSelected() {
		return anyCheckSetSelectedProperty().get();
	}

	enum PredefinedCheckSet {
		QUICK(DirIdCheck.class.getCanonicalName()), //TODO: get identifier via static method?
		FULL(DirIdCheck.class.getCanonicalName()),
		CUSTOM();

		private Collection<String> checkIds;

		PredefinedCheckSet(String... checkIds) {
			this.checkIds = Set.of(checkIds);
		}

		Collection<String> getCheckIds() {
			return checkIds;
		}
	}
}

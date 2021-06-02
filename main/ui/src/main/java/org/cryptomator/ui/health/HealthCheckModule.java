package org.cryptomator.ui.health;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptofs.health.api.HealthCheck;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlLoaderFactory;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.StageFactory;
import org.cryptomator.ui.keyloading.KeyLoadingComponent;
import org.cryptomator.ui.keyloading.KeyLoadingStrategy;
import org.cryptomator.ui.mainwindow.MainWindow;

import javax.inject.Provider;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

@Module(subcomponents = {KeyLoadingComponent.class})
abstract class HealthCheckModule {

	@Provides
	@HealthCheckScoped
	static AtomicReference<Masterkey> provideMasterkeyRef() {
		return new AtomicReference<>();
	}

	@Provides
	@HealthCheckScoped
	static AtomicReference<VaultConfig> provideVaultConfigRef() {
		return new AtomicReference<>();
	}

	@Provides
	@HealthCheckScoped
	static Collection<HealthCheck> provideAvailableHealthChecks() {
		return HealthCheck.allChecks();
	}

	@Provides
	@HealthCheckScoped
	static ObjectProperty<HealthCheckTask> provideSelectedHealthCheckTask() {
		return new SimpleObjectProperty<>();
	}

	/* Only inject with Lazy-Wrapper!*/
	@Provides
	@HealthCheckScoped
	static Collection<HealthCheckTask> provideAvailableHealthCheckTasks(Collection<HealthCheck> availableHealthChecks, @HealthCheckWindow Vault vault, AtomicReference<Masterkey> masterkeyRef, AtomicReference<VaultConfig> vaultConfigRef, SecureRandom csprng, ResourceBundle resourceBundle) {
		return availableHealthChecks.stream().map(check -> new HealthCheckTask(vault.getPath(), vaultConfigRef.get(), masterkeyRef.get(), csprng, check, resourceBundle)).toList();
	}

	@Provides
	@HealthCheckWindow
	@HealthCheckScoped
	static KeyLoadingStrategy provideKeyLoadingStrategy(KeyLoadingComponent.Builder compBuilder, @HealthCheckWindow Vault vault, @HealthCheckWindow Stage window) {
		return compBuilder.vault(vault).window(window).build().keyloadingStrategy();
	}

	@Provides
	@HealthCheckWindow
	@HealthCheckScoped
	static FxmlLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FxmlLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@HealthCheckWindow
	@HealthCheckScoped
	static Stage provideStage(StageFactory factory, @MainWindow Stage owner, ResourceBundle resourceBundle, ChangeListener<Boolean> showingListener) {
		Stage stage = factory.create();
		stage.setTitle(resourceBundle.getString("health.title"));
		stage.setResizable(true);
		stage.initModality(Modality.WINDOW_MODAL);
		stage.initOwner(owner);
		stage.showingProperty().addListener(showingListener); // bind masterkey lifecycle to window
		return stage;
	}

	@Provides
	@HealthCheckScoped
	static ChangeListener<Boolean> provideWindowShowingChangeListener(AtomicReference<Masterkey> masterkey) {
		return (observable, wasShowing, isShowing) -> {
			if (!isShowing) {
				Optional.ofNullable(masterkey.getAndSet(null)).ifPresent(Masterkey::destroy);
			}
		};
	}

	@Provides
	@FxmlScene(FxmlFile.HEALTH_START)
	@HealthCheckScoped
	static Scene provideHealthStartScene(@HealthCheckWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.HEALTH_START);
	}

	@Provides
	@FxmlScene(FxmlFile.HEALTH_CHECK_LIST)
	@HealthCheckScoped
	static Scene provideHealthCheckListScene(@HealthCheckWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.HEALTH_CHECK_LIST);
	}

	@Binds
	@IntoMap
	@FxControllerKey(StartController.class)
	abstract FxController bindStartController(StartController controller);

	@Binds
	@IntoMap
	@FxControllerKey(CheckListController.class)
	abstract FxController bindCheckController(CheckListController controller);

	@Binds
	@IntoMap
	@FxControllerKey(CheckDetailController.class)
	abstract FxController bindCheckDetailController(CheckDetailController controller);

	@Binds
	@IntoMap
	@FxControllerKey(ResultListCellController.class)
	abstract FxController bindResultListCellController(ResultListCellController controller);

}

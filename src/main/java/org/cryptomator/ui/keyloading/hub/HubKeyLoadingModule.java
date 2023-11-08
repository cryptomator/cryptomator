package org.cryptomator.ui.keyloading.hub;

import com.google.common.io.BaseEncoding;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import org.cryptomator.common.settings.DeviceKey;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptolib.common.MessageDigestSupplier;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlLoaderFactory;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.cryptomator.ui.keyloading.KeyLoadingScoped;
import org.cryptomator.ui.keyloading.KeyLoadingStrategy;

import javax.inject.Named;
import javafx.scene.Scene;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Module
public abstract class HubKeyLoadingModule {

	@Provides
	@KeyLoadingScoped
	static HubConfig provideHubConfig(@KeyLoading Vault vault) {
		try {
			return vault.getVaultConfigCache().get().getHeader("hub", HubConfig.class);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Provides
	@KeyLoadingScoped
	@Named("windowTitle")
	static String provideWindowTitle(@KeyLoading Vault vault, ResourceBundle resourceBundle) {
		return String.format(resourceBundle.getString("unlock.title"), vault.getDisplayName());
	}


	@Provides
	@KeyLoadingScoped
	@Named("deviceId")
	static String provideDeviceId(DeviceKey deviceKey) {
		var publicKey = Objects.requireNonNull(deviceKey.get()).getPublic().getEncoded();
		try (var instance = MessageDigestSupplier.SHA256.instance()) {
			var hashedKey = instance.get().digest(publicKey);
			return BaseEncoding.base16().encode(hashedKey);
		}
	}

	@Provides
	@Named("bearerToken")
	@KeyLoadingScoped
	static AtomicReference<String> provideBearerTokenRef() {
		return new AtomicReference<>();
	}

	@Provides
	@KeyLoadingScoped
	static CompletableFuture<ReceivedKey> provideResult() {
		return new CompletableFuture<>();
	}

	@Binds
	@IntoMap
	@KeyLoadingScoped
	@StringKey(HubKeyLoadingStrategy.SCHEME_HUB_HTTP)
	abstract KeyLoadingStrategy bindHubKeyLoadingStrategyToHubHttp(HubKeyLoadingStrategy strategy);

	@Binds
	@IntoMap
	@KeyLoadingScoped
	@StringKey(HubKeyLoadingStrategy.SCHEME_HUB_HTTPS)
	abstract KeyLoadingStrategy bindHubKeyLoadingStrategyToHubHttps(HubKeyLoadingStrategy strategy);

	@Provides
	@FxmlScene(FxmlFile.HUB_NO_KEYCHAIN)
	@KeyLoadingScoped
	static Scene provideHubNoKeychainScene(@KeyLoading FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.HUB_NO_KEYCHAIN);
	}

	@Provides
	@FxmlScene(FxmlFile.HUB_AUTH_FLOW)
	@KeyLoadingScoped
	static Scene provideHubAuthFlowScene(@KeyLoading FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.HUB_AUTH_FLOW);
	}

	@Provides
	@FxmlScene(FxmlFile.HUB_INVALID_LICENSE)
	@KeyLoadingScoped
	static Scene provideInvalidLicenseScene(@KeyLoading FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.HUB_INVALID_LICENSE);
	}

	@Provides
	@FxmlScene(FxmlFile.HUB_RECEIVE_KEY)
	@KeyLoadingScoped
	static Scene provideHubReceiveKeyScene(@KeyLoading FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.HUB_RECEIVE_KEY);
	}

	@Provides
	@FxmlScene(FxmlFile.HUB_LEGACY_REGISTER_DEVICE)
	@KeyLoadingScoped
	static Scene provideHubLegacyRegisterDeviceScene(@KeyLoading FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.HUB_LEGACY_REGISTER_DEVICE);
	}


	@Provides
	@FxmlScene(FxmlFile.HUB_REGISTER_SUCCESS)
	@KeyLoadingScoped
	static Scene provideHubRegisterSuccessScene(@KeyLoading FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.HUB_REGISTER_SUCCESS);
	}

	@Provides
	@FxmlScene(FxmlFile.HUB_REGISTER_FAILED)
	@KeyLoadingScoped
	static Scene provideHubRegisterFailedScene(@KeyLoading FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.HUB_REGISTER_FAILED);
	}

	@Provides
	@FxmlScene(FxmlFile.HUB_SETUP_DEVICE)
	@KeyLoadingScoped
	static Scene provideHubRegisterDeviceScene(@KeyLoading FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.HUB_SETUP_DEVICE);
	}

	@Provides
	@FxmlScene(FxmlFile.HUB_UNAUTHORIZED_DEVICE)
	@KeyLoadingScoped
	static Scene provideHubUnauthorizedDeviceScene(@KeyLoading FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.HUB_UNAUTHORIZED_DEVICE);
	}

	@Provides
	@FxmlScene(FxmlFile.HUB_REQUIRE_ACCOUNT_INIT)
	@KeyLoadingScoped
	static Scene provideRequireAccountInitScene(@KeyLoading FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.HUB_REQUIRE_ACCOUNT_INIT);
	}

	@Binds
	@IntoMap
	@FxControllerKey(NoKeychainController.class)
	abstract FxController bindNoKeychainController(NoKeychainController controller);

	@Binds
	@IntoMap
	@FxControllerKey(AuthFlowController.class)
	abstract FxController bindAuthFlowController(AuthFlowController controller);

	@Binds
	@IntoMap
	@FxControllerKey(InvalidLicenseController.class)
	abstract FxController bindInvalidLicenseController(InvalidLicenseController controller);

	@Binds
	@IntoMap
	@FxControllerKey(ReceiveKeyController.class)
	abstract FxController bindReceiveKeyController(ReceiveKeyController controller);

	@Binds
	@IntoMap
	@FxControllerKey(RegisterDeviceController.class)
	abstract FxController bindRegisterDeviceController(RegisterDeviceController controller);

	@Binds
	@IntoMap
	@FxControllerKey(LegacyRegisterDeviceController.class)
	abstract FxController bindLegacyRegisterDeviceController(LegacyRegisterDeviceController controller);

	@Binds
	@IntoMap
	@FxControllerKey(RegisterSuccessController.class)
	abstract FxController bindRegisterSuccessController(RegisterSuccessController controller);

	@Binds
	@IntoMap
	@FxControllerKey(RegisterFailedController.class)
	abstract FxController bindRegisterFailedController(RegisterFailedController controller);

	@Binds
	@IntoMap
	@FxControllerKey(UnauthorizedDeviceController.class)
	abstract FxController bindUnauthorizedDeviceController(UnauthorizedDeviceController controller);

	@Binds
	@IntoMap
	@FxControllerKey(RequireAccountInitController.class)
	abstract FxController bindRequireAccountInitController(RequireAccountInitController controller);
}

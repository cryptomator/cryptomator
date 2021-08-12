package org.cryptomator.ui.keyloading.hub;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlLoaderFactory;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.NewPasswordController;
import org.cryptomator.ui.common.PasswordStrengthUtil;
import org.cryptomator.ui.common.UserInteractionLock;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.cryptomator.ui.keyloading.KeyLoadingScoped;
import org.cryptomator.ui.keyloading.KeyLoadingStrategy;

import javax.inject.Named;
import javafx.scene.Scene;
import java.net.URI;
import java.security.KeyPair;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

@Module
public abstract class HubKeyLoadingModule {

	public enum HubLoadingResult {
		SUCCESS,
		FAILED,
		CANCELLED
	}

	@Provides
	@KeyLoadingScoped
	static AtomicReference<KeyPair> provideKeyPair() {
		return new AtomicReference<>();
	}

	@Provides
	@Named("bearerToken")
	@KeyLoadingScoped
	static AtomicReference<String> provideBearerTokenRef() {
		return new AtomicReference<>();
	}

	@Provides
	@KeyLoadingScoped
	static AtomicReference<EciesParams> provideEciesParamsRef() {
		return new AtomicReference<>();
	}

	@Provides
	@KeyLoadingScoped
	static UserInteractionLock<HubLoadingResult> provideResultLock() {
		return new UserInteractionLock<>(null);
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
	@FxmlScene(FxmlFile.HUB_P12)
	@KeyLoadingScoped
	static Scene provideHubP12LoadingScene(@KeyLoading FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.HUB_P12);
	}

	@Provides
	@FxmlScene(FxmlFile.HUB_AUTH_FLOW)
	@KeyLoadingScoped
	static Scene provideHubAuthFlowScene(@KeyLoading FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.HUB_AUTH_FLOW);
	}


	@Provides
	@FxmlScene(FxmlFile.HUB_RECEIVE_KEY)
	@KeyLoadingScoped
	static Scene provideHubReceiveKeyScene(@KeyLoading FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.HUB_RECEIVE_KEY);
	}

	@Binds
	@IntoMap
	@FxControllerKey(P12Controller.class)
	abstract FxController bindP12Controller(P12Controller controller);

	@Binds
	@IntoMap
	@FxControllerKey(P12LoadController.class)
	abstract FxController bindP12LoadController(P12LoadController controller);

	@Binds
	@IntoMap
	@FxControllerKey(P12CreateController.class)
	abstract FxController bindP12CreateController(P12CreateController controller);

	@Binds
	@IntoMap
	@FxControllerKey(AuthFlowController.class)
	abstract FxController bindAuthFlowController(AuthFlowController controller);

	@Provides
	@IntoMap
	@FxControllerKey(NewPasswordController.class)
	static FxController provideNewPasswordController(ResourceBundle resourceBundle, PasswordStrengthUtil strengthRater) {
		return new NewPasswordController(resourceBundle, strengthRater);
	}

	@Binds
	@IntoMap
	@FxControllerKey(ReceiveKeyController.class)
	abstract FxController bindReceiveKeyController(ReceiveKeyController controller);

}

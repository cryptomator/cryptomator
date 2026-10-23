package org.cryptomator.ui.preferences;

import org.cryptomator.JavaFXUtil;
import org.cryptomator.common.Environment;
import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.common.settings.DeviceKey;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.SettingsProvider;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.integrations.keychain.KeychainAccessException;
import org.cryptomator.integrations.keychain.KeychainAccessProvider;
import org.cryptomator.integrations.revealpath.RevealPathService;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import javafx.application.Platform;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@EnabledOnOs({OS.MAC, OS.LINUX}) // other platforms switch the keychain backend without migrating entries
public class GeneralPreferencesControllerTest {

	private final Queue<Runnable> queuedTasks = new ArrayDeque<>();
	private final FxApplicationWindows appWindows = Mockito.mock(FxApplicationWindows.class);
	private final FirstKeychain firstKeychain = new FirstKeychain();
	private final SecondKeychain secondKeychain = new SecondKeychain();
	private Settings settings;
	private GeneralPreferencesController controller;

	@BeforeAll
	public static void startup() throws InterruptedException {
		var isRunning = JavaFXUtil.startPlatform();
		Assumptions.assumeTrue(isRunning);
	}

	@BeforeEach
	public void setup() throws KeychainAccessException {
		settings = Settings.create(Mockito.mock(SettingsProvider.class), Mockito.mock(Environment.class));
		settings.useKeychain.set(true);
		settings.keychainProvider.set(FirstKeychain.class.getName());
		var vaultSettings = VaultSettings.withRandomId();
		vaultSettings.displayName.set("Vault");
		settings.directories.add(vaultSettings);
		firstKeychain.storePassphrase(vaultSettings.id, "Vault", "asd");
		firstKeychain.storePassphrase(DeviceKey.KEYCHAIN_KEY, "Device Key", "qwe");
		var executor = Mockito.mock(ExecutorService.class);
		Mockito.doAnswer(invocation -> queuedTasks.add(invocation.getArgument(0))).when(executor).execute(ArgumentMatchers.any());
		controller = new GeneralPreferencesController(Mockito.mock(Stage.class), settings, Optional.empty(), List.of(firstKeychain, secondKeychain), Mockito.mock(KeychainManager.class), Mockito.mock(RevealPathService.class), Mockito.mock(Environment.class), appWindows, executor);
	}

	@Test
	public void testKeepsUnavailableKeychainProvider() throws InterruptedException {
		settings.keychainProvider.set("org.example.UnavailableKeychainAccess");

		initializeController();

		Assertions.assertEquals("org.example.UnavailableKeychainAccess", settings.keychainProvider.get());
		Assertions.assertSame(firstKeychain, controller.keychainBackendChoiceBox.getValue());
	}

	@Test
	public void testCommitsKeychainProviderAfterMigration() throws InterruptedException, KeychainAccessException {
		initializeController();

		runOnFxThread(() -> controller.keychainBackendChoiceBox.setValue(secondKeychain));

		Assertions.assertEquals(FirstKeychain.class.getName(), settings.keychainProvider.get());
		Assertions.assertTrue(controller.keychainBackendChoiceBox.isDisabled());

		runQueuedTasks();

		Assertions.assertEquals(SecondKeychain.class.getName(), settings.keychainProvider.get());
		Assertions.assertFalse(controller.keychainBackendChoiceBox.isDisabled());
		Assertions.assertArrayEquals("qwe".toCharArray(), secondKeychain.loadPassphrase(DeviceKey.KEYCHAIN_KEY));
	}

	@Test
	public void testRevertsKeychainProviderIfMigrationFails() throws InterruptedException {
		secondKeychain.failStores = true;
		initializeController();

		runOnFxThread(() -> controller.keychainBackendChoiceBox.setValue(secondKeychain));
		runQueuedTasks();

		Assertions.assertEquals(FirstKeychain.class.getName(), settings.keychainProvider.get());
		Assertions.assertSame(firstKeychain, controller.keychainBackendChoiceBox.getValue());
		Assertions.assertFalse(controller.keychainBackendChoiceBox.isDisabled());
		Assertions.assertTrue(queuedTasks.isEmpty());
		Mockito.verify(appWindows).showErrorWindow(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
	}

	private void initializeController() throws InterruptedException {
		runOnFxThread(() -> {
			controller.useKeychainCheckbox = new CheckBox();
			controller.keychainBackendChoiceBox = new ChoiceBox<>();
			controller.useQuickAccessCheckbox = new CheckBox();
			controller.quickAccessServiceChoiceBox = new ChoiceBox<>();
			controller.startHiddenCheckbox = new CheckBox();
			controller.autoCloseVaultsCheckbox = new CheckBox();
			controller.debugModeCheckbox = new CheckBox();
			controller.autoStartCheckbox = new CheckBox();
			controller.nodeOrientation = new ToggleGroup();
			controller.initialize();
		});
	}

	private void runQueuedTasks() throws InterruptedException {
		while (!queuedTasks.isEmpty()) {
			queuedTasks.poll().run();
		}
		runOnFxThread(() -> {}); // runs after the completion callbacks already queued on the FX thread
	}

	private static void runOnFxThread(Runnable runnable) throws InterruptedException {
		var failure = new AtomicReference<Throwable>();
		var latch = new CountDownLatch(1);
		Platform.runLater(() -> {
			try {
				runnable.run();
			} catch (Throwable t) {
				failure.set(t);
			} finally {
				latch.countDown();
			}
		});
		Assertions.assertTrue(latch.await(5, TimeUnit.SECONDS));
		if (failure.get() != null) {
			throw new AssertionError(failure.get());
		}
	}

	private static class MapKeychain implements KeychainAccessProvider {

		private final Map<String, String> entries = new HashMap<>();
		boolean failStores;

		@Override
		public void storePassphrase(String key, String displayName, CharSequence passphrase) throws KeychainAccessException {
			if (failStores) {
				throw new KeychainAccessException("store failed");
			}
			entries.put(key, passphrase.toString());
		}

		@Override
		public char[] loadPassphrase(String key) {
			var passphrase = entries.get(key);
			return passphrase == null ? null : passphrase.toCharArray();
		}

		@Override
		public void deletePassphrase(String key) {
			entries.remove(key);
		}

		@Override
		public void changePassphrase(String key, String displayName, CharSequence passphrase) throws KeychainAccessException {
			storePassphrase(key, displayName, passphrase);
		}

		@Override
		public boolean isSupported() {
			return true;
		}

		@Override
		public boolean isLocked() {
			return false;
		}
	}

	private static class FirstKeychain extends MapKeychain {}

	private static class SecondKeychain extends MapKeychain {}

}

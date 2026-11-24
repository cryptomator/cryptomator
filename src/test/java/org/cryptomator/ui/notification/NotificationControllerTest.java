package org.cryptomator.ui.notification;

import org.cryptomator.JavaFXUtil;
import org.cryptomator.common.Constants;
import org.cryptomator.common.Environment;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.SettingsProvider;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.event.FileIsInUseEvent;
import org.cryptomator.cryptofs.event.FileWriteConflictEvent;
import org.cryptomator.event.VaultEvent;
import org.cryptomator.ui.fxapp.FxNotificationManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

class NotificationControllerTest {

	private static final String LOCKED_HINT = "Unlock \"Vault\" for details";

	private final Stage window = Mockito.mock(Stage.class);
	private final FxNotificationManager notificationManager = Mockito.mock(FxNotificationManager.class);
	private final ExecutorService executorService = Mockito.mock(ExecutorService.class);
	private final ObservableList<VaultEvent> events = FXCollections.observableArrayList();
	private final BooleanProperty vaultUnlocked = new SimpleBooleanProperty(true);
	private final Vault vault = Mockito.mock(Vault.class);
	private NotificationController controller;
	private EventHandler<WindowEvent> onShowing;
	private boolean showing;

	@BeforeAll
	public static void startup() throws InterruptedException {
		var isRunning = JavaFXUtil.startPlatform();
		Assumptions.assumeTrue(isRunning);
	}

	@BeforeEach
	@SuppressWarnings("unchecked")
	public void setup() {
		Mockito.when(notificationManager.getEventsRequiringNotification()).thenReturn(events);
		Mockito.when(vault.getDisplayName()).thenReturn("Vault");
		Mockito.when(vault.unlockedProperty()).thenReturn(Bindings.createBooleanBinding(vaultUnlocked::get, vaultUnlocked));
		Mockito.when(vault.isUnlocked()).thenAnswer(_ -> vaultUnlocked.get());
		Mockito.when(window.isShowing()).thenAnswer(_ -> showing);
		Mockito.doAnswer(_ -> showing = false).when(window).close();
		var settings = Settings.create(Mockito.mock(SettingsProvider.class), Mockito.mock(Environment.class));
		var resourceBundle = ResourceBundle.getBundle("i18n.strings", Locale.ROOT);
		controller = new NotificationController(window, notificationManager, executorService, resourceBundle, settings);
		controller.actionButton = Mockito.mock(Button.class);
		controller.initialize();
		ArgumentCaptor<EventHandler<WindowEvent>> onShowingCaptor = ArgumentCaptor.forClass(EventHandler.class);
		Mockito.verify(window).setOnShowing(onShowingCaptor.capture());
		onShowing = onShowingCaptor.getValue();
	}

	private void show() {
		if (!showing) {
			showing = true;
			onShowing.handle(new WindowEvent(window, WindowEvent.WINDOW_SHOWING));
		}
	}

	private VaultEvent fileInUse(String cleartextPath, Runnable ignoreMethod) {
		var event = new FileIsInUseEvent(Instant.now(), cleartextPath, Path.of("/vault/d/AB/CDEF" + cleartextPath + ".c9r"), "alice" + Constants.HUB_USER_DEVICE_SEPARATOR + "laptop", Instant.now(), ignoreMethod);
		return new VaultEvent(vault, event);
	}

	private VaultEvent writeConflict(String cleartextPath) {
		return writeConflict(cleartextPath, "/vault/d/AB/CDEF" + cleartextPath + ".c9r");
	}

	private VaultEvent writeConflict(String cleartextPath, String ciphertextPath) {
		return new VaultEvent(vault, new FileWriteConflictEvent(cleartextPath, Path.of(ciphertextPath)));
	}

	@Test
	public void testReusedWindowSubmitsIgnoreOfNextFileInUseEventOnly() {
		var ignoreA = Mockito.mock(Runnable.class);
		var ignoreB = Mockito.mock(Runnable.class);
		events.add(fileInUse("/a.txt", ignoreA));
		show();
		controller.processSelectedEvent();

		events.add(fileInUse("/b.txt", ignoreB));
		show();

		Assertions.assertEquals("b.txt", controller.getFileName());
		controller.processSelectedEvent();
		Mockito.verify(executorService).submit(ignoreA);
		Mockito.verify(executorService).submit(ignoreB);
		Assertions.assertTrue(events.isEmpty());
	}

	@Test
	public void testReusedWindowDismissesWriteConflictAfterFileInUseEvent() {
		events.add(fileInUse("/a.txt", Mockito.mock(Runnable.class)));
		show();
		controller.processSelectedEvent();
		Mockito.clearInvocations(executorService);

		events.add(writeConflict("/b.txt"));
		show();

		Assertions.assertEquals("Your latest changes may not be saved", controller.getMessage());
		Assertions.assertEquals("b.txt", controller.getFileName());
		Assertions.assertEquals("Dismiss", controller.getActionText());
		controller.processSelectedEvent();
		Mockito.verifyNoInteractions(executorService);
		Assertions.assertTrue(events.isEmpty());
	}

	@Test
	public void testWriteConflictAddedWhileVisibleIsSelected() {
		events.add(fileInUse("/a.txt", Mockito.mock(Runnable.class)));
		show();

		events.add(writeConflict("/b.txt"));

		Assertions.assertEquals("b.txt", controller.getFileName());
		Assertions.assertEquals("2/2", controller.getPaging());
		controller.previousNotification();
		Assertions.assertEquals("a.txt", controller.getFileName());

		events.add(fileInUse("/c.txt", Mockito.mock(Runnable.class)));

		Assertions.assertEquals("a.txt", controller.getFileName());
		Assertions.assertEquals("1/3", controller.getPaging());
	}

	@Test
	public void testBatchBeforeFirstShowSelectsNewestWriteConflict() {
		events.addAll(writeConflict("/a.txt"), writeConflict("/b.txt"), fileInUse("/c.txt", Mockito.mock(Runnable.class)));
		show();

		Assertions.assertEquals("b.txt", controller.getFileName());
		Assertions.assertEquals("2/3", controller.getPaging());
		controller.previousNotification();
		Assertions.assertEquals("a.txt", controller.getFileName());
		controller.nextNotification();
		controller.nextNotification();
		Assertions.assertEquals("c.txt", controller.getFileName());
	}

	@Test
	public void testBatchWhileVisibleSelectsNewestWriteConflict() {
		events.add(fileInUse("/x.txt", Mockito.mock(Runnable.class)));
		show();

		events.addAll(writeConflict("/a.txt"), writeConflict("/b.txt"), fileInUse("/c.txt", Mockito.mock(Runnable.class)));

		Assertions.assertEquals("b.txt", controller.getFileName());
		Assertions.assertEquals("3/4", controller.getPaging());
		controller.previousNotification();
		controller.previousNotification();
		Assertions.assertEquals("x.txt", controller.getFileName());
	}

	@Test
	public void testShowingWithNothingPendingClearsFileName() {
		events.add(writeConflict("/a.txt"));
		show();
		controller.processSelectedEvent();

		show();

		Assertions.assertEquals("", controller.getFileName());
		Assertions.assertEquals("", controller.getEventTime());
	}

	@Test
	public void testPageSwitchAndUnlockRerenderDisarmActionButton() {
		vaultUnlocked.set(false);
		events.addAll(writeConflict("/a.txt"), fileInUse("/b.txt", Mockito.mock(Runnable.class)));
		show();
		Mockito.clearInvocations(controller.actionButton);

		controller.nextNotification();
		Mockito.verify(controller.actionButton).disarm();

		Mockito.clearInvocations(controller.actionButton);
		vaultUnlocked.set(true);
		Assertions.assertEquals("Ignore Use Status", controller.getActionText());
		Mockito.verify(controller.actionButton).disarm();
	}

	@Test
	public void testDismissingLastPageShowsPreviousPage() {
		events.add(fileInUse("/a.txt", Mockito.mock(Runnable.class)));
		show();
		events.add(writeConflict("/b.txt"));

		controller.processSelectedEvent();

		Assertions.assertEquals("a.txt", controller.getFileName());
		Assertions.assertEquals("1/1", controller.getPaging());
	}

	@Test
	public void testWriteConflictWithUnknownCleartextPathShowsEncryptedName() {
		events.add(writeConflict(null, "/vault/d/AB/CDEF/hash123==.c9s/contents.c9r"));
		show();

		Assertions.assertEquals("hash123==.c9s", controller.getFileName());
	}

	@Test
	public void testFirstShowWithLockedVaultMasksWriteConflict() {
		vaultUnlocked.set(false);
		events.add(writeConflict("/a.txt"));
		show();

		Assertions.assertEquals("Your latest changes may not be saved", controller.getMessage());
		Assertions.assertTrue(controller.getDescription().startsWith("This file changed elsewhere"));
		Assertions.assertEquals(LOCKED_HINT, controller.getFileName());
	}

	@Test
	public void testFirstShowWithLockedVaultMasksFileInUseEvent() {
		vaultUnlocked.set(false);
		events.add(fileInUse("/a.txt", Mockito.mock(Runnable.class)));
		show();

		Assertions.assertEquals(LOCKED_HINT, controller.getFileName());
		Assertions.assertEquals("", controller.getDescription());
		Assertions.assertEquals("Dismiss", controller.getActionText());
	}

	@Test
	public void testLockingWhileOpenMasksWriteConflict() {
		events.add(writeConflict("/a.txt"));
		show();

		vaultUnlocked.set(false);

		Assertions.assertEquals("Your latest changes may not be saved", controller.getMessage());
		Assertions.assertEquals(LOCKED_HINT, controller.getFileName());
	}

	@Test
	public void testLockingWhileOpenMasksFileInUseEvent() {
		events.add(fileInUse("/a.txt", Mockito.mock(Runnable.class)));
		show();

		vaultUnlocked.set(false);

		Assertions.assertEquals(LOCKED_HINT, controller.getFileName());
		Assertions.assertEquals("", controller.getDescription());
		Assertions.assertEquals("Dismiss", controller.getActionText());
	}

	@Test
	public void testDismissRenderedPageIgnoresUnlockNotYetRendered() {
		vaultUnlocked.set(false);
		events.add(fileInUse("/a.txt", Mockito.mock(Runnable.class)));
		show();

		Mockito.when(vault.isUnlocked()).thenReturn(true);
		controller.processSelectedEvent();

		Mockito.verifyNoInteractions(executorService);
	}

	@Test
	public void testIgnoreRenderedPageSkipsIgnoreOnLockNotYetRendered() {
		events.add(fileInUse("/a.txt", Mockito.mock(Runnable.class)));
		show();

		Mockito.when(vault.isUnlocked()).thenReturn(false);
		controller.processSelectedEvent();

		Mockito.verifyNoInteractions(executorService);
	}

	@Test
	public void testDismissOnLockedFileInUsePageRemovesOnlyThatPage() {
		vaultUnlocked.set(false);
		var other = writeConflict("/b.txt");
		events.addAll(fileInUse("/a.txt", Mockito.mock(Runnable.class)), other);
		show();
		controller.previousNotification();

		controller.processSelectedEvent();

		Mockito.verifyNoInteractions(executorService);
		Assertions.assertEquals(List.of(other), events);
		Assertions.assertEquals("Your latest changes may not be saved", controller.getMessage());
	}

}

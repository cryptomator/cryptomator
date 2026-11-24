package org.cryptomator.ui.eventview;

import org.cryptomator.JavaFXUtil;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.event.DecryptionFailedEvent;
import org.cryptomator.cryptofs.event.FileWriteConflictEvent;
import org.cryptomator.cryptofs.event.FilesystemEvent;
import org.cryptomator.event.FSEventBucket;
import org.cryptomator.event.FSEventBucketContent;
import org.cryptomator.event.FileSystemEventAggregator;
import org.cryptomator.integrations.mount.Mountpoint;
import org.cryptomator.integrations.revealpath.RevealPathService;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

class EventListCellControllerTest {

	private static final String LOCKED_HINT = "Unlock \"Vault\" for details";
	private static final String MASKED_MESSAGE = "***********";

	private final BooleanProperty vaultUnlocked = new SimpleBooleanProperty(true);
	private final Vault vault = Mockito.mock(Vault.class);
	private final RevealPathService revealService = Mockito.mock(RevealPathService.class);
	private EventListCellController controller;

	@BeforeAll
	public static void startup() throws InterruptedException {
		var isRunning = JavaFXUtil.startPlatform();
		Assumptions.assumeTrue(isRunning);
	}

	@BeforeEach
	public void setup() throws Exception {
		Mockito.when(vault.getDisplayName()).thenReturn("Vault");
		Mockito.when(vault.unlockedProperty()).thenReturn(Bindings.createBooleanBinding(vaultUnlocked::get, vaultUnlocked));
		Mockito.when(vault.isUnlocked()).thenAnswer(_ -> vaultUnlocked.get());
		onFxThread(() -> {
			controller = new EventListCellController(new FileSystemEventAggregator(), revealService, ResourceBundle.getBundle("i18n.strings", Locale.ROOT));
			controller.root = new HBox();
			controller.eventActionsMenu = new ContextMenu();
			controller.eventActionsButton = new Button();
			controller.initialize();
		});
	}

	private static void onFxThread(Runnable action) throws Exception {
		try {
			CompletableFuture.runAsync(action, Platform::runLater).get(5, TimeUnit.SECONDS);
		} catch (ExecutionException e) {
			if (e.getCause() instanceof Error error) {
				throw error;
			}
			throw e;
		}
	}

	private Map.Entry<FSEventBucket, FSEventBucketContent> entry(FilesystemEvent event, Path ciphertextPath) {
		return Map.entry(new FSEventBucket(vault, ciphertextPath, event.getClass()), new FSEventBucketContent(event, 1));
	}

	private Map.Entry<FSEventBucket, FSEventBucketContent> writeConflictEntry(String cleartextPath) {
		var ciphertextPath = Path.of("/vault/d/AB/CDEF/abc123==.c9r");
		return entry(new FileWriteConflictEvent(cleartextPath, ciphertextPath), ciphertextPath);
	}

	private List<String> actionTexts() {
		return controller.eventActionsMenu.getItems().stream().map(MenuItem::getText).toList();
	}

	private MenuItem action(String text) {
		return controller.eventActionsMenu.getItems().stream().filter(item -> item.getText().equals(text)).findFirst().orElseThrow();
	}

	@Test
	public void testRowWithoutEntryIsMasked() throws Exception {
		onFxThread(() -> {
			Assertions.assertEquals(MASKED_MESSAGE, controller.getMessage());
			Assertions.assertEquals(FontAwesome5Icon.LOCK, controller.getIcon());
		});
	}

	@Test
	public void testWriteConflictRowWhileUnlocked() throws Exception {
		onFxThread(() -> {
			controller.setEventEntry(writeConflictEntry("/docs/report.txt"));

			Assertions.assertEquals("Changes not saved", controller.getMessage());
			Assertions.assertEquals("report.txt", controller.getDescription());
			Assertions.assertEquals(FontAwesome5Icon.EXCLAMATION_TRIANGLE, controller.getIcon());
			Assertions.assertEquals(List.of("Dismiss", "Show decrypted file", "Show encrypted file"), actionTexts());
		});
	}

	@Test
	public void testWriteConflictRowActionsRevealDecryptedAndEncryptedFile() throws Exception {
		var mount = Path.of("/mnt/vault").toAbsolutePath();
		Mockito.when(vault.getMountPoint()).thenReturn(Mountpoint.forPath(mount));
		onFxThread(() -> {
			controller.setEventEntry(writeConflictEntry("/docs/report.txt"));
			action("Show decrypted file").fire();
		});
		Mockito.verify(revealService).reveal(mount.resolve("docs").resolve("report.txt"));

		onFxThread(() -> action("Show encrypted file").fire());
		Mockito.verify(revealService).reveal(Path.of("/vault/d/AB/CDEF/abc123==.c9r"));
	}

	@Test
	public void testDecryptionFailedRowOfShortenedNameShowsShortenedNodeName() throws Exception {
		onFxThread(() -> {
			var ciphertextPath = Path.of("/vault/d/AB/CDEF/hash123==.c9s/contents.c9r");
			controller.setEventEntry(entry(new DecryptionFailedEvent(ciphertextPath, new Exception()), ciphertextPath));

			Assertions.assertEquals("hash123==.c9s", controller.getDescription());
		});
	}

	@Test
	public void testWriteConflictRowWithUnknownCleartextPath() throws Exception {
		onFxThread(() -> {
			controller.setEventEntry(writeConflictEntry(null));

			Assertions.assertEquals("abc123==.c9r", controller.getDescription());
			Assertions.assertEquals(List.of("Dismiss", "Show encrypted file"), actionTexts());
		});
	}

	@Test
	public void testWriteConflictRowFirstRenderedWhileLocked() throws Exception {
		vaultUnlocked.set(false);
		onFxThread(() -> {
			controller.setEventEntry(writeConflictEntry("/docs/report.txt"));

			Assertions.assertEquals("Changes not saved", controller.getMessage());
			Assertions.assertEquals(FontAwesome5Icon.EXCLAMATION_TRIANGLE, controller.getIcon());
			Assertions.assertEquals(LOCKED_HINT, controller.getDescription());
		});
	}

	@Test
	public void testWriteConflictRowLockedAfterRender() throws Exception {
		onFxThread(() -> {
			controller.setEventEntry(writeConflictEntry("/docs/report.txt"));
			vaultUnlocked.set(false);

			Assertions.assertEquals("Changes not saved", controller.getMessage());
			Assertions.assertEquals(FontAwesome5Icon.EXCLAMATION_TRIANGLE, controller.getIcon());
			Assertions.assertEquals(LOCKED_HINT, controller.getDescription());
		});
	}

	@Test
	public void testOtherEventRowOnRecycledControllerStaysMaskedWhileLocked() throws Exception {
		vaultUnlocked.set(false);
		onFxThread(() -> {
			controller.setEventEntry(writeConflictEntry("/docs/report.txt"));
			var ciphertextPath = Path.of("/vault/d/AB/CDEF/def456==.c9r");
			controller.setEventEntry(entry(new DecryptionFailedEvent(ciphertextPath, new Exception()), ciphertextPath));

			Assertions.assertEquals(MASKED_MESSAGE, controller.getMessage());
			Assertions.assertEquals(FontAwesome5Icon.LOCK, controller.getIcon());
		});
	}

}

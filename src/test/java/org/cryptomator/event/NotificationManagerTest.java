package org.cryptomator.event;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.event.FileWriteConflictEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class NotificationManagerTest {

	private final Vault vault = Mockito.mock(Vault.class);
	private final NotificationManager notificationManager = new NotificationManager();

	@Test
	public void testFileWriteConflictIsQueued() {
		var event = new FileWriteConflictEvent("/report.txt", Path.of("/vault/d/AB/CDEF/abc123==.c9r"));

		Assertions.assertTrue(notificationManager.offer(vault, event));

		var pending = new ArrayList<VaultEvent>();
		Assertions.assertTrue(notificationManager.appendToAndClear(pending));
		Assertions.assertEquals(List.of(new VaultEvent(vault, event)), pending);
	}

	@Test
	public void testFileWriteConflictOnSamePathIsDebounced() {
		var ciphertextPath = Path.of("/vault/d/AB/CDEF/abc123==.c9r");
		var first = new FileWriteConflictEvent("/report.txt", ciphertextPath);
		var second = new FileWriteConflictEvent("/report.txt", ciphertextPath);

		Assertions.assertTrue(notificationManager.offer(vault, first));
		Assertions.assertFalse(notificationManager.offer(vault, second));

		var pending = new ArrayList<VaultEvent>();
		notificationManager.appendToAndClear(pending);
		Assertions.assertEquals(List.of(new VaultEvent(vault, first)), pending);
	}

}

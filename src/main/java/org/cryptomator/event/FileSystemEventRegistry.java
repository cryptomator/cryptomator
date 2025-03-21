package org.cryptomator.event;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.event.BrokenDirFileEvent;
import org.cryptomator.cryptofs.event.BrokenFileNodeEvent;
import org.cryptomator.cryptofs.event.ConflictResolutionFailedEvent;
import org.cryptomator.cryptofs.event.ConflictResolvedEvent;
import org.cryptomator.cryptofs.event.DecryptionFailedEvent;
import org.cryptomator.cryptofs.event.FilesystemEvent;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

//TODO: Rename to aggregator
@Singleton
public class FileSystemEventRegistry {

	public record Key(Vault vault, Path idPath, Class<? extends FilesystemEvent> c) {};

	public record Value(FilesystemEvent mostRecentEvent, int count) {}

	private final ConcurrentHashMap<Key, Value> map;
	private final AtomicBoolean hasUpdates;

	@Inject
	public FileSystemEventRegistry(ScheduledExecutorService scheduledExecutorService) {
		this.map = new ConcurrentHashMap<>();
		this.hasUpdates = new AtomicBoolean(false);
	}

	/**
	 * Enques the given event to be inserted into the map.
	 *
	 * @param v Vault where the event occurred
	 * @param e Actual {@link FilesystemEvent}
	 */
	public void enqueue(Vault v, FilesystemEvent e) {
		var key = computeKey(v, e);
		hasUpdates.set(true);
		map.compute(key, (k, val) -> {
			if (val == null) {
				return new Value(e, 1);
			} else {
				return new Value(e, val.count() + 1);
			}
		});

	}

	/**
	 * Removes an event from the map.
	 * <p>
	 * To identify the event, a similar event (in the sense of map key) is given.
	 *
	 * @return the removed {@link Value}
	 * @implNote Method is not synchronized, because it is only executed if executed by JavaFX application thread
	 */
	public Value remove(Key key) {
		hasUpdates.set(true);
		return map.remove(key);
	}

	/**
	 * Clears the event map.
	 * <p>
	 * Must be executed on the JavaFX application thread
	 *
	 * @implNote Method is not synchronized, because it is only executed if executed by JavaFX application thread
	 */
	public void clear() {
		hasUpdates.set(true);
		map.clear();
	}


	public boolean hasUpdates() {
		return hasUpdates.get();
	}

	/**
	 * Clones the map entries into a collection.
	 * <p>
	 * The collection is first cleared, then all map entries are added in one bulk operation. Cleans the hasUpdates status.
	 *
	 * @param targetCollection
	 */
	public void cloneTo(Collection<Map.Entry<Key, Value>> targetCollection) {
		hasUpdates.set(false);
		targetCollection.clear();
		targetCollection.addAll(map.entrySet());
	}

	/**
	 * Method to compute the identifying key for a given filesystem event
	 *
	 * @param v Vault where the event occurred
	 * @param event Actual {@link FilesystemEvent}
	 * @return a {@link Key} used in the map and lru cache
	 */
	private static Key computeKey(Vault v, FilesystemEvent event) {
		var p = switch (event) {
			case DecryptionFailedEvent(_, Path ciphertextPath, _) -> ciphertextPath;
			case ConflictResolvedEvent(_, _, _, _, Path resolvedCiphertext) -> resolvedCiphertext;
			case ConflictResolutionFailedEvent(_, _, Path conflictingCiphertext, _) -> conflictingCiphertext;
			case BrokenDirFileEvent(_, Path ciphertext) -> ciphertext;
			case BrokenFileNodeEvent(_, _, Path ciphertext) -> ciphertext;
		};
		return new Key(v, p, event.getClass());
	}
}

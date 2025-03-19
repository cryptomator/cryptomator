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
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
public class VaultEventsMap {

	private static final int MAX_MAP_SIZE = 400;

	public record Key(Vault vault, Path idPath, Class<? extends FilesystemEvent> c) {}

	public record Value(FilesystemEvent mostRecentEvent, int count) {}

	/**
	 * Queue of elements to be inserted into the map
	 */
	private final ConcurrentMap<Key, Value> queue;
	/**
	 * Least-recently-used cache.
	 */
	private final TreeSet<Key> lruCache;
	/**
	 * Actual map
	 */
	private final ObservableMap<Key, Value> map;

	private final ScheduledExecutorService scheduler;

	private final AtomicBoolean queueHasElements;

	@Inject
	public VaultEventsMap(ScheduledExecutorService scheduledExecutorService) {
		this.queue = new ConcurrentHashMap<>();
		this.lruCache = new TreeSet<>(this::compareKeys);
		this.map = FXCollections.observableHashMap();
		this.scheduler = scheduledExecutorService;
		this.queueHasElements = new AtomicBoolean(false);
		scheduler.scheduleWithFixedDelay(() -> {
			if (queueHasElements.get()) {
				flush();
			}
		}, 1000, 1000, TimeUnit.MILLISECONDS);
	}

	/**
	 * Enques the given event to be inserted into the map.
	 *
	 * @param v Vault where the event occurred
	 * @param e Actual {@link FilesystemEvent}
	 */
	public synchronized void enque(Vault v, FilesystemEvent e) {
		var key = computeKey(v, e);
		queue.compute(key, (k, val) -> {
			if (val == null) {
				return new Value(e, 1);
			} else {
				return new Value(e, val.count() + 1);
			}
		});

		queueHasElements.set(true);
	}


	/**
	 * Lists all entries in this map as {@link VaultEvent}. The list is sorted ascending by the timestamp of event occurral (and more if it is the same timestamp).
	 * Must be executed on the JavaFX application thread
	 *
	 * @return a list of vault events, mainly sorted ascending by the event timestamp
	 */
	public List<VaultEvent> listAll() {
		if (!Platform.isFxApplicationThread()) {
			throw new IllegalStateException("Listing map entries must be performed on JavaFX application thread");
		}
		return lruCache.stream().map(key -> {
			var value = map.get(key);
			return new VaultEvent(key.vault(), value.mostRecentEvent(), value.count());
		}).toList();
	}

	/**
	 * Removes an event from the map.
	 * <p>
	 * To identify the event, a similar event (in the sense of map key) is given.
	 * Must be executed on the JavaFX application thread
	 *
	 * @param v Vault where the event occurred
	 * @param similar A similar {@link FilesystemEvent} (same class, same idPath)
	 * @return the removed {@link Value}
	 */
	public Value remove(Vault v, FilesystemEvent similar) {
		if (!Platform.isFxApplicationThread()) {
			throw new IllegalStateException("Map removal must be performed on JavaFX application thread");
		}
		var key = computeKey(v, similar);
		lruCache.remove(key);
		return map.remove(key);
	}

	public void clear() {
		if (!Platform.isFxApplicationThread()) {
			throw new IllegalStateException("Map removal must be performed on JavaFX application thread");
		}
		lruCache.clear();
		map.clear();
	}

	/**
	 * Flushes all changes from the queue into the map
	 */
	private synchronized void flush() {
		//Lock queue
		var latch = new CountDownLatch(1);
		Platform.runLater(() -> {
			queue.forEach(this::updateMap);
			queue.clear();
			latch.countDown();
		});
		try {
			latch.await();
			queueHasElements.set(false);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Updates a single map entry
	 *
	 * @param k Key of the entry to update
	 * @param v Value of the entry to update
	 */
	private void updateMap(Key k, Value v) {
		var entry = map.get(k);
		if (entry == null) {
			if (map.size() == MAX_MAP_SIZE) {
				var toRemove = lruCache.first();
				lruCache.remove(toRemove);
				map.remove(toRemove);
			}
			map.put(k, v);
			lruCache.add(k);
		} else {
			lruCache.remove(k);
			map.put(k, new Value(v.mostRecentEvent, entry.count + v.count));
			lruCache.add(k); //correct, because cache-sorting uses the map in comparsionMethod
		}
	}

	/* Observability */

	public void addListener(MapChangeListener<? super Key, ? super Value> mapChangeListener) {
		map.addListener(mapChangeListener);
	}

	public void removeListener(MapChangeListener<? super Key, ? super Value> mapChangeListener) {
		map.removeListener(mapChangeListener);
	}


	/* Internal stuff */

	/**
	 * Comparsion method for the lru cache. During comparsion the map is accessed.
	 * First the entries are compared by the event timestamp, then vaultId, then identifying path and lastly by class name.
	 *
	 * @param left a {@link Key} object
	 * @param right another {@link Key} object, compared to {@code left}
	 * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
	 */
	private int compareKeys(Key left, Key right) {
		var t1 = map.get(left).mostRecentEvent.getTimestamp();
		var t2 = map.get(right).mostRecentEvent.getTimestamp();
		var timeComparsion = t1.compareTo(t2);
		if (timeComparsion != 0) {
			return timeComparsion;
		}
		var vaultIdComparsion = left.vault.getId().compareTo(right.vault.getId());
		if (vaultIdComparsion != 0) {
			return vaultIdComparsion;
		}
		var pathComparsion = left.idPath.compareTo(right.idPath);
		if (pathComparsion != 0) {
			return pathComparsion;
		}
		return left.c.getName().compareTo(right.c.getName());
	}

	/**
	 * Method to compute the identifying key for a given filesystem event
	 *
	 * @param v Vault where the event occurred
	 * @param event Actual {@link FilesystemEvent}
	 * @return a {@link Key} used in the map and lru cache
	 */
	private Key computeKey(Vault v, FilesystemEvent event) {
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

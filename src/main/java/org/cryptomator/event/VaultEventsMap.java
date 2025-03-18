package org.cryptomator.event;

import org.cryptomator.common.ObservableMapDecorator;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.event.BrokenDirFileEvent;
import org.cryptomator.cryptofs.event.BrokenFileNodeEvent;
import org.cryptomator.cryptofs.event.ConflictResolutionFailedEvent;
import org.cryptomator.cryptofs.event.ConflictResolvedEvent;
import org.cryptomator.cryptofs.event.DecryptionFailedEvent;
import org.cryptomator.cryptofs.event.FilesystemEvent;

import javax.inject.Inject;
import javax.inject.Singleton;
import javafx.collections.FXCollections;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeSet;

/**
 * Map containing {@link VaultEvent}s.
 * The map is keyed by three elements:
 * <ul>
 *     <li>the vault where the event occurred</li>
 *     <li>an identifying path (can be cleartext or ciphertext)</li>
 *     <li>the class name of the occurred event</li>
 * </ul>
 *
 * <p>
 * Use {@link VaultEventsMap#put(Vault, FilesystemEvent)} to add an element and {@link VaultEventsMap#remove(Vault, FilesystemEvent)} to remove it.
 * <p>
 * The map is size restricted to {@value MAX_SIZE} elements. If a _new_ element (i.e. not already present) is added, the least recently added is removed.
 */
@Singleton
public class VaultEventsMap extends ObservableMapDecorator<VaultEventsMap.Key, VaultEventsMap.Value> {

	private static final int MAX_SIZE = 300;

	public record Key(Vault vault, Path idPath, Class<? extends FilesystemEvent> c) {}

	public record Value(FilesystemEvent mostRecentEvent, int count) {}

	/**
	 * Internal least-recently-used cache.
	 */
	private final TreeSet<Key> lruCache;

	@Inject
	public VaultEventsMap() {
		super(FXCollections.observableHashMap());
		this.lruCache = new TreeSet<>(this::compareToMapKeys);
	}


	/**
	 * Comparsion method for the lru cache. During comparsion the map is accessed.
	 * First the entries are compared by the event timestamp, then vaultId, then identifying path and lastly by class name.
	 *
	 * @param left a {@link Key} object
	 * @param right another {@link Key} object, compared to {@code left}
	 * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
	 */
	private int compareToMapKeys(Key left, Key right) {
		var t1 = delegate.get(left).mostRecentEvent.getTimestamp();
		var t2 = delegate.get(right).mostRecentEvent.getTimestamp();
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
	 * Lists all entries in this map as {@link VaultEvent}. The list is sorted ascending by the timestamp of event occurral (and more if it is the same timestamp).
	 * This method is synchronized with {@link VaultEventsMap#put(Vault, FilesystemEvent)} and {@link VaultEventsMap#remove(Vault, FilesystemEvent)}.
	 *
	 * @return a list of vault events, mainly sorted ascending by the event timestamp
	 */
	public synchronized List<VaultEvent> listAll() {
		return lruCache.stream().map(key -> {
			var value = delegate.get(key);
			return new VaultEvent(key.vault(), value.mostRecentEvent(), value.count());
		}).toList();
	}

	public synchronized void put(Vault v, FilesystemEvent e) {
		var key = computeKey(v, e);
		//if-else
		var entry = delegate.get(key);
		if (entry == null) {
			if (size() == MAX_SIZE) {
				var toRemove = lruCache.first();
				lruCache.remove(toRemove);
				delegate.remove(toRemove);
			}
			delegate.put(key, new Value(e, 1));
			lruCache.add(key);
		} else {
			lruCache.remove(key);
			delegate.put(key, new Value(e, entry.count() + 1));
			lruCache.add(key); //correct, because cache-sorting uses the map in comparsionMethod
		}
	}

	public synchronized Value remove(Vault v, FilesystemEvent similar) {
		var key = computeKey(v, similar);
		lruCache.remove(key);
		return delegate.remove(key);
	}

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

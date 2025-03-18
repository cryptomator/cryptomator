package org.cryptomator.common;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.event.BrokenDirFileEvent;
import org.cryptomator.cryptofs.event.BrokenFileNodeEvent;
import org.cryptomator.cryptofs.event.ConflictResolutionFailedEvent;
import org.cryptomator.cryptofs.event.ConflictResolvedEvent;
import org.cryptomator.cryptofs.event.DecryptionFailedEvent;
import org.cryptomator.cryptofs.event.FilesystemEvent;
import org.cryptomator.event.VaultEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

/**
 * Map containing {@link VaultEvent}s.
 * The map is keyed by the ciphertext path of the affected resource _and_ the {@link FilesystemEvent}s class in order to group same events
 * <p>
 * Use {@link VaultEventsMap#put(VaultEvent)} to add an element and {@link VaultEventsMap#remove(VaultEvent)} to remove it.
 * <p>
 * The map is size restricted to {@value MAX_SIZE} elements. If a _new_ element (i.e. not already present) is added, the least recently added is removed.
 */
@Singleton
public class VaultEventsMap implements ObservableMap<VaultEventsMap.Key, VaultEvent> {

	private static final int MAX_SIZE = 300;

	public record Key(Vault v, Path key, Class<? extends FilesystemEvent> c) {}
	public record Value(FilesystemEvent event, int count) {}

	private final ObservableMap<Key, VaultEvent> delegate;

	@Inject
	public VaultEventsMap() {
		delegate = FXCollections.observableHashMap();
	}

	@Override
	public void addListener(MapChangeListener<? super Key, ? super VaultEvent> mapChangeListener) {
		delegate.addListener(mapChangeListener);
	}

	@Override
	public void removeListener(MapChangeListener<? super Key, ? super VaultEvent> mapChangeListener) {
		delegate.removeListener(mapChangeListener);
	}

	@Override
	public int size() {
		return delegate.size();
	}

	@Override
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return delegate.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return delegate.containsValue(value);
	}

	@Override
	public VaultEvent get(Object key) {
		return delegate.get(key);
	}

	@Override
	public @Nullable VaultEvent put(Key key, VaultEvent value) {
		return delegate.put(key, value);
	}

	@Override
	public VaultEvent remove(Object key) {
		return delegate.remove(key);
	}

	@Override
	public void putAll(@NotNull Map<? extends Key, ? extends VaultEvent> m) {
		delegate.putAll(m);
	}

	@Override
	public void clear() {
		delegate.clear();
	}

	@Override
	public @NotNull Set<Key> keySet() {
		return delegate.keySet();
	}

	@Override
	public @NotNull Collection<VaultEvent> values() {
		return delegate.values();
	}

	@Override
	public @NotNull Set<Entry<Key, VaultEvent>> entrySet() {
		return delegate.entrySet();
	}

	@Override
	public void addListener(InvalidationListener invalidationListener) {
		delegate.addListener(invalidationListener);
	}

	@Override
	public void removeListener(InvalidationListener invalidationListener) {
		delegate.removeListener(invalidationListener);
	}

	public synchronized void put(VaultEvent e) {
		//compute key
		var key = computeKey(e);
		//if-else
		var nullOrEntry = delegate.get(key);
		if (nullOrEntry == null) {
			if (size() == MAX_SIZE) {
				delegate.entrySet().stream() //
						.min(Comparator.comparing(entry -> entry.getValue().actualEvent().getTimestamp())) //
						.ifPresent(oldestEntry -> delegate.remove(oldestEntry.getKey()));
			}
			delegate.put(key, e);
		} else {
			delegate.put(key, nullOrEntry.incrementCount(e.actualEvent()));
		}
	}

	public synchronized VaultEvent remove(VaultEvent similar) {
		//compute key
		var key = computeKey(similar);
		return this.remove(key);
	}

	private Key computeKey(VaultEvent ve) {
		var e = ve.actualEvent();
		var p = switch (e) {
			case DecryptionFailedEvent(_, Path ciphertextPath, _) -> ciphertextPath;
			case ConflictResolvedEvent(_, _, _, _, Path resolvedCiphertext) -> resolvedCiphertext;
			case ConflictResolutionFailedEvent(_, _, Path conflictingCiphertext, _) -> conflictingCiphertext;
			case BrokenDirFileEvent(_, Path ciphertext) -> ciphertext;
			case BrokenFileNodeEvent(_, _, Path ciphertext) -> ciphertext;
		};
		return new Key(ve.v(), p, e.getClass());
	}
}

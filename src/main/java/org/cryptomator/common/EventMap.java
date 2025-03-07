package org.cryptomator.common;

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
import java.util.Map;
import java.util.Set;

/**
 * Map containing {@link VaultEvent}s.
 * The map is keyed by the ciphertext path of the affected resource _and_ the {@link FilesystemEvent}s class in order to group same events
 */
@Singleton
public class EventMap implements ObservableMap<EventMap.EventKey, VaultEvent> {

	public record EventKey(Path ciphertextPath, Class<? extends FilesystemEvent> c) {}

	private final ObservableMap<EventMap.EventKey, VaultEvent> delegate;

	@Inject
	public EventMap() {
		delegate = FXCollections.observableHashMap();
	}

	@Override
	public void addListener(MapChangeListener<? super EventKey, ? super VaultEvent> mapChangeListener) {
		delegate.addListener(mapChangeListener);
	}

	@Override
	public void removeListener(MapChangeListener<? super EventKey, ? super VaultEvent> mapChangeListener) {
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
	public @Nullable VaultEvent put(EventKey key, VaultEvent value) {
		return delegate.put(key, value);
	}

	@Override
	public VaultEvent remove(Object key) {
		return delegate.remove(key);
	}

	@Override
	public void putAll(@NotNull Map<? extends EventKey, ? extends VaultEvent> m) {
		delegate.putAll(m);
	}

	@Override
	public void clear() {
		delegate.clear();
	}

	@Override
	public @NotNull Set<EventKey> keySet() {
		return delegate.keySet();
	}

	@Override
	public @NotNull Collection<VaultEvent> values() {
		return delegate.values();
	}

	@Override
	public @NotNull Set<Entry<EventKey, VaultEvent>> entrySet() {
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
		var key = computeKey(e.actualEvent());
		//if-else
		var nullOrEntry = delegate.get(key);
		if (nullOrEntry == null) {
			delegate.put(key, e);
		} else {
			delegate.put(key, nullOrEntry.incrementCount(e.timestamp()));
		}
	}

	public synchronized VaultEvent remove(VaultEvent similar) {
		//compute key
		var key = computeKey(similar.actualEvent());
		return this.remove(key);
	}

	private EventKey computeKey(FilesystemEvent e) {
		var p = switch (e) {
			case DecryptionFailedEvent(Path ciphertextPath, _) -> ciphertextPath;
			case ConflictResolvedEvent(_, _, _, Path resolvedCiphertext) -> resolvedCiphertext;
			case ConflictResolutionFailedEvent(_, Path conflictingCiphertext, _) -> conflictingCiphertext;
			case BrokenDirFileEvent(Path ciphertext) -> ciphertext;
			case BrokenFileNodeEvent(_, Path ciphertext) -> ciphertext;
		};
		return new EventKey(p, e.getClass());
	}
}

package org.cryptomator.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javafx.beans.InvalidationListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public abstract class ObservableMapDecorator<K, V> implements ObservableMap<K, V> {

	protected final ObservableMap<K, V> delegate;

	protected ObservableMapDecorator(ObservableMap<K, V> delegate) {
		this.delegate = delegate;
	}

	@Override
	public void addListener(MapChangeListener<? super K, ? super V> mapChangeListener) {
		delegate.addListener(mapChangeListener);
	}

	@Override
	public void removeListener(MapChangeListener<? super K, ? super V> mapChangeListener) {
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
	public V get(Object key) {
		return delegate.get(key);
	}

	@Override
	public @Nullable V put(K key, V value) {
		return delegate.put(key, value);
	}

	@Override
	public V remove(Object key) {
		return delegate.remove(key);
	}

	@Override
	public void putAll(@NotNull Map<? extends K, ? extends V> m) {
		delegate.putAll(m);
	}

	@Override
	public void clear() {
		delegate.clear();
	}

	@Override
	public @NotNull Set<K> keySet() {
		return delegate.keySet();
	}

	@Override
	public @NotNull Collection<V> values() {
		return delegate.values();
	}

	@Override
	public @NotNull Set<Entry<K, V>> entrySet() {
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

}

package org.cryptomator.filesystem.nio;

import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

class WeakValuedCache<Key, Value> {

	private final LoadingCache<Key, Value> delegate;

	private WeakValuedCache(Function<Key, Value> loader) {
		delegate = CacheBuilder.newBuilder() //
				.weakValues() //
				.build(new CacheLoader<Key, Value>() {
					@Override
					public Value load(Key key) {
						return loader.apply(key);
					}
				});
	}

	public static <Key, Value> WeakValuedCache<Key, Value> usingLoader(Function<Key, Value> loader) {
		return new WeakValuedCache<>(loader);
	}

	public Value get(Key key) {
		try {
			return delegate.get(key);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

}

package org.cryptomator.webdav.jackrabbit;

import java.util.Map;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.AbstractDualBidiMap;
import org.apache.commons.collections4.map.LRUMap;

final class BidiLRUMap<K, V> extends AbstractDualBidiMap<K, V> {

	BidiLRUMap(int maxSize) {
		super(new LRUMap<K, V>(maxSize), new LRUMap<V, K>(maxSize));
	}

	protected BidiLRUMap(final Map<K, V> normalMap, final Map<V, K> reverseMap, final BidiMap<V, K> inverseBidiMap) {
		super(normalMap, reverseMap, inverseBidiMap);
	}

	@Override
	protected BidiMap<V, K> createBidiMap(Map<V, K> normalMap, Map<K, V> reverseMap, BidiMap<K, V> inverseMap) {
		return new BidiLRUMap<V, K>(normalMap, reverseMap, inverseMap);
	}

}

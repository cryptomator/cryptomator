package org.cryptomator.crypto;

import java.util.Map;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.AbstractDualBidiMap;
import org.apache.commons.collections4.map.LRUMap;
import org.cryptomator.crypto.exceptions.DecryptFailedException;

public class PathCachingCryptorDecorator extends AbstractCryptorDecorator {

	private static final int MAX_CACHED_PATHS = 5000;
	private static final int MAX_CACHED_NAMES = 5000;

	private final Map<String, String> pathCache = new LRUMap<>(MAX_CACHED_PATHS); // <cleartextDirectoryId, ciphertextPath>
	private final BidiMap<String, String> nameCache = new BidiLRUMap<>(MAX_CACHED_NAMES); // <cleartextName, ciphertextName>

	private PathCachingCryptorDecorator(Cryptor cryptor) {
		super(cryptor);
	}

	public static Cryptor decorate(Cryptor cryptor) {
		return new PathCachingCryptorDecorator(cryptor);
	}

	/* Cryptor */

	@Override
	public String encryptDirectoryPath(String cleartextDirectoryId, String nativePathSep) {
		return pathCache.computeIfAbsent(cleartextDirectoryId, id -> cryptor.encryptDirectoryPath(id, nativePathSep));
	}

	@Override
	public String encryptFilename(String cleartextName) {
		return nameCache.computeIfAbsent(cleartextName, name -> cryptor.encryptFilename(name));
	}

	@Override
	public String decryptFilename(String ciphertextName) throws DecryptFailedException {
		String cleartextName = nameCache.getKey(ciphertextName);
		if (cleartextName == null) {
			cleartextName = cryptor.decryptFilename(ciphertextName);
			nameCache.put(cleartextName, ciphertextName);
		}
		return cleartextName;
	}

	private static class BidiLRUMap<K, V> extends AbstractDualBidiMap<K, V> {

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

}

package org.cryptomator.crypto;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.AbstractDualBidiMap;
import org.apache.commons.collections4.map.LRUMap;
import org.cryptomator.crypto.exceptions.DecryptFailedException;

public class PathCachingCryptorDecorator extends AbstractCryptorDecorator {

	private static final int MAX_CACHED_PATHS = 5000;
	private static final int MAX_CACHED_NAMES = 5000;

	private final Map<String, String> pathCache = new LRUMap<>(MAX_CACHED_PATHS); // <cleartextPath, ciphertextPath>
	private final BidiMap<String, String> nameCache = new BidiLRUMap<>(MAX_CACHED_NAMES); // <cleartextName, ciphertextName>

	private PathCachingCryptorDecorator(Cryptor cryptor) {
		super(cryptor);
	}

	public static Cryptor decorate(Cryptor cryptor) {
		return new PathCachingCryptorDecorator(cryptor);
	}

	/* Cryptor */

	@Override
	public String encryptDirectoryPath(String cleartextPath, String nativePathSep) {
		if (pathCache.containsKey(cleartextPath)) {
			return pathCache.get(cleartextPath);
		} else {
			final String ciphertextPath = cryptor.encryptDirectoryPath(cleartextPath, nativePathSep);
			pathCache.put(cleartextPath, ciphertextPath);
			return ciphertextPath;
		}
	}

	@Override
	public String encryptFilename(String cleartextName, CryptorMetadataSupport ioSupport) throws IOException {
		if (nameCache.containsKey(cleartextName)) {
			return nameCache.get(cleartextName);
		} else {
			final String ciphertextName = cryptor.encryptFilename(cleartextName, ioSupport);
			nameCache.put(cleartextName, ciphertextName);
			return ciphertextName;
		}
	}

	@Override
	public String decryptFilename(String ciphertextName, CryptorMetadataSupport ioSupport) throws IOException, DecryptFailedException {
		if (nameCache.containsValue(ciphertextName)) {
			return nameCache.getKey(ciphertextName);
		} else {
			final String cleartextName = cryptor.decryptFilename(ciphertextName, ioSupport);
			nameCache.put(cleartextName, ciphertextName);
			return ciphertextName;
		}
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

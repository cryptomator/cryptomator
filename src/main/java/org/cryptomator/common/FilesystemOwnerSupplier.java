package org.cryptomator.common;

import java.util.function.Supplier;

/**
 * Interface marking a class to be used in {@link org.cryptomator.cryptofs.CryptoFileSystemProperties.Builder#withOwnerGetter(Supplier)}.
 */
@FunctionalInterface
public interface FilesystemOwnerSupplier {

	/**
	 * Get the filesystem owner.
	 *
	 * @return the filesystem owner
	 */
	String getOwner();

}

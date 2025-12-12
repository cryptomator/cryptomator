package org.cryptomator.common;

/**
 * Objects which has some kind of owner.
 */
@FunctionalInterface
public interface FilesystemOwnerSupplier {

	/**
	 * Get the object owner.
	 *
	 * @return the object owner
	 */
	String getOwner();

}

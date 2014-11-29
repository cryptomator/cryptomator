package org.cryptomator.crypto;

public interface SensitiveDataSwipeListener {

	/**
	 * Removes sensitive data from memory. Depending on the data (e.g. for passwords) it might be necessary to overwrite the memory before
	 * freeing the object.
	 */
	void swipeSensitiveData();

}

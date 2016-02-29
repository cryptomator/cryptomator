/*******************************************************************************
 * Copyright (c) 2014, 2016 cryptomator.org
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Tillmann Gaida - initial implementation
 ******************************************************************************/
package org.cryptomator.ui.util;

import java.util.Optional;

/**
 * Wrapper around an object, which should be closed later - explicitly or by a
 * {@link DeferredCloser}. The wrapped object can be accessed as long as the
 * resource has not been closed.
 * 
 * @author Tillmann Gaida
 *
 * @param <T>
 *            any type
 */
public interface DeferredClosable<T> extends AutoCloseable {
	/**
	 * Returns the wrapped Object.
	 * 
	 * @return empty if the object has been closed.
	 */
	public Optional<T> get();

	/**
	 * Quietly closes the Object. If the object was closed before, nothing
	 * happens.
	 */
	public void close();

	/**
	 * @return an empty object.
	 */
	public static <T> DeferredClosable<T> empty() {
		return DeferredCloser.empty();
	}
}
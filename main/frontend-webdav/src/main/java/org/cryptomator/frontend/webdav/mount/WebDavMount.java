/*******************************************************************************
 * Copyright (c) 2014, 2016 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Markus Kreusch - Refactored WebDavMounter to use strategy pattern
 ******************************************************************************/
package org.cryptomator.frontend.webdav.mount;

import org.cryptomator.frontend.CommandFailedException;

/**
 * A mounted webdav share.
 * 
 * @author Markus Kreusch
 */
public interface WebDavMount extends AutoCloseable {

	/**
	 * Unmounts this {@code WebDavMount}.
	 * 
	 * @throws CommandFailedException if the unmount operation fails
	 */
	void unmount() throws CommandFailedException;

	/**
	 * Reveals the mounted drive in the operating systems default file browser.
	 * 
	 * @throws CommandFailedException if the reveal operation fails
	 */
	void reveal() throws CommandFailedException;

}

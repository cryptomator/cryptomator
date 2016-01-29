/*******************************************************************************
 * Copyright (c) 2014, 2016 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Markus Kreusch - Refactored WebDavMounter to use strategy pattern
 *     Sebastian Stenzel - minor strategy fine tuning
 ******************************************************************************/
package org.cryptomator.frontend.webdav.mount;

/**
 * A strategy able to mount a webdav share and display it to the user.
 * 
 * @author Markus Kreusch
 */
interface WebDavMounterStrategy extends WebDavMounter {

	/**
	 * @return {@code false} if this {@code WebDavMounterStrategy} can not work on the local machine, {@code true} if it could work
	 */
	boolean shouldWork();

	/**
	 * Invoked when mounting strategy gets chosen. On some operating systems (we don't want to tell names here) mounting might be faster,
	 * when certain things are prepared before the actual mount attempt.
	 */
	void warmUp(int serverPort);

}

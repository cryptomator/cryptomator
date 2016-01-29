/*******************************************************************************
 * Copyright (c) 2014, 2016 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *      Markus Kreusch - Refactored WebDavMounter to use strategy pattern
 ******************************************************************************/
package org.cryptomator.frontend.webdav.mount;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import org.cryptomator.frontend.CommandFailedException;
import org.cryptomator.frontend.Frontend.MountParam;

/**
 * A WebDavMounter acting as fallback if no other mounter works.
 *
 * @author Markus Kreusch
 */
final class FallbackWebDavMounter implements WebDavMounterStrategy {

	@Override
	public boolean shouldWork() {
		return true;
	}

	@Override
	public void warmUp(int serverPort) {
		// no-op
	}

	@Override
	public WebDavMount mount(URI uri, Map<MountParam, Optional<String>> mountParams) {
		displayMountInstructions();
		return new AbstractWebDavMount() {
			@Override
			public void unmount() {
				displayUnmountInstructions();
			}

			@Override
			public void reveal() throws CommandFailedException {
				displayRevealInstructions();
			}
		};
	}

	private void displayMountInstructions() {
		// TODO display message to user pointing to cryptomator.org/mounting#mount which describes what to do
		// Machine-readable mount instructions: http://tools.ietf.org/html/rfc4709#page-5 :-)
	}

	private void displayUnmountInstructions() {
		// TODO display message to user pointing to cryptomator.org/mounting#unmount which describes what to do
	}

	private void displayRevealInstructions() {
		// TODO display message to user pointing to cryptomator.org/mounting#reveal which describes what to do
	}

}

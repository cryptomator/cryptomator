/*******************************************************************************
 * Copyright (c) 2014 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *      Markus Kreusch - Refactored WebDavMounter to use strategy pattern
 ******************************************************************************/
package org.cryptomator.ui.util.mount;

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
	public WebDavMount mount(int localPort) {
		displayMountInstructions();
		return new WebDavMount() {
			@Override
			public void unmount() {
				displayUnmountInstructions();
			}
		};
	}

	private void displayMountInstructions() {
		// TODO display message to user pointing to cryptomator.org/mounting#mount which describes what to do
	}

	private void displayUnmountInstructions() {
		// TODO display message to user pointing to cryptomator.org/mounting#unmount which describes what to do
	}

}

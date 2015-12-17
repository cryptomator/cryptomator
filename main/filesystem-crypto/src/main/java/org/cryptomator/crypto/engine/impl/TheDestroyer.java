/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.engine.impl;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;

final class TheDestroyer {

	private TheDestroyer() {

	}

	public static void destroyQuietly(Destroyable d) {
		try {
			d.destroy();
		} catch (DestroyFailedException e) {
			// ignore
		}
	}

}

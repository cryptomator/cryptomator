/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.engine;

import javax.security.auth.Destroyable;

/**
 * A Cryptor instance, once initialized with a set of keys, provides access to threadsafe cryptographic routines.
 */
public interface Cryptor extends Destroyable {

	FilenameCryptor getFilenameCryptor();

	FileContentCryptor getFileContentCryptor();

	void randomizeMasterkey();

	boolean readKeysFromMasterkeyFile(byte[] masterkeyFileContents, CharSequence passphrase);

	byte[] writeKeysToMasterkeyFile(CharSequence passphrase);

}

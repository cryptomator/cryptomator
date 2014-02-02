/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package de.sebastianstenzel.oce.crypto;

import de.sebastianstenzel.oce.crypto.aes256.AesCryptor;

public abstract class Cryptor implements FilenamePseudonymizing, StorageCrypting {
	
	private static final Cryptor DEFAULT_CRYPTOR = new AesCryptor();
	
	public static Cryptor getDefaultCryptor() {
		return DEFAULT_CRYPTOR;
	}

}

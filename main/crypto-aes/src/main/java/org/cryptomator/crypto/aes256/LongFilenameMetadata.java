/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.crypto.aes256;

import java.io.Serializable;

class LongFilenameMetadata implements Serializable {

	private static final long serialVersionUID = 6214509403824421320L;
	private String encryptedFilename;

	/* Getter/Setter */

	public String getEncryptedFilename() {
		return encryptedFilename;
	}

	public void setEncryptedFilename(String encryptedFilename) {
		this.encryptedFilename = encryptedFilename;
	}

}

/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.webdav.jackrabbit;

import java.io.Serializable;
import java.util.UUID;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

class LongFilenameMetadata implements Serializable {

	private static final long serialVersionUID = 6214509403824421320L;

	@JsonDeserialize(as = DualHashBidiMap.class)
	private BidiMap<UUID, String> encryptedFilenames = new DualHashBidiMap<>();

	/* Getter/Setter */

	public synchronized String getEncryptedFilenameForUUID(final UUID uuid) {
		return encryptedFilenames.get(uuid);
	}

	public synchronized UUID getOrCreateUuidForEncryptedFilename(String encryptedFilename) {
		UUID uuid = encryptedFilenames.getKey(encryptedFilename);
		if (uuid == null) {
			uuid = UUID.randomUUID();
			encryptedFilenames.put(uuid, encryptedFilename);
		}
		return uuid;
	}

	public BidiMap<UUID, String> getEncryptedFilenames() {
		return encryptedFilenames;
	}

	public void setEncryptedFilenames(BidiMap<UUID, String> encryptedFilenames) {
		this.encryptedFilenames = encryptedFilenames;
	}

}

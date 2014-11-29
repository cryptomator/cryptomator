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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonPropertyOrder(value = { "iv", "salt", "files" })
class Metadata implements Serializable {
	private static final long serialVersionUID = 6214509403824421320L;
	private byte[] iv;
	private byte[] salt;
	@JsonDeserialize(as = DualHashBidiMap.class)
	private BidiMap<String, byte[]> filenames;
	private Map<String, Long> filesizes;
	
	Metadata() {
		// used by jackson
	}
	
	Metadata(byte[] iv, byte[] salt) {
		this.iv = iv;
		this.salt = salt;
	}

	/* Getter/Setter */
	
	public byte[] getIv() {
		return iv;
	}

	public void setIv(byte[] iv) {
		this.iv = iv;
	}

	public byte[] getSalt() {
		return salt;
	}

	public void setSalt(byte[] salt) {
		this.salt = salt;
	}

	public BidiMap<String, byte[]> getFilenames() {
		if (filenames == null) {
			filenames = new DualHashBidiMap<>();
		}
		return filenames;
	}
	
	public void setFilenames(BidiMap<String, byte[]> filesnames) {
		this.filenames = filesnames;
	}

	public Map<String, Long> getFilesizes() {
		if (filesizes == null) {
			filesizes = new HashMap<>();
		}
		return filesizes;
	}

	public void setFilesizes(Map<String, Long> filesizes) {
		this.filesizes = filesizes;
	}

}

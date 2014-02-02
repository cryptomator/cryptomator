/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package de.sebastianstenzel.oce.crypto.aes256;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonPropertyOrder(value = { "ownerKey", "additionalKeys" })
class Keys implements Serializable {

	private static final long serialVersionUID = -19303594304327167L;
	private Key ownerKey;
	@JsonDeserialize(as = HashMap.class)
	private Map<String, Key> additionalKeys;
	
	public Key getOwnerKey() {
		return ownerKey;
	}
	
	public void setOwnerKey(Key ownerKey) {
		this.ownerKey = ownerKey;
	}
	
	public Map<String, Key> getAdditionalKeys() {
		return additionalKeys;
	}
	
	public void setAdditionalKeys(Map<String, Key> additionalKeys) {
		this.additionalKeys = additionalKeys;
	}
	
	@JsonPropertyOrder(value = { "salt", "iv", "iterations", "keyLength", "masterkey" })
	public static class Key implements Serializable {

		private static final long serialVersionUID = 8578363158959619885L;
		private byte[] salt;
		private byte[] iv;
		private int iterations;
		private int keyLength;
		private byte[] pwVerification;
		private byte[] masterkey;
		
		public byte[] getSalt() {
			return salt;
		}
		
		public void setSalt(byte[] salt) {
			this.salt = salt;
		}

		public byte[] getIv() {
			return iv;
		}

		public void setIv(byte[] iv) {
			this.iv = iv;
		}

		public int getIterations() {
			return iterations;
		}

		public void setIterations(int iterations) {
			this.iterations = iterations;
		}

		public int getKeyLength() {
			return keyLength;
		}

		public void setKeyLength(int keyLength) {
			this.keyLength = keyLength;
		}

		public byte[] getPwVerification() {
			return pwVerification;
		}

		public void setPwVerification(byte[] pwVerification) {
			this.pwVerification = pwVerification;
		}

		public byte[] getMasterkey() {
			return masterkey;
		}

		public void setMasterkey(byte[] masterkey) {
			this.masterkey = masterkey;
		}

		
	}


}

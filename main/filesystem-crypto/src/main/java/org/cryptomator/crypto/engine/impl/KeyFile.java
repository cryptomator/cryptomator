/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.engine.impl;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder(value = {"version", "scryptSalt", "scryptCostParam", "scryptBlockSize", "primaryMasterKey", "hmacMasterKey", "versionMac"})
class KeyFile implements Serializable {

	private static final long serialVersionUID = 8578363158959619885L;

	@JsonProperty("version")
	private Integer version;

	@JsonProperty("scryptSalt")
	private byte[] scryptSalt;

	@JsonProperty("scryptCostParam")
	private int scryptCostParam;

	@JsonProperty("scryptBlockSize")
	private int scryptBlockSize;

	@JsonProperty("primaryMasterKey")
	private byte[] encryptionMasterKey;

	@JsonProperty("hmacMasterKey")
	private byte[] macMasterKey;

	@JsonProperty("versionMac")
	private byte[] versionMac;

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public byte[] getScryptSalt() {
		return scryptSalt;
	}

	public void setScryptSalt(byte[] scryptSalt) {
		this.scryptSalt = scryptSalt;
	}

	public int getScryptCostParam() {
		return scryptCostParam;
	}

	public void setScryptCostParam(int scryptCostParam) {
		this.scryptCostParam = scryptCostParam;
	}

	public int getScryptBlockSize() {
		return scryptBlockSize;
	}

	public void setScryptBlockSize(int scryptBlockSize) {
		this.scryptBlockSize = scryptBlockSize;
	}

	public byte[] getEncryptionMasterKey() {
		return encryptionMasterKey;
	}

	public void setEncryptionMasterKey(byte[] encryptionMasterKey) {
		this.encryptionMasterKey = encryptionMasterKey;
	}

	public byte[] getMacMasterKey() {
		return macMasterKey;
	}

	public void setMacMasterKey(byte[] macMasterKey) {
		this.macMasterKey = macMasterKey;
	}

	public byte[] getVersionMac() {
		return versionMac;
	}

	public void setVersionMac(byte[] versionMac) {
		this.versionMac = versionMac;
	}

}
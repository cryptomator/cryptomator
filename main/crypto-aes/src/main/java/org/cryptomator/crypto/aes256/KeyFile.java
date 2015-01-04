package org.cryptomator.crypto.aes256;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = {"salt", "iv", "iterations", "keyLength", "primaryMasterKey", "hMacMasterKey"})
public class KeyFile implements Serializable {

	private static final long serialVersionUID = 8578363158959619885L;
	private byte[] salt;
	private int iterations;
	private int keyLength;
	private byte[] primaryMasterKey;
	private byte[] hMacMasterKey;

	public byte[] getSalt() {
		return salt;
	}

	public void setSalt(byte[] salt) {
		this.salt = salt;
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

	public byte[] getPrimaryMasterKey() {
		return primaryMasterKey;
	}

	public void setPrimaryMasterKey(byte[] primaryMasterKey) {
		this.primaryMasterKey = primaryMasterKey;
	}

	public byte[] getHMacMasterKey() {
		return hMacMasterKey;
	}

	public void setHMacMasterKey(byte[] hMacMasterKey) {
		this.hMacMasterKey = hMacMasterKey;
	}

}
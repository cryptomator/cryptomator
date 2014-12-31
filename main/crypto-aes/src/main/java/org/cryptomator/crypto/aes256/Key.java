package org.cryptomator.crypto.aes256;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = {"salt", "iv", "iterations", "keyLength", "masterkey", "secondaryKey"})
public class Key implements Serializable {

	private static final long serialVersionUID = 8578363158959619885L;
	private byte[] salt;
	private byte[] iv;
	private int iterations;
	private int keyLength;
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

	public byte[] getMasterkey() {
		return masterkey;
	}

	public void setMasterkey(byte[] masterkey) {
		this.masterkey = masterkey;
	}

}
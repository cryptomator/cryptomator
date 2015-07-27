package org.cryptomator.crypto.aes256;

class Block {

	final byte[] buffer;
	final long blockNumber;

	Block(byte[] buffer, long blockNumber) {
		this.buffer = buffer;
		this.blockNumber = blockNumber;
	}

}

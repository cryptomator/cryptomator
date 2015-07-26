package org.cryptomator.crypto.aes256;

class Block {

	final int numBytes;
	final byte[] buffer;
	final long blockNumber;

	Block(int numBytes, byte[] buffer, long blockNumber) {
		this.numBytes = numBytes;
		this.buffer = buffer;
		this.blockNumber = blockNumber;
	}

}

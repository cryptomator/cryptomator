package org.cryptomator.crypto.aes256;

import java.nio.ByteBuffer;

class Block {

	final ByteBuffer buffer;
	final long blockNumber;

	Block(ByteBuffer buffer, long blockNumber) {
		this.buffer = buffer;
		this.blockNumber = blockNumber;
	}

}

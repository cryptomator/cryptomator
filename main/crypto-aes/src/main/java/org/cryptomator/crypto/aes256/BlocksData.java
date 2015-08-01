package org.cryptomator.crypto.aes256;

import java.nio.ByteBuffer;

class BlocksData {

	public static final int MAX_NUM_BLOCKS = 128;

	final ByteBuffer buffer;
	final long startBlockNum;
	final int numBlocks;

	BlocksData(ByteBuffer buffer, long startBlockNum, int numBlocks) {
		if (numBlocks > MAX_NUM_BLOCKS) {
			throw new IllegalArgumentException("Too many blocks to process at once: " + numBlocks);
		}
		this.buffer = buffer;
		this.startBlockNum = startBlockNum;
		this.numBlocks = numBlocks;
	}

}

/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.crypto;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;

import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.io.ByteBuffers;

class BlockAlignedReadableFile implements ReadableFile {

	private final ReadableFile delegate;
	private final int blockSize;
	private final ByteBuffer currentBlockBuffer;
	private boolean eofReached = false;
	private Mode mode = Mode.PASSTHROUGH;

	private enum Mode {
		BLOCK_ALIGNED, PASSTHROUGH;
	}

	public BlockAlignedReadableFile(ReadableFile delegate, int blockSize) {
		if (blockSize < 1) {
			throw new IllegalArgumentException("Invalid block size");
		}
		this.delegate = delegate;
		this.blockSize = blockSize;
		this.currentBlockBuffer = ByteBuffer.allocate(blockSize);
		this.currentBlockBuffer.flip(); // so remaining() is 0 -> next read will read from physical source.
	}

	@Override
	public void position(long logicalPosition) throws UncheckedIOException {
		switchToBlockAlignedMode();
		long blockNumber = logicalPosition / blockSize;
		long physicalPosition = blockNumber * blockSize;
		assert physicalPosition <= logicalPosition;
		int diff = (int) (logicalPosition - physicalPosition);
		assert diff >= 0;
		assert diff < blockSize;
		delegate.position(physicalPosition);
		eofReached = false;
		readCurrentBlock();
		currentBlockBuffer.position(diff);
	}

	// visible for testing
	void switchToBlockAlignedMode() {
		mode = Mode.BLOCK_ALIGNED;
	}

	@Override
	public int read(ByteBuffer target) throws UncheckedIOException {
		switch (mode) {
		case PASSTHROUGH:
			return delegate.read(target);
		case BLOCK_ALIGNED:
			return readBlockAligned(target);
		default:
			throw new IllegalStateException("Unsupported mode " + mode);
		}
	}

	private int readBlockAligned(ByteBuffer target) {
		if (eofReached) {
			return -1;
		} else {
			int read = 0;
			while (!eofReached && target.hasRemaining()) {
				read += ByteBuffers.copy(currentBlockBuffer, target);
				readCurrentBlockIfNeeded();
			}
			return read;
		}
	}

	private void readCurrentBlockIfNeeded() {
		if (!currentBlockBuffer.hasRemaining()) {
			readCurrentBlock();
		}
	}

	private void readCurrentBlock() {
		currentBlockBuffer.clear();
		if (delegate.read(currentBlockBuffer) == -1) {
			eofReached = true;
		}
		currentBlockBuffer.flip();
	}

	@Override
	public boolean isOpen() {
		return delegate.isOpen();
	}

	@Override
	public long size() throws UncheckedIOException {
		return delegate.size();
	}

	@Override
	public void close() throws UncheckedIOException {
		delegate.close();
	}

}

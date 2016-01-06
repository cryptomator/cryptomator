/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.blockaligned;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;

import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.filesystem.delegating.DelegatingWritableFile;
import org.cryptomator.io.ByteBuffers;

class BlockAlignedWritableFile extends DelegatingWritableFile {

	private final int blockSize;
	private final ReadableFile readableFile;
	private final ByteBuffer currentBlockBuffer;
	private Mode mode = Mode.PASSTHROUGH;

	private enum Mode {
		BLOCK_ALIGNED, PASSTHROUGH;
	}

	public BlockAlignedWritableFile(WritableFile delegate, ReadableFile readableFile, int blockSize) {
		super(delegate);
		this.readableFile = readableFile;
		this.blockSize = blockSize;
		this.currentBlockBuffer = ByteBuffer.allocate(blockSize);
	}

	@Override
	public void position(long logicalPosition) throws UncheckedIOException {
		switchToBlockAlignedMode();
		long blockNumber = logicalPosition / blockSize;
		long physicalPosition = blockNumber * blockSize;
		readableFile.position(physicalPosition);
		readableFile.read(currentBlockBuffer);
		int advance = (int) (logicalPosition - physicalPosition);
		currentBlockBuffer.position(advance);
		super.position(physicalPosition);
	}

	// visible for testing
	void switchToBlockAlignedMode() {
		mode = Mode.BLOCK_ALIGNED;
	}

	@Override
	public int write(ByteBuffer source) throws UncheckedIOException {
		switch (mode) {
		case PASSTHROUGH:
			return super.write(source);
		case BLOCK_ALIGNED:
			return writeBlockAligned(source);
		default:
			throw new IllegalStateException("Unsupported mode " + mode);
		}
	}

	private int writeBlockAligned(ByteBuffer source) {
		int written = 0;
		while (source.hasRemaining()) {
			written += ByteBuffers.copy(source, currentBlockBuffer);
			writeCurrentBlockIfNeeded();
		}
		return written;
	}

	@Override
	public void close() throws UncheckedIOException {
		currentBlockBuffer.flip();
		writeCurrentBlock();
		readableFile.close();
		super.close();
	}

	private void writeCurrentBlockIfNeeded() {
		if (!currentBlockBuffer.hasRemaining()) {
			writeCurrentBlock();
			readCurrentBlock();
		}
	}

	private void writeCurrentBlock() {
		currentBlockBuffer.rewind();
		super.write(currentBlockBuffer);
	}

	private void readCurrentBlock() {
		currentBlockBuffer.clear();
		readableFile.read(currentBlockBuffer);
		currentBlockBuffer.rewind();
	}

}

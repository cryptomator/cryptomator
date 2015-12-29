package org.cryptomator.filesystem.blockaligned;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;

import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.delegating.DelegatingReadableFile;
import org.cryptomator.io.ByteBuffers;

class BlockAlignedReadableFile extends DelegatingReadableFile {

	private final int blockSize;
	private final ByteBuffer currentBlockBuffer;
	private boolean eofReached = false;

	public BlockAlignedReadableFile(ReadableFile delegate, int blockSize) {
		super(delegate);
		if (blockSize < 1) {
			throw new IllegalArgumentException("Invalid block size");
		}
		this.blockSize = blockSize;
		this.currentBlockBuffer = ByteBuffer.allocate(blockSize);
		this.currentBlockBuffer.flip(); // so the next attempt will read from source.
	}

	@Override
	public void position(long logicalPosition) throws UncheckedIOException {
		long blockNumber = logicalPosition / blockSize;
		long physicalPosition = blockNumber * blockSize;
		super.position(physicalPosition);
		eofReached = false;
		readCurrentBlock();
		int advance = (int) (logicalPosition - physicalPosition);
		currentBlockBuffer.position(advance);
	}

	@Override
	public int read(ByteBuffer target) throws UncheckedIOException {
		int read = -1;
		while (!eofReached && target.hasRemaining()) {
			read += ByteBuffers.copy(currentBlockBuffer, target);
			readCurrentBlockIfNeeded();
		}
		return read;
	}

	private void readCurrentBlockIfNeeded() {
		if (!currentBlockBuffer.hasRemaining()) {
			readCurrentBlock();
		}
	}

	private void readCurrentBlock() {
		currentBlockBuffer.clear();
		if (super.read(currentBlockBuffer) == -1) {
			eofReached = true;
		}
		currentBlockBuffer.flip();
	}

}

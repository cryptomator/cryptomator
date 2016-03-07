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
import java.util.Optional;
import java.util.function.Supplier;

import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.io.ByteBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BlockAlignedWritableFile implements WritableFile {

	private static final Logger LOG = LoggerFactory.getLogger(BlockAlignedWritableFile.class);

	private final Supplier<WritableFile> openWritable;
	private final Supplier<ReadableFile> openReadable;
	private final int blockSize;
	private final ByteBuffer currentBlockBuffer;
	private Mode mode = Mode.PASSTHROUGH;
	private Optional<WritableFile> delegate;
	private long logicalPosition;

	private enum Mode {
		BLOCK_ALIGNED, PASSTHROUGH;
	}

	public BlockAlignedWritableFile(Supplier<WritableFile> openWritable, Supplier<ReadableFile> openReadable, int blockSize) {
		this.openWritable = openWritable;
		this.openReadable = openReadable;
		this.blockSize = blockSize;
		this.currentBlockBuffer = ByteBuffer.allocate(blockSize);
		currentBlockBuffer.flip(); // make sure the buffer has no remaining bytes by default
		delegate = Optional.of(openWritable.get());
	}

	@Override
	public void position(long logicalPosition) throws UncheckedIOException {
		switchToBlockAlignedMode();
		this.logicalPosition = logicalPosition;
		readCurrentBlock();
	}

	// visible for testing
	void switchToBlockAlignedMode() {
		LOG.trace("switching to blockaligend write...");
		mode = Mode.BLOCK_ALIGNED;
	}

	@Override
	public int write(ByteBuffer source) throws UncheckedIOException {
		switch (mode) {
		case PASSTHROUGH:
			return delegate.get().write(source);
		case BLOCK_ALIGNED:
			return writeBlockAligned(source);
		default:
			throw new IllegalStateException("Unsupported mode " + mode);
		}
	}

	private int writeBlockAligned(ByteBuffer source) {
		int writtenTotal = 0;
		while (source.hasRemaining()) {
			int written = ByteBuffers.copy(source, currentBlockBuffer);
			logicalPosition += written;
			writeCurrentBlockIfNeeded();
			writtenTotal += written;
		}
		return writtenTotal;
	}

	@Override
	public void close() throws UncheckedIOException {
		currentBlockBuffer.flip();
		writeCurrentBlock();
		delegate.ifPresent(WritableFile::close);
	}

	private void writeCurrentBlockIfNeeded() {
		if (!currentBlockBuffer.hasRemaining()) {
			writeCurrentBlock();
			readCurrentBlock();
		}
	}

	private void writeCurrentBlock() {
		currentBlockBuffer.rewind();
		delegate.get().write(currentBlockBuffer);
	}

	private void readCurrentBlock() {
		// TODO lock that shit

		// determine right position:
		long blockNumber = logicalPosition / blockSize;
		long physicalPosition = blockNumber * blockSize;

		// switch from write to read access:
		delegate.get().close();
		currentBlockBuffer.clear();
		try (ReadableFile r = openReadable.get()) {
			r.position(physicalPosition);
			int numRead = r.read(currentBlockBuffer);
			assert numRead == currentBlockBuffer.position();
		}
		int advance = (int) (logicalPosition - physicalPosition);
		currentBlockBuffer.position(advance);

		// continue write access:
		WritableFile w = openWritable.get();
		w.position(physicalPosition);
		delegate = Optional.of(w);
	}

	@Override
	public boolean isOpen() {
		return delegate.get().isOpen();
	}

	@Override
	public void truncate() throws UncheckedIOException {
		delegate.get().truncate();
	}

}

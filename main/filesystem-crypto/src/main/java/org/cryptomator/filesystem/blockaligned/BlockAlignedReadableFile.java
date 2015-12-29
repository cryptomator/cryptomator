package org.cryptomator.filesystem.blockaligned;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;

import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.delegating.DelegatingReadableFile;

class BlockAlignedReadableFile extends DelegatingReadableFile {

	public BlockAlignedReadableFile(ReadableFile delegate, int blockSize) {
		super(delegate);
	}

	@Override
	public void position(long position) throws UncheckedIOException {
		// TODO Auto-generated method stub
		super.position(position);
	}

	@Override
	public int read(ByteBuffer target) throws UncheckedIOException {
		// TODO Auto-generated method stub
		return super.read(target);
	}

}

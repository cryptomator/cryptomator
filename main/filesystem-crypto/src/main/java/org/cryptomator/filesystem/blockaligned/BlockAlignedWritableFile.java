package org.cryptomator.filesystem.blockaligned;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;

import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.filesystem.delegating.DelegatingWritableFile;

class BlockAlignedWritableFile extends DelegatingWritableFile {

	public BlockAlignedWritableFile(WritableFile delegate, int blockSize) {
		super(delegate);
	}

	@Override
	public void position(long position) throws UncheckedIOException {
		// TODO Auto-generated method stub
		super.position(position);
	}

	@Override
	public int write(ByteBuffer source) throws UncheckedIOException {
		// TODO Auto-generated method stub
		return super.write(source);
	}

}

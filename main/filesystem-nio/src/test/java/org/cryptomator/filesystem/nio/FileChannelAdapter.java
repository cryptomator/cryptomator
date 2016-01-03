package org.cryptomator.filesystem.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

class FileChannelAdapter extends FileChannel {

	@Override
	public int read(ByteBuffer dst) throws IOException {
		return 0;
	}

	@Override
	public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
		return 0;
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		return 0;
	}

	@Override
	public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
		return 0;
	}

	@Override
	public long position() throws IOException {
		return 0;
	}

	@Override
	public FileChannel position(long newPosition) throws IOException {
		return null;
	}

	@Override
	public long size() throws IOException {
		return 0;
	}

	@Override
	public FileChannel truncate(long size) throws IOException {
		return null;
	}

	@Override
	public void force(boolean metaData) throws IOException {
	}

	@Override
	public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
		return 0;
	}

	@Override
	public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
		return 0;
	}

	@Override
	public int read(ByteBuffer dst, long position) throws IOException {
		return 0;
	}

	@Override
	public int write(ByteBuffer src, long position) throws IOException {
		return 0;
	}

	@Override
	public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
		return null;
	}

	@Override
	public FileLock lock(long position, long size, boolean shared) throws IOException {
		return null;
	}

	@Override
	public FileLock tryLock(long position, long size, boolean shared) throws IOException {
		return null;
	}

	@Override
	public void implCloseChannel() throws IOException {
	}

}

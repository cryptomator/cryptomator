package org.cryptomator.crypto.engine.impl;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

class BytesWithSequenceNumber implements Comparable<BytesWithSequenceNumber> {

	private final Future<ByteBuffer> byteBuffer;
	private final long sequenceNumber;

	public BytesWithSequenceNumber(Future<ByteBuffer> byteBuffer, long sequenceNumber) {
		this.byteBuffer = byteBuffer;
		this.sequenceNumber = sequenceNumber;
	}

	public ByteBuffer get() throws InterruptedException {
		try {
			return byteBuffer.get();
		} catch (ExecutionException e) {
			assert e.getCause() instanceof RuntimeException;
			throw (RuntimeException) e.getCause();
		}
	}

	@Override
	public int compareTo(BytesWithSequenceNumber other) {
		return Long.compare(this.sequenceNumber, other.sequenceNumber);
	}

}
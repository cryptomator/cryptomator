package org.cryptomator.filesystem.nio;

import java.util.concurrent.atomic.AtomicInteger;

class OpenCloseCounter {

	private final AtomicInteger counter = new AtomicInteger();

	public void countOpen() {
		counter.incrementAndGet();
	}

	public void countClose() {
		int openCount = counter.decrementAndGet();
		if (openCount < 0) {
			counter.incrementAndGet();
			throw new IllegalStateException("Close without corresponding open");
		}
	}

	public boolean isOpen() {
		return counter.get() > 0;
	}

}

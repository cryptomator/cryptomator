package org.cryptomator.ipc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;

class LoopbackCommunicator implements IpcCommunicator {

	private static final Logger LOG = LoggerFactory.getLogger(LoopbackCommunicator.class);

	private final TransferQueue<IpcMessage> transferQueue = new LinkedTransferQueue<>();

	@Override
	public boolean isClient() {
		return false;
	}

	@Override
	public void listen(IpcMessageListener listener, Executor executor) {
		executor.execute(() -> {
			try {
				var msg = transferQueue.take();
				listener.handleMessage(msg);
			} catch (InterruptedException e) {
				LOG.error("Failed to read IPC message", e);
				Thread.currentThread().interrupt();
			}
		});
	}

	@Override
	public void send(IpcMessage message, Executor executor) {
		executor.execute(() -> {
			try {
				transferQueue.put(message);
			} catch (InterruptedException e) {
				LOG.error("Failed to send IPC message", e);
				Thread.currentThread().interrupt();
			}
		});
	}

	@Override
	public void close() {
		// no-op
	}
}

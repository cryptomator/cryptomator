package org.cryptomator.ipc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.Executor;

class Client implements IpcCommunicator {

	private static final Logger LOG = LoggerFactory.getLogger(Client.class);

	private final SocketChannel socketChannel;

	private Client(SocketChannel socketChannel) {
		this.socketChannel = socketChannel;
	}

	public static Client create(Path socketPath) throws IOException {
		var address = UnixDomainSocketAddress.of(socketPath);
		var socketChannel = SocketChannel.open(address);
		LOG.info("Connected to IPC server on socket {}", socketPath);
		return new Client(socketChannel);
	}

	@Override
	public boolean isClient() {
		return true;
	}

	@Override
	public void listen(IpcMessageListener listener, Executor executor) {
		executor.execute(() -> {
			try {
				while (socketChannel.isConnected()) {
					var msg = IpcMessage.receive(socketChannel);
					listener.handleMessage(msg);
				}
			} catch (IOException e) {
				LOG.error("Failed to read IPC message", e);
			}
		});
	}

	@Override
	public void send(IpcMessage message, Executor executor) {
		executor.execute(() -> {
			try {
				message.send(socketChannel);
			} catch (IOException e) {
				LOG.error("Failed to send IPC message", e);
			}
		});
	}

	@Override
	public void close() throws IOException {
		socketChannel.close();
	}
}

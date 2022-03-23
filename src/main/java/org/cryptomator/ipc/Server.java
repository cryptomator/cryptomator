package org.cryptomator.ipc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.UnsupportedAddressTypeException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executor;

class Server implements IpcCommunicator {

	private static final Logger LOG = LoggerFactory.getLogger(Server.class);

	private final ServerSocketChannel serverSocketChannel;
	private final Path socketPath;

	private Server(ServerSocketChannel serverSocketChannel, Path socketPath) {
		this.serverSocketChannel = serverSocketChannel;
		this.socketPath = socketPath;
	}

	public static Server create(Path socketPath) throws IOException {
		Files.createDirectories(socketPath.getParent());
		var address = UnixDomainSocketAddress.of(socketPath);
		ServerSocketChannel ch = null;
		try {
			ch = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
			ch.bind(address);
			LOG.info("Spawning IPC server listening on socket {}", socketPath);
			return new Server(ch, socketPath);
		} catch (IOException | AlreadyBoundException | UnsupportedAddressTypeException e) {
			if (ch != null) {
				ch.close();
			}
			throw e;
		}
	}

	@Override
	public boolean isClient() {
		return false;
	}

	@Override
	public void listen(IpcMessageListener listener, Executor executor) {
		executor.execute(() -> {
			while (serverSocketChannel.isOpen()) {
				try (var ch = serverSocketChannel.accept()) {
					while (ch.isConnected()) {
						var msg = IpcMessage.receive(ch);
						listener.handleMessage(msg);
					}
				} catch (AsynchronousCloseException e) {
					return; // serverSocketChannel closed or listener interrupted
				} catch (EOFException | ClosedChannelException e) {
					// continue with next connected client
				} catch (IOException e) {
					LOG.error("Failed to read IPC message", e);
				}
			}
		});
	}

	@Override
	public void send(IpcMessage message, Executor executor) {
		executor.execute(() -> {
			try (var ch = serverSocketChannel.accept()) {
				message.send(ch);
			} catch (IOException e) {
				LOG.error("Failed to send IPC message", e);
			}
		});
	}

	@Override
	public void close() throws IOException {
		try {
			serverSocketChannel.close();
		} finally {
			Files.deleteIfExists(socketPath);
			LOG.debug("IPC server closed");
		}
	}
}

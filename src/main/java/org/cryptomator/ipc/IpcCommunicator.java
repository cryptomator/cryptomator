package org.cryptomator.ipc;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.MoreExecutors;

public interface IpcCommunicator extends Closeable {

	Logger LOG = LoggerFactory.getLogger(IpcCommunicator.class);

	/**
	 * Attempts to establish a socket connection via one of the given paths.
	 * <p>
	 * If no connection to an existing sockets can be established, a new socket is created for the first given path.
	 * <p>
	 * If this fails as well, a fallback communicator is returned that allows process-internal communication mocking the API
	 * that would have been used for IPC.
	 *
	 * @param socketPaths The socket path(s)
	 * @return A communicator object that allows sending and receiving messages
	 */
	static IpcCommunicator create(Iterable<Path> socketPaths) {
		if (!socketPaths.iterator().hasNext()) {
			LOG.warn("No socket path provided. Using Loop back Communicator as fallback.");
			return new LoopbackCommunicator();
		}
		for (var p : socketPaths) {
			try {
				var attr = Files.readAttributes(p, BasicFileAttributes.class);
				if (attr.isOther()) {
					return Client.create(p);
				}
			} catch (IOException e) {
				// attempt next socket path
			}
		}
		// Didn't get any connection yet? I.e. we're the first app instance, so let's launch a server:
		try {
			final var socketPath = socketPaths.iterator().next();
			Files.deleteIfExists(socketPath); // ensure path does not exist before creating it
			return Server.create(socketPath);
		} catch (IOException e) {
			LOG.warn("Failed to create IPC server", e);
			return new LoopbackCommunicator();
		}
	}

	boolean isClient();

	/**
	 * Listens to incoming messages until the connection gets closed.
	 *  @param listener The listener that should be notified of incoming messages
	 * @param executor An executor on which to listen. Listening will block, so you might want to use a background thread.
	 * @return
	 */
	void listen(IpcMessageListener listener, Executor executor);

	/**
	 * Sends the given message.
	 *
	 * @param message The message to send
	 * @param executor An executor used to send the message. Sending will block, so you might want to use a background thread.
	 */
	void send(IpcMessage message, Executor executor);

	default void sendRevealRunningApp() {
		send(new RevealRunningAppMessage(), MoreExecutors.directExecutor());
	}

	default void sendHandleLaunchargs(List<String> args) {
		send(new HandleLaunchArgsMessage(args), MoreExecutors.directExecutor());
	}

	/**
	 * Clean up resources.
	 *
	 * @implSpec Must be idempotent
	 * @throws IOException In case of I/O errors.
	 */
	@Override
	void close() throws IOException;

	default void closeUnchecked() throws UncheckedIOException {
		try {
			close();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}

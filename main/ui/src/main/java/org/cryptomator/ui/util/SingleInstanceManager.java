/*******************************************************************************
 * Copyright (c) 2014, 2016 cryptomator.org
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Tillmann Gaida - initial implementation
 ******************************************************************************/
package org.cryptomator.ui.util;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.prefs.Preferences;

import org.apache.commons.io.IOUtils;
import org.cryptomator.ui.Cryptomator;
import org.cryptomator.ui.util.ListenerRegistry.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classes and methods to manage running this application in a mode, which only
 * shows one instance.
 * 
 * @author Tillmann Gaida
 */
public class SingleInstanceManager {
	private static final Logger LOG = LoggerFactory.getLogger(SingleInstanceManager.class);

	/**
	 * Connection to a running instance
	 */
	public static class RemoteInstance implements Closeable {
		final SocketChannel channel;

		RemoteInstance(SocketChannel channel) {
			super();
			this.channel = channel;
		}

		/**
		 * Sends a message to the running instance.
		 * 
		 * @param string
		 *            May not be longer than 2^16 - 1 bytes.
		 * @param timeout
		 *            timeout in milliseconds. this should be larger than the
		 *            precision of {@link System#currentTimeMillis()}.
		 * @return true if the message was sent within the given timeout.
		 * @throws IOException
		 */
		public boolean sendMessage(String string, long timeout) throws IOException {
			Objects.requireNonNull(string);
			byte[] message = string.getBytes(StandardCharsets.UTF_8);
			if (message.length >= 256 * 256) {
				throw new IOException("Message too long.");
			}

			ByteBuffer buf = ByteBuffer.allocate(message.length + 2);
			buf.put((byte) (message.length / 256));
			buf.put((byte) (message.length % 256));
			buf.put(message);

			buf.flip();
			TimeoutTask.attempt(t -> {
				if (channel.write(buf) < 0) {
					return true;
				}
				return !buf.hasRemaining();
			} , timeout, 10);
			return !buf.hasRemaining();
		}

		@Override
		public void close() throws IOException {
			channel.close();
		}

		public int getRemotePort() throws IOException {
			return ((InetSocketAddress) channel.getRemoteAddress()).getPort();
		}
	}

	public static interface MessageListener {
		void handleMessage(String message);
	}

	/**
	 * Represents a socket making this the main instance of the application.
	 */
	public static class LocalInstance implements Closeable {
		private class ChannelState {
			ByteBuffer write = ByteBuffer.wrap(applicationKey.getBytes(StandardCharsets.UTF_8));
			ByteBuffer readLength = ByteBuffer.allocate(2);
			ByteBuffer readMessage = null;
		}

		final ListenerRegistry<MessageListener, String> registry = new ListenerRegistry<>(MessageListener::handleMessage);
		final String applicationKey;
		final ServerSocketChannel channel;
		final Selector selector;
		int port = 0;

		public LocalInstance(String applicationKey, ServerSocketChannel channel, Selector selector) {
			Objects.requireNonNull(applicationKey);
			this.applicationKey = applicationKey;
			this.channel = channel;
			this.selector = selector;
		}

		/**
		 * Register a listener for
		 * 
		 * @param listener
		 * @return
		 */
		public ListenerRegistration registerListener(MessageListener listener) {
			Objects.requireNonNull(listener);
			return registry.registerListener(listener);
		}

		void handleSelection(SelectionKey key) throws IOException {
			if (key.isAcceptable()) {
				final SocketChannel accepted = channel.accept();
				SelectionKey keyOfAcceptedConnection = null;
				try {
					if (accepted != null) {
						LOG.debug("accepted incoming connection");
						accepted.configureBlocking(false);
						keyOfAcceptedConnection = accepted.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
					}
				} finally {
					if (keyOfAcceptedConnection == null) {
						accepted.close();
					}
				}
			}

			if (key.attachment() == null) {
				key.attach(new ChannelState());
			}

			ChannelState state = (ChannelState) key.attachment();

			if (key.isWritable() && state.write != null) {
				((WritableByteChannel) key.channel()).write(state.write);
				if (!state.write.hasRemaining()) {
					state.write = null;
				}
				LOG.trace("wrote welcome. switching to read only.");
				key.interestOps(SelectionKey.OP_READ);
			}

			if (key.isReadable()) {
				ByteBuffer buffer = state.readLength != null ? state.readLength : state.readMessage;

				if (((ReadableByteChannel) key.channel()).read(buffer) < 0) {
					key.cancel();
				}

				if (!buffer.hasRemaining()) {
					buffer.flip();
					if (state.readLength != null) {
						int length = (buffer.get() + 256) % 256;
						length = length * 256 + ((buffer.get() + 256) % 256);

						state.readLength = null;
						state.readMessage = ByteBuffer.allocate(length);
					} else {
						byte[] bytes = new byte[buffer.limit()];
						buffer.get(bytes);

						state.readMessage = null;
						state.readLength = ByteBuffer.allocate(2);

						registry.broadcast(new String(bytes, "UTF-8"));
					}
				}
			}
		}

		@Override
		public void close() {
			IOUtils.closeQuietly(selector);
			IOUtils.closeQuietly(channel);
			if (getSavedPort(applicationKey).orElse(-1).equals(port)) {
				Preferences.userNodeForPackage(Cryptomator.class).remove(applicationKey);
			}
		}

		void selectionLoop() {
			try {
				final Set<SelectionKey> keysToRemove = new HashSet<>();
				while (selector.select() > 0) {
					final Set<SelectionKey> keys = selector.selectedKeys();
					for (SelectionKey key : keys) {
						if (Thread.interrupted()) {
							return;
						}
						try {
							handleSelection(key);
						} catch (IOException | IllegalStateException e) {
							LOG.error("exception in selector", e);
						} finally {
							keysToRemove.add(key);
						}
					}
					keys.removeAll(keysToRemove);
				}
			} catch (ClosedSelectorException e) {
				return;
			} catch (Exception e) {
				LOG.error("error while selecting", e);
			}
		}
	}

	/**
	 * Checks if there is a valid port at
	 * {@link Preferences#userNodeForPackage(Class)} for {@link Cryptomator} under the
	 * given applicationKey, tries to connect to the port at the loopback
	 * address and checks if the port identifies with the applicationKey.
	 * 
	 * @param applicationKey
	 *            key used to load the port and check the identity of the
	 *            connection.
	 * @return
	 */
	public static Optional<RemoteInstance> getRemoteInstance(String applicationKey) {
		Optional<Integer> port = getSavedPort(applicationKey);

		if (!port.isPresent()) {
			return Optional.empty();
		}

		SocketChannel channel = null;
		boolean close = true;
		try {
			channel = SocketChannel.open();
			channel.configureBlocking(false);
			LOG.debug("connecting to instance {}", port.get());
			channel.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port.get()));

			SocketChannel fChannel = channel;
			if (!TimeoutTask.attempt(t -> fChannel.finishConnect(), 1000, 10)) {
				return Optional.empty();
			}

			LOG.debug("connected to instance {}", port.get());

			final byte[] bytes = applicationKey.getBytes(StandardCharsets.UTF_8);
			ByteBuffer buf = ByteBuffer.allocate(bytes.length);
			tryFill(channel, buf, 1000);
			if (buf.hasRemaining()) {
				return Optional.empty();
			}

			buf.flip();

			for (int i = 0; i < bytes.length; i++) {
				if (buf.get() != bytes[i]) {
					return Optional.empty();
				}
			}

			close = false;
			return Optional.of(new RemoteInstance(channel));
		} catch (Exception e) {
			return Optional.empty();
		} finally {
			if (close) {
				IOUtils.closeQuietly(channel);
			}
		}
	}

	static Optional<Integer> getSavedPort(String applicationKey) {
		int port = Preferences.userNodeForPackage(Cryptomator.class).getInt(applicationKey, -1);

		if (port == -1) {
			LOG.info("no running instance found");
			return Optional.empty();
		}

		return Optional.of(port);
	}

	/**
	 * Creates a server socket on a free port and saves the port in
	 * {@link Preferences#userNodeForPackage(Class)} for {@link Cryptomator} under the
	 * given applicationKey.
	 * 
	 * @param applicationKey
	 *            key used to save the port and identify upon connection.
	 * @param exec
	 *            the task which is submitted is interruptable.
	 * @return
	 * @throws IOException
	 */
	public static LocalInstance startLocalInstance(String applicationKey, ExecutorService exec) throws IOException {
		final ServerSocketChannel channel = ServerSocketChannel.open();
		channel.configureBlocking(false);
		channel.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));

		final int port = ((InetSocketAddress) channel.getLocalAddress()).getPort();
		Preferences.userNodeForPackage(Cryptomator.class).putInt(applicationKey, port);
		LOG.debug("InstanceManager bound to port {}", port);

		Selector selector = Selector.open();
		channel.register(selector, SelectionKey.OP_ACCEPT);

		LocalInstance instance = new LocalInstance(applicationKey, channel, selector);

		exec.submit(() -> {
			try {
				instance.port = ((InetSocketAddress) channel.getLocalAddress()).getPort();
			} catch (IOException e) {

			}
			instance.selectionLoop();
		});

		return instance;
	}

	/**
	 * tries to fill the given buffer for the given time
	 * 
	 * @param channel
	 * @param buf
	 * @param timeout
	 * @throws ClosedChannelException
	 * @throws IOException
	 */
	public static <T extends SelectableChannel & ReadableByteChannel> void tryFill(T channel, final ByteBuffer buf, int timeout) throws IOException {
		if (channel.isBlocking()) {
			throw new IllegalStateException("Channel is in blocking mode.");
		}

		try (Selector selector = Selector.open()) {
			channel.register(selector, SelectionKey.OP_READ);

			TimeoutTask.attempt(remainingTime -> {
				if (!buf.hasRemaining()) {
					return true;
				}
				if (selector.select(remainingTime) > 0) {
					if (channel.read(buf) < 0) {
						return true;
					}
				}
				return !buf.hasRemaining();
			} , timeout, 1);
		}
	}
}
/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.launcher;

import com.google.common.io.MoreFiles;
import org.cryptomator.common.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * First running application on a machine opens a server socket. Further processes will connect as clients.
 */
@Singleton
class IpcFactory {

	private static final Logger LOG = LoggerFactory.getLogger(IpcFactory.class);
	private static final String RMI_NAME = "Cryptomator";

	private final List<Path> portFilePaths;
	private final IpcProtocolImpl ipcHandler;

	@Inject
	public IpcFactory(Environment env, IpcProtocolImpl ipcHandler) {
		this.portFilePaths = env.getIpcPortPath().collect(Collectors.toUnmodifiableList());
		this.ipcHandler = ipcHandler;
	}

	public IpcEndpoint create() {
		if (portFilePaths.isEmpty()) {
			LOG.warn("No IPC port file path specified.");
			return new SelfEndpoint(ipcHandler);
		} else {
			System.setProperty("java.rmi.server.hostname", "localhost");
			return attemptClientConnection().or(this::createServerEndpoint).orElseGet(() -> new SelfEndpoint(ipcHandler));
		}
	}

	private Optional<IpcEndpoint> attemptClientConnection() {
		for (Path portFilePath : portFilePaths) {
			try {
				int port = readPort(portFilePath);
				LOG.debug("[Client] Connecting to port {}...", port);
				Registry registry = LocateRegistry.getRegistry("localhost", port, new ClientSocketFactory());
				IpcProtocol remoteInterface = (IpcProtocol) registry.lookup(RMI_NAME);
				return Optional.of(new ClientEndpoint(remoteInterface));
			} catch (NotBoundException | IOException e) {
				LOG.debug("[Client] Failed to connect.");
				// continue with next portFilePath...
			}
		}
		return Optional.empty();
	}

	private int readPort(Path portFilePath) throws IOException {
		try (ReadableByteChannel ch = Files.newByteChannel(portFilePath, StandardOpenOption.READ)) {
			LOG.debug("[Client] Reading IPC port from {}", portFilePath);
			ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES);
			if (ch.read(buf) == Integer.BYTES) {
				buf.flip();
				return buf.getInt();
			} else {
				throw new IOException("Invalid IPC port file.");
			}
		}
	}

	private Optional<IpcEndpoint> createServerEndpoint() {
		assert !portFilePaths.isEmpty();
		Path portFilePath = portFilePaths.get(0);
		try {
			ServerSocket socket = new ServerSocket(0, Byte.MAX_VALUE, InetAddress.getByName("localhost"));
			RMIClientSocketFactory csf = RMISocketFactory.getDefaultSocketFactory();
			SingletonServerSocketFactory ssf = new SingletonServerSocketFactory(socket);
			Registry registry = LocateRegistry.createRegistry(0, csf, ssf);
			UnicastRemoteObject.exportObject(ipcHandler, 0);
			registry.rebind(RMI_NAME, ipcHandler);
			writePort(portFilePath, socket.getLocalPort());
			return Optional.of(new ServerEndpoint(ipcHandler, socket, registry, portFilePath));
		} catch (IOException e) {
			LOG.warn("[Server] Failed to create IPC server.", e);
			return Optional.empty();
		}
	}

	private void writePort(Path portFilePath, int port) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES);
		buf.putInt(port);
		buf.flip();
		MoreFiles.createParentDirectories(portFilePath);
		try (WritableByteChannel ch = Files.newByteChannel(portFilePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			if (ch.write(buf) != Integer.BYTES) {
				throw new IOException("Did not write expected number of bytes.");
			}
		}
		LOG.debug("[Server] Wrote IPC port {} to {}", port, portFilePath);
	}

	interface IpcEndpoint extends Closeable {

		boolean isConnectedToRemote();

		IpcProtocol getRemote();

	}

	static class SelfEndpoint implements IpcEndpoint {

		protected final IpcProtocol remoteObject;

		SelfEndpoint(IpcProtocol remoteObject) {
			this.remoteObject = remoteObject;
		}

		@Override
		public boolean isConnectedToRemote() {
			return false;
		}

		@Override
		public IpcProtocol getRemote() {
			return remoteObject;
		}

		@Override
		public void close() {
			// no-op
		}
	}

	static class ClientEndpoint implements IpcEndpoint {

		private final IpcProtocol remoteInterface;

		public ClientEndpoint(IpcProtocol remoteInterface) {
			this.remoteInterface = remoteInterface;
		}

		public IpcProtocol getRemote() {
			return remoteInterface;
		}

		@Override
		public boolean isConnectedToRemote() {
			return true;
		}

		@Override
		public void close() {
			// no-op
		}

	}

	class ServerEndpoint extends SelfEndpoint {

		private final ServerSocket socket;
		private final Registry registry;
		private final Path portFilePath;

		private ServerEndpoint(IpcProtocol remoteObject, ServerSocket socket, Registry registry, Path portFilePath) {
			super(remoteObject);
			this.socket = socket;
			this.registry = registry;
			this.portFilePath = portFilePath;
		}

		@Override
		public void close() {
			try {
				registry.unbind(RMI_NAME);
				UnicastRemoteObject.unexportObject(remoteObject, true);
				socket.close();
				Files.deleteIfExists(portFilePath);
				LOG.debug("[Server] Shut down");
			} catch (NotBoundException | IOException e) {
				LOG.warn("[Server] Error shutting down:", e);
			}
		}

	}

	/**
	 * Always returns the same pre-constructed server socket.
	 */
	private static class SingletonServerSocketFactory implements RMIServerSocketFactory {

		private final ServerSocket socket;

		public SingletonServerSocketFactory(ServerSocket socket) {
			this.socket = socket;
		}

		@Override
		public synchronized ServerSocket createServerSocket(int port) throws IOException {
			if (port != 0) {
				throw new IllegalArgumentException("This factory doesn't support specific ports.");
			}
			return this.socket;
		}

	}

	/**
	 * Creates client sockets with short timeouts.
	 */
	private static class ClientSocketFactory implements RMIClientSocketFactory {

		@Override
		public Socket createSocket(String host, int port) throws IOException {
			return new SocketWithFixedTimeout(host, port, 1000);
		}

	}

	private static class SocketWithFixedTimeout extends Socket {

		public SocketWithFixedTimeout(String host, int port, int timeoutInMs) throws UnknownHostException, IOException {
			super(host, port);
			super.setSoTimeout(timeoutInMs);
		}

		@Override
		public synchronized void setSoTimeout(int timeout) throws SocketException {
			// do nothing, timeout is fixed
		}

	}

}

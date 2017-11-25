/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.launcher;

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
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.ConnectException;
import java.rmi.ConnectIOException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.UnicastRemoteObject;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.MoreFiles;

/**
 * First running application on a machine opens a server socket. Further processes will connect as clients.
 */
abstract class InterProcessCommunicator implements InterProcessCommunicationProtocol, Closeable {

	private static final Logger LOG = LoggerFactory.getLogger(InterProcessCommunicator.class);
	private static final String RMI_NAME = "Cryptomator";

	public abstract boolean isServer();

	/**
	 * @param endpoint The server-side communication endpoint.
	 * @return Either a client or a server communicator.
	 * @throws IOException In case of communication errors.
	 */
	public static InterProcessCommunicator start(InterProcessCommunicationProtocol endpoint) throws IOException {
		return start(getIpcPortPath(), endpoint);
	}

	// visible for testing
	static InterProcessCommunicator start(Path portFilePath, InterProcessCommunicationProtocol endpoint) throws IOException {
		System.setProperty("java.rmi.server.hostname", "localhost");
		try {
			// try to connect to existing server:
			ClientCommunicator client = new ClientCommunicator(portFilePath);
			LOG.trace("Connected to running process.");
			return client;
		} catch (ConnectException | ConnectIOException | NotBoundException e) {
			LOG.debug("Could not connect to running process.");
			// continue
		}

		// spawn a new server:
		LOG.trace("Spawning new server...");
		ServerCommunicator server = new ServerCommunicator(endpoint, portFilePath);
		LOG.debug("Server listening on port {}.", server.getPort());
		return server;
	}

	private static Path getIpcPortPath() {
		final String settingsPathProperty = System.getProperty("cryptomator.ipcPortPath");
		if (settingsPathProperty == null) {
			LOG.warn("System property cryptomator.ipcPortPath not set.");
			return Paths.get(".ipcPort.tmp");
		} else {
			return Paths.get(replaceHomeDir(settingsPathProperty));
		}
	}

	private static String replaceHomeDir(String path) {
		if (path.startsWith("~/")) {
			return SystemUtils.USER_HOME + path.substring(1);
		} else {
			return path;
		}
	}

	public static class ClientCommunicator extends InterProcessCommunicator {

		private final IpcProtocolRemote remote;

		private ClientCommunicator(Path portFilePath) throws ConnectException, NotBoundException, RemoteException {
			if (Files.notExists(portFilePath)) {
				throw new ConnectException("No IPC port file.");
			}
			try {
				int port = ClientCommunicator.readPort(portFilePath);
				LOG.debug("Connecting to port {}...", port);
				Registry registry = LocateRegistry.getRegistry("localhost", port, new ClientSocketFactory());
				this.remote = (IpcProtocolRemote) registry.lookup(RMI_NAME);
			} catch (IOException e) {
				throw new ConnectException("Error reading IPC port file.");
			}
		}

		private static int readPort(Path portFilePath) throws IOException {
			ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES);
			try (ReadableByteChannel ch = Files.newByteChannel(portFilePath, StandardOpenOption.READ)) {
				if (ch.read(buf) == Integer.BYTES) {
					buf.flip();
					return buf.getInt();
				} else {
					throw new IOException("Invalid IPC port file.");
				}
			}
		}

		@Override
		public void handleLaunchArgs(String[] args) {
			try {
				remote.handleLaunchArgs(args);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public boolean isServer() {
			return false;
		}

		@Override
		public void close() {
			// no-op
		}

	}

	public static class ServerCommunicator extends InterProcessCommunicator {

		private final ServerSocket socket;
		private final Registry registry;
		private final IpcProtocolRemoteImpl remote;
		private final Path portFilePath;

		private ServerCommunicator(InterProcessCommunicationProtocol delegate, Path portFilePath) throws IOException {
			this.socket = new ServerSocket(0, Byte.MAX_VALUE, InetAddress.getByName("localhost"));
			RMIClientSocketFactory csf = RMISocketFactory.getDefaultSocketFactory();
			SingletonServerSocketFactory ssf = new SingletonServerSocketFactory(socket);
			this.registry = LocateRegistry.createRegistry(0, csf, ssf);
			this.remote = new IpcProtocolRemoteImpl(delegate);
			UnicastRemoteObject.exportObject(remote, 0);
			registry.rebind(RMI_NAME, remote);
			this.portFilePath = portFilePath;
			ServerCommunicator.writePort(portFilePath, socket.getLocalPort());
		}

		private static void writePort(Path portFilePath, int port) throws IOException {
			ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES);
			buf.putInt(port);
			buf.flip();
			MoreFiles.createParentDirectories(portFilePath);
			try (WritableByteChannel ch = Files.newByteChannel(portFilePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
				if (ch.write(buf) != Integer.BYTES) {
					throw new IOException("Did not write expected number of bytes.");
				}
			}
		}

		@Override
		public void handleLaunchArgs(String[] args) {
			throw new UnsupportedOperationException("Server doesn't invoke methods.");
		}

		@Override
		public boolean isServer() {
			return true;
		}

		private int getPort() {
			return socket.getLocalPort();
		}

		@Override
		public void close() {
			try {
				registry.unbind(RMI_NAME);
				UnicastRemoteObject.unexportObject(remote, true);
				socket.close();
				Files.deleteIfExists(portFilePath);
				LOG.debug("Server shut down.");
			} catch (NotBoundException | IOException e) {
				LOG.warn("Failed to close IPC Server.", e);
			}
		}

	}

	private static interface IpcProtocolRemote extends Remote {
		void handleLaunchArgs(String[] args) throws RemoteException;
	}

	private static class IpcProtocolRemoteImpl implements IpcProtocolRemote {

		private final InterProcessCommunicationProtocol delegate;

		protected IpcProtocolRemoteImpl(InterProcessCommunicationProtocol delegate) throws RemoteException {
			this.delegate = delegate;
		}

		@Override
		public void handleLaunchArgs(String[] args) {
			delegate.handleLaunchArgs(args);
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

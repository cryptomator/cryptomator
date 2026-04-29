package org.cryptomator.common.mount;

import org.cryptomator.integrations.mount.MountFailedException;

/**
 * Thrown by {@link Mounter} if a local loopback server can not be started because its TCP port is already occupied.
 */
public class PortAlreadyInUseException extends MountFailedException {

	private final int port;
	private final boolean defaultPort;

	public PortAlreadyInUseException(int port, boolean defaultPort, MountFailedException cause) {
		super("Port " + port + " is already in use.", cause);
		this.port = port;
		this.defaultPort = defaultPort;
	}

	public int getPort() {
		return port;
	}

	public boolean isDefaultPort() {
		return defaultPort;
	}
}

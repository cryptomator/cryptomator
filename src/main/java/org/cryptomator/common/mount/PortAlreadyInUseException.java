package org.cryptomator.common.mount;

import org.cryptomator.integrations.mount.MountFailedException;

/**
 * Thrown by {@link Mounter} if a local loopback server can not be started because its TCP port is already occupied.
 */
public class PortAlreadyInUseException extends MountFailedException {

	private final int port;
	private final boolean defaultPort;

	/**
	 * Creates a typed mount failure for an occupied loopback port.
	 *
	 * @param port The TCP port that could not be bound.
	 * @param defaultPort {@code true} if the port came from the global default volume settings.
	 * @param cause The original mount failure containing the binding error.
	 */
	public PortAlreadyInUseException(int port, boolean defaultPort, MountFailedException cause) {
		super("Port " + port + " is already in use.", cause);
		this.port = port;
		this.defaultPort = defaultPort;
	}

	/**
	 * @return The TCP port that was already occupied.
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @return {@code true} if the occupied port belongs to the global default volume settings.
	 */
	public boolean isDefaultPort() {
		return defaultPort;
	}
}

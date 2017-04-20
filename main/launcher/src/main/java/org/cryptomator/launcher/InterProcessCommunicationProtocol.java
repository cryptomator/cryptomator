package org.cryptomator.launcher;

public interface InterProcessCommunicationProtocol {
	void handleLaunchArgs(String[] args);
}
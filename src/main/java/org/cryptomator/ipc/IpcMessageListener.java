package org.cryptomator.ipc;

import java.util.List;

public interface IpcMessageListener {

	default void handleMessage(IpcMessage message) {
		switch (message) {
			case RevealRunningAppMessage x -> revealRunningApp();
			case HandleLaunchArgsMessage hlam -> handleLaunchArgs(hlam.args());
			default -> {}
		}
	}

	void revealRunningApp();

	void handleLaunchArgs(List<String> args);

}

package org.cryptomator.ipc;

import java.util.List;

public interface IpcMessageListener {

	default void handleMessage(IpcMessage message) {
		if (message instanceof RevealRunningAppMessage) {
			revealRunningApp();
		} else if (message instanceof HandleLaunchArgsMessage m) {
			handleLaunchArgs(m.args());
		}
	}

	void revealRunningApp();

	void handleLaunchArgs(List<String> args);

}

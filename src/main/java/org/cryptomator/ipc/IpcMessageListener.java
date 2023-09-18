package org.cryptomator.ipc;

import java.util.List;

public interface IpcMessageListener {

	default void handleMessage(IpcMessage message) {
		switch (message) {
			case RevealRunningAppMessage _ -> revealRunningApp();
			case HandleLaunchArgsMessage m -> handleLaunchArgs(m.args());
		}
	}

	void revealRunningApp();

	void handleLaunchArgs(List<String> args);

}

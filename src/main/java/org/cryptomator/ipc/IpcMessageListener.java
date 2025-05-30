package org.cryptomator.ipc;

import java.util.List;

public interface IpcMessageListener {

	default void handleMessage(IpcMessage message) {
		switch (message) {
			case RevealRunningAppMessage m -> revealRunningApp(); // TODO: rename to _ with JEP 443
			case HandleLaunchArgsMessage m -> handleLaunchArgs(m.args());
		}
	}

	void revealRunningApp();

	void handleLaunchArgs(List<String> args);

}

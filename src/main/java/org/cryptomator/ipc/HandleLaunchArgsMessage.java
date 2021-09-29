package org.cryptomator.ipc;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

record HandleLaunchArgsMessage(List<String> args) implements IpcMessage {

	private static final char DELIMITER = '\n';

	public static HandleLaunchArgsMessage decode(ByteBuffer encoded) {
		var str = StandardCharsets.UTF_8.decode(encoded).toString();
		var args = Splitter.on(DELIMITER).omitEmptyStrings().splitToList(str);
		return new HandleLaunchArgsMessage(args);
	}

	@Override
	public MessageType getMessageType() {
		return MessageType.HANDLE_LAUNCH_ARGS;
	}

	@Override
	public ByteBuffer encodePayload() {
		var str = Joiner.on(DELIMITER).join(args);
		return StandardCharsets.UTF_8.encode(str);
	}
}

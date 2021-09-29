package org.cryptomator.ipc;

import java.nio.ByteBuffer;

public record RevealRunningAppMessage() implements IpcMessage {

	static RevealRunningAppMessage decode(ByteBuffer ignored) {
		return new RevealRunningAppMessage();
	}

	@Override
	public MessageType getMessageType() {
		return MessageType.REVEAL_RUNNING_APP;
	}

	@Override
	public ByteBuffer encodePayload() {
		return ByteBuffer.allocate(0);
	}
}

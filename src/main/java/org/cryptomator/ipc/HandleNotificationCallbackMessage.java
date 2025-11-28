package org.cryptomator.ipc;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

record HandleNotificationCallbackMessage(String content) implements IpcMessage {

	@Override
	public MessageType getMessageType() {
		return MessageType.HANDLE_NOTIFICATION_CALLBACK;
	}

	@Override
	public ByteBuffer encodePayload() {
		return ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8));
	}

	public static IpcMessage decode(ByteBuffer byteBuffer) {
		var content = StandardCharsets.UTF_8.decode(byteBuffer).toString();
		return new HandleNotificationCallbackMessage(content);
	}
}

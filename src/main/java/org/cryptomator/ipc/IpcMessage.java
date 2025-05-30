package org.cryptomator.ipc;

import org.cryptomator.cryptolib.common.ByteBuffers;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.function.Function;

//TODO can the enum be removed?
sealed interface IpcMessage permits HandleLaunchArgsMessage, RevealRunningAppMessage {

	enum MessageType {
		REVEAL_RUNNING_APP(RevealRunningAppMessage::decode),
		HANDLE_LAUNCH_ARGS(HandleLaunchArgsMessage::decode);

		private final Function<ByteBuffer, IpcMessage> decoder;

		MessageType(Function<ByteBuffer, IpcMessage> decoder) {
			this.decoder = decoder;
		}

		static MessageType forOrdinal(int ordinal) {
			try {
				return values()[ordinal];
			} catch (IndexOutOfBoundsException e) {
				throw new IllegalArgumentException("No such message type: " + ordinal, e);
			}
		}

		IpcMessage decodePayload(ByteBuffer payload) {
			return decoder.apply(payload);
		}
	}

	MessageType getMessageType();

	ByteBuffer encodePayload();

	static IpcMessage receive(ReadableByteChannel channel) throws IOException {
		var header = ByteBuffer.allocate(2 * Integer.BYTES);
		if (ByteBuffers.fill(channel, header) < header.capacity()) {
			throw new EOFException();
		}
		header.flip();
		int typeNo = header.getInt();
		int length = header.getInt();
		MessageType type = MessageType.forOrdinal(typeNo);
		var payload = ByteBuffer.allocate(length);
		ByteBuffers.fill(channel, payload);
		payload.flip();
		return type.decodePayload(payload);
	}

	default void send(WritableByteChannel channel) throws IOException {
		var payload = encodePayload();
		var buf = ByteBuffer.allocate(2 * Integer.BYTES + payload.remaining());
		buf.putInt(getMessageType().ordinal()); // message type
		buf.putInt(payload.remaining()); // message length
		buf.put(payload); // message
		buf.flip();
		while (buf.hasRemaining()) {
			channel.write(buf);
		}
	}
}

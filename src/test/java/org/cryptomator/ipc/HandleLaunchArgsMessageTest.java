package org.cryptomator.ipc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class HandleLaunchArgsMessageTest {

	@Test
	public void testSendAndReceiveSingleArgument(@TempDir Path tmpDir) throws IOException {
		var message = new HandleLaunchArgsMessage(List.of("singleArgument"));

		var file = tmpDir.resolve("single.tmp");
		try (var ch = FileChannel.open(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
			message.send(ch);
			ch.position(0);
			if (IpcMessage.receive(ch) instanceof HandleLaunchArgsMessage received) {
				Assertions.assertArrayEquals(message.args().toArray(), received.args().toArray());
			} else {
				Assertions.fail("Received message of unexpected class");
			}
		}
	}

	@Test
	public void testSendAndReceiveMultipleArguments(@TempDir Path tmpDir) throws IOException {
		var message = new HandleLaunchArgsMessage(List.of("arg1", "arg2", "arg3"));

		var file = tmpDir.resolve("multiple.tmp");
		try (var ch = FileChannel.open(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
			message.send(ch);
			ch.position(0);
			if (IpcMessage.receive(ch) instanceof HandleLaunchArgsMessage received) {
				Assertions.assertArrayEquals(message.args().toArray(), received.args().toArray());
			} else {
				Assertions.fail("Received message of unexpected class");
			}
		}
	}

	@Test
	public void testSendAndReceiveWithEmptyArguments(@TempDir Path tmpDir) throws IOException {
		var message = new HandleLaunchArgsMessage(List.of());

		var file = tmpDir.resolve("empty.tmp");
		try (var ch = FileChannel.open(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
			message.send(ch);
			ch.position(0);
			if (IpcMessage.receive(ch) instanceof HandleLaunchArgsMessage received) {
				Assertions.assertArrayEquals(message.args().toArray(), received.args().toArray());
			} else {
				Assertions.fail("Received message of unexpected class");
			}
		}
	}

	@Test
	public void testDecodeSpecialCharacters() {
		ByteBuffer buffer = ByteBuffer.wrap("arg1\narg2\narg3".getBytes(StandardCharsets.UTF_8));
		HandleLaunchArgsMessage message = HandleLaunchArgsMessage.decode(buffer);
		Assertions.assertEquals(List.of("arg1", "arg2", "arg3"), message.args());
	}

	@Test
	public void testDecodeEmptyPayload() {
		ByteBuffer buffer = ByteBuffer.wrap("".getBytes(StandardCharsets.UTF_8));
		HandleLaunchArgsMessage message = HandleLaunchArgsMessage.decode(buffer);
		Assertions.assertEquals(List.of(), message.args());
	}

	@Test
	public void testDecodeOnlyDelimiters() {
		ByteBuffer buffer = ByteBuffer.wrap("\n\n\n".getBytes(StandardCharsets.UTF_8));
		HandleLaunchArgsMessage message = HandleLaunchArgsMessage.decode(buffer);
		Assertions.assertEquals(List.of(), message.args());
	}
}

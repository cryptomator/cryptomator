package org.cryptomator.ipc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class HandleLaunchArgsMessageTest {

	@Test
	public void testSendAndReceive(@TempDir Path tmpDir) throws IOException {
		var message = new HandleLaunchArgsMessage(List.of("hello world", "foo bar"));

		var file = tmpDir.resolve("tmp.file");
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
	public void testSendAndReceiveEmpty(@TempDir Path tmpDir) throws IOException {
		var message = new HandleLaunchArgsMessage(List.of());

		var file = tmpDir.resolve("tmp.file");
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
}
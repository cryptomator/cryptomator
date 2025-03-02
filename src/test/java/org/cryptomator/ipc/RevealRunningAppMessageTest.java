package org.cryptomator.ipc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class RevealRunningAppMessageTest {

	@Test
	public void testSendAndReceive(@TempDir Path tmpDir) throws IOException {
		var message = new RevealRunningAppMessage();

		var file = tmpDir.resolve("tmp.file");
		try (var ch = FileChannel.open(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
			message.send(ch);
			ch.position(0);
			if (IpcMessage.receive(ch) instanceof RevealRunningAppMessage received) {
				Assertions.assertNotNull(received);
			} else {
				Assertions.fail("Received message of unexpected class");
			}
		}
	}

	@Test
	public void testDecodeFromEmptyBuffer() {
		ByteBuffer buffer = ByteBuffer.allocate(0);
		RevealRunningAppMessage message = RevealRunningAppMessage.decode(buffer);
		Assertions.assertNotNull(message);
	}

	@Test
	public void testEncodePayload() {
		RevealRunningAppMessage message = new RevealRunningAppMessage();
		ByteBuffer buffer = message.encodePayload();
		Assertions.assertEquals(0, buffer.remaining());
	}
}

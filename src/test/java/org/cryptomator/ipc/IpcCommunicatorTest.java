package org.cryptomator.ipc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

public class IpcCommunicatorTest {

	@Test
	public void testSendAndReceive(@TempDir Path tmpDir) throws IOException, InterruptedException {
		var socketPath = tmpDir.resolve("foo.sock");
		try (var server = IpcCommunicator.create(List.of(socketPath));
			 var client = IpcCommunicator.create(List.of(socketPath))) {
			Assertions.assertNotSame(server, client);

			var cdl = new CountDownLatch(1);
			var executor = Executors.newSingleThreadExecutor();
			server.listen(new IpcMessageListener() {
				@Override
				public void revealRunningApp() {
					cdl.countDown();
				}

				@Override
				public void handleLaunchArgs(List<String> args) {

				}
			}, executor);
			client.sendRevealRunningApp();

			Assertions.assertTimeoutPreemptively(Duration.ofMillis(300), (Executable) cdl::await);
			executor.shutdown();
		}
	}

}
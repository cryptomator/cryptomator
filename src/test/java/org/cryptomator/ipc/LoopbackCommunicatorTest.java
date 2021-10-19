package org.cryptomator.ipc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

public class LoopbackCommunicatorTest {

	@Test
	public void testSendAndReceive() {
		try (var communicator = new LoopbackCommunicator()) {
			var cdl = new CountDownLatch(1);
			var executor = Executors.newSingleThreadExecutor();
			communicator.listen(new IpcMessageListener() {
				@Override
				public void revealRunningApp() {
					cdl.countDown();
				}

				@Override
				public void handleLaunchArgs(List<String> args) {

				}
			}, executor);
			communicator.sendRevealRunningApp();

			Assertions.assertTimeoutPreemptively(Duration.ofMillis(300), (Executable) cdl::await);
			executor.shutdown();
		}
	}

}
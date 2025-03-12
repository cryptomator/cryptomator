package org.cryptomator;

import javafx.application.Platform;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class JavaFXUtil {

	private JavaFXUtil() {}

	public static boolean startPlatform() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		try {
			Platform.startup(latch::countDown);
		} catch (IllegalStateException e) {
			//already initialized
			latch.countDown();
		}
		return latch.await(5, TimeUnit.SECONDS);
	}

}

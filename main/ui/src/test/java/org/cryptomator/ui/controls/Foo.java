package org.cryptomator.ui.controls;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

public class Foo {
	
	@Test
	public void foo() {
		StringProperty str = new SimpleStringProperty("foo");
		CountDownLatch done = new CountDownLatch(1);
		str.addListener(observable -> {
			Assertions.assertEquals("bar", str.get());
			done.countDown();
		});
		str.set("bar");
		Assertions.assertTimeoutPreemptively(Duration.ofMillis(100), () -> {
			done.await();
		});
	}

}

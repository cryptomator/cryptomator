/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.logging;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;

public class LaunchBasedTriggeringPolicyTest {

	@Test
	public void testTriggerOnceAndNeverAgain() {
		LaunchBasedTriggeringPolicy<Object> policy = new LaunchBasedTriggeringPolicy<>();
		File activeFile = Mockito.mock(File.class);
		Object event = Mockito.mock(Object.class);

		// 1st invocation
		boolean triggered = policy.isTriggeringEvent(activeFile, event);
		Assertions.assertTrue(triggered);

		// 2nd invocation
		triggered = policy.isTriggeringEvent(activeFile, event);
		Assertions.assertFalse(triggered);

		// 3rd invocation
		triggered = policy.isTriggeringEvent(activeFile, event);
		Assertions.assertFalse(triggered);

		Mockito.verifyZeroInteractions(activeFile);
		Mockito.verifyZeroInteractions(event);
	}

}

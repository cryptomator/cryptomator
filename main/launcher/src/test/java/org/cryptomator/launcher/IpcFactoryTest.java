/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.launcher;

import org.cryptomator.common.Environment;
import org.cryptomator.launcher.IpcFactory.IpcEndpoint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class IpcFactoryTest {

	private Environment environment = Mockito.mock(Environment.class);
	private IpcProtocolImpl protocolHandler = Mockito.mock(IpcProtocolImpl.class);

	@Test
	@DisplayName("Wihout IPC port files")
	public void testNoIpcWithoutPortFile() throws IOException {
		IpcFactory inTest = new IpcFactory(environment, protocolHandler);

		Mockito.when(environment.getIpcPortPath()).thenReturn(Stream.empty());
		try (IpcEndpoint endpoint1 = inTest.create()) {
			Assertions.assertEquals(IpcFactory.SelfEndpoint.class, endpoint1.getClass());
			Assertions.assertFalse(endpoint1.isConnectedToRemote());
			Assertions.assertSame(protocolHandler, endpoint1.getRemote());
			try (IpcEndpoint endpoint2 = inTest.create()) {
				Assertions.assertEquals(IpcFactory.SelfEndpoint.class, endpoint2.getClass());
				Assertions.assertNotSame(endpoint1, endpoint2);
				Assertions.assertFalse(endpoint2.isConnectedToRemote());
				Assertions.assertSame(protocolHandler, endpoint2.getRemote());
			}
		}
	}

	@Test
	@DisplayName("Start server and client with port shared via file")
	public void testInterProcessCommunication(@TempDir Path tmpDir) throws IOException {
		Path portFile = tmpDir.resolve("testPortFile");
		Mockito.when(environment.getIpcPortPath()).thenReturn(Stream.of(portFile));
		IpcFactory inTest = new IpcFactory(environment, protocolHandler);

		Assertions.assertFalse(Files.exists(portFile));
		try (IpcEndpoint endpoint1 = inTest.create()) {
			Assertions.assertEquals(IpcFactory.ServerEndpoint.class, endpoint1.getClass());
			Assertions.assertFalse(endpoint1.isConnectedToRemote());
			Assertions.assertTrue(Files.exists(portFile));
			Assertions.assertSame(protocolHandler, endpoint1.getRemote());
			Mockito.verifyZeroInteractions(protocolHandler);
			try (IpcEndpoint endpoint2 = inTest.create()) {
				Assertions.assertEquals(IpcFactory.ClientEndpoint.class, endpoint2.getClass());
				Assertions.assertNotSame(endpoint1, endpoint2);
				Assertions.assertTrue(endpoint2.isConnectedToRemote());
				Assertions.assertNotSame(protocolHandler, endpoint2.getRemote());
				Mockito.verifyZeroInteractions(protocolHandler);
				endpoint2.getRemote().handleLaunchArgs(new String[] {"foo"});
				Mockito.verify(protocolHandler).handleLaunchArgs(new String[] {"foo"});
			}
		}
	}

}

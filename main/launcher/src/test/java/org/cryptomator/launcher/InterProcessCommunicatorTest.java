package org.cryptomator.launcher;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class InterProcessCommunicatorTest {

	Path portFilePath = Mockito.mock(Path.class);
	FileSystem fs = Mockito.mock(FileSystem.class);
	FileSystemProvider provider = Mockito.mock(FileSystemProvider.class);
	SeekableByteChannel portFileChannel = Mockito.mock(SeekableByteChannel.class);
	AtomicInteger port = new AtomicInteger(-1);

	@Before
	public void setup() throws IOException {
		Mockito.when(portFilePath.getFileSystem()).thenReturn(fs);
		Mockito.when(fs.provider()).thenReturn(provider);
		Mockito.when(provider.newByteChannel(Mockito.eq(portFilePath), Mockito.any(), Mockito.any())).thenReturn(portFileChannel);
		Mockito.when(portFileChannel.read(Mockito.any())).then(invocation -> {
			ByteBuffer buf = invocation.getArgument(0);
			buf.putInt(port.get());
			return Integer.BYTES;
		});
		Mockito.when(portFileChannel.write(Mockito.any())).then(invocation -> {
			ByteBuffer buf = invocation.getArgument(0);
			port.set(buf.getInt());
			return Integer.BYTES;
		});
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testStartWithDummyPort1() throws IOException {
		port.set(0);
		InterProcessCommunicationProtocol protocol = Mockito.mock(InterProcessCommunicationProtocol.class);
		try (InterProcessCommunicator result = InterProcessCommunicator.start(portFilePath, protocol)) {
			Assert.assertTrue(result.isServer());
			Mockito.verifyZeroInteractions(protocol);
			result.handleLaunchArgs(new String[] {"foo"});
		}
	}

	@Test
	public void testStartWithDummyPort2() throws IOException {
		Mockito.doThrow(new NoSuchFileException("port file")).when(provider).checkAccess(portFilePath);

		InterProcessCommunicationProtocol protocol = Mockito.mock(InterProcessCommunicationProtocol.class);
		try (InterProcessCommunicator result = InterProcessCommunicator.start(portFilePath, protocol)) {
			Assert.assertTrue(result.isServer());
			Mockito.verifyZeroInteractions(protocol);
		}
	}

	@Test
	public void testInterProcessCommunication() throws IOException, InterruptedException {
		port.set(-1);
		InterProcessCommunicationProtocol protocol = Mockito.mock(InterProcessCommunicationProtocol.class);
		try (InterProcessCommunicator result1 = InterProcessCommunicator.start(portFilePath, protocol)) {
			Assert.assertTrue(result1.isServer());
			Mockito.verifyZeroInteractions(protocol);

			try (InterProcessCommunicator result2 = InterProcessCommunicator.start(portFilePath, null)) {
				Assert.assertFalse(result2.isServer());
				Assert.assertNotSame(result1, result2);

				result2.handleLaunchArgs(new String[] {"foo"});
				Mockito.verify(protocol).handleLaunchArgs(new String[] {"foo"});
			}
		}
	}

}

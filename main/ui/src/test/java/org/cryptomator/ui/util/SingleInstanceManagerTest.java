/*******************************************************************************
 * Copyright (c) 2014, 2016 cryptomator.org
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Tillmann Gaida - initial implementation
 ******************************************************************************/
package org.cryptomator.ui.util;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;

import org.cryptomator.ui.util.SingleInstanceManager.LocalInstance;
import org.cryptomator.ui.util.SingleInstanceManager.MessageListener;
import org.cryptomator.ui.util.SingleInstanceManager.RemoteInstance;
import org.junit.Test;

public class SingleInstanceManagerTest {
	@Test(timeout = 10000)
	public void testTryFillTimeout() throws Exception {
		try (final ServerSocket socket = new ServerSocket(0)) {
			// we need to asynchronously accept the connection
			final ForkJoinTask<?> forked = ForkJoinTask.adapt(() -> {
				try {
					socket.setSoTimeout(1000);

					socket.accept();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}).fork();

			try (SocketChannel channel = SocketChannel.open()) {
				channel.configureBlocking(false);
				channel.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), socket.getLocalPort()));
				TimeoutTask.attempt(t -> channel.finishConnect(), 1000, 1);
				final ByteBuffer buffer = ByteBuffer.allocate(1);
				SingleInstanceManager.tryFill(channel, buffer, 1000);
				assertTrue(buffer.hasRemaining());
			}

			forked.join();
		}
	}

	@Test(timeout = 10000)
	public void testTryFill() throws Exception {
		try (final ServerSocket socket = new ServerSocket(0)) {
			// we need to asynchronously accept the connection
			final ForkJoinTask<?> forked = ForkJoinTask.adapt(() -> {
				try {
					socket.setSoTimeout(1000);

					socket.accept().getOutputStream().write(1);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}).fork();

			try (SocketChannel channel = SocketChannel.open()) {
				channel.configureBlocking(false);
				channel.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), socket.getLocalPort()));
				TimeoutTask.attempt(t -> channel.finishConnect(), 1000, 1);
				final ByteBuffer buffer = ByteBuffer.allocate(1);
				SingleInstanceManager.tryFill(channel, buffer, 1000);
				assertFalse(buffer.hasRemaining());
			}

			forked.join();
		}
	}

	String appKey = "APPKEY";

	@Test
	public void testOneMessage() throws Exception {
		ExecutorService exec = Executors.newCachedThreadPool();

		try {
			final LocalInstance server = SingleInstanceManager.startLocalInstance(appKey, exec);
			final Optional<RemoteInstance> r = SingleInstanceManager.getRemoteInstance(appKey);

			CountDownLatch latch = new CountDownLatch(1);

			final MessageListener listener = spy(new MessageListener() {
				@Override
				public void handleMessage(String message) {
					latch.countDown();
				}
			});
			server.registerListener(listener);

			assertTrue(r.isPresent());

			String message = "Is this thing on?";
			assertTrue(r.get().sendMessage(message, 1000));
			System.out.println("wrote message");

			latch.await(10, TimeUnit.SECONDS);

			verify(listener).handleMessage(message);
		} finally {
			exec.shutdownNow();
		}
	}

	@Test(timeout = 60000)
	public void testALotOfMessages() throws Exception {
		final int connectors = 256;
		final int messagesPerConnector = 256;

		ExecutorService exec = Executors.newSingleThreadExecutor();
		ExecutorService exec2 = Executors.newFixedThreadPool(16);

		try (final LocalInstance server = SingleInstanceManager.startLocalInstance(appKey, exec)) {

			Set<String> sentMessages = new ConcurrentSkipListSet<>();
			Set<String> receivedMessages = new HashSet<>();

			CountDownLatch sendLatch = new CountDownLatch(connectors);
			CountDownLatch receiveLatch = new CountDownLatch(connectors * messagesPerConnector);

			server.registerListener(message -> {
				receivedMessages.add(message);
				receiveLatch.countDown();
			});

			Set<RemoteInstance> instances = Collections.synchronizedSet(new HashSet<>());

			for (int i = 0; i < connectors; i++) {
				exec2.submit(() -> {
					try {
						final Optional<RemoteInstance> r = SingleInstanceManager.getRemoteInstance(appKey);
						assertTrue(r.isPresent());
						instances.add(r.get());

						for (int j = 0; j < messagesPerConnector; j++) {
							exec2.submit(() -> {
								try {
									for (;;) {
										final String message = UUID.randomUUID().toString();
										if (!sentMessages.add(message)) {
											continue;
										}
										r.get().sendMessage(message, 1000);
										break;
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
							});
						}

						sendLatch.countDown();
					} catch (Throwable e) {
						e.printStackTrace();
					}
				});
			}

			assertTrue(sendLatch.await(1, TimeUnit.MINUTES));

			exec2.shutdown();
			assertTrue(exec2.awaitTermination(1, TimeUnit.MINUTES));

			assertTrue(receiveLatch.await(1, TimeUnit.MINUTES));

			assertEquals(sentMessages, receivedMessages);

			for (RemoteInstance remoteInstance : instances) {
				try {
					remoteInstance.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} finally {
			exec.shutdownNow();
			exec2.shutdownNow();
		}
	}
}

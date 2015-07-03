package org.cryptomator.webdav.jackrabbit;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ForkJoinTask;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.cryptomator.crypto.aes256.Aes256Cryptor;
import org.cryptomator.webdav.WebDavServer;
import org.cryptomator.webdav.WebDavServer.ServletLifeCycleAdapter;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.io.Files;

public class RangeRequestTest {

	private static final Aes256Cryptor CRYPTOR = new Aes256Cryptor();
	private static final WebDavServer SERVER = new WebDavServer();

	@BeforeClass
	public static void startServer() {
		SERVER.start();
	}

	@AfterClass
	public static void stopServer() {
		SERVER.stop();
	}

	@Test
	public void testAsyncRangeRequests() throws IOException, URISyntaxException {
		final File tmpVault = Files.createTempDir();
		final ServletLifeCycleAdapter servlet = SERVER.createServlet(tmpVault.toPath(), CRYPTOR, new ArrayList<String>(), new ArrayList<String>(), "JUnitTestVault");
		final boolean started = servlet.start();
		final URI vaultBaseUri = new URI("http", servlet.getServletUri().getSchemeSpecificPart() + "/", null);
		final URL testResourceUrl = new URL(vaultBaseUri.toURL(), "testfile.txt");

		Assert.assertTrue(started);
		Assert.assertNotNull(vaultBaseUri);

		// prepare 8MiB test data:
		final byte[] plaintextData = new byte[2097152 * Integer.BYTES];
		final ByteBuffer bbIn = ByteBuffer.wrap(plaintextData);
		for (int i = 0; i < 2097152; i++) {
			bbIn.putInt(i);
		}
		final InputStream plaintextIn = new ByteArrayInputStream(plaintextData);

		// put request:
		final HttpURLConnection putConn = (HttpURLConnection) testResourceUrl.openConnection();
		putConn.setDoOutput(true);
		putConn.setRequestMethod("PUT");
		IOUtils.copy(plaintextIn, putConn.getOutputStream());
		putConn.getOutputStream().close();
		final int putResponse = putConn.getResponseCode();
		putConn.disconnect();
		Assert.assertEquals(201, putResponse);

		// multiple async range requests:
		final Collection<ForkJoinTask<?>> tasks = new ArrayList<>();
		final Random generator = new Random(System.currentTimeMillis());
		for (int i = 0; i < 100; i++) {
			final int pos1 = generator.nextInt(plaintextData.length);
			final int pos2 = generator.nextInt(plaintextData.length);
			final ForkJoinTask<?> task = ForkJoinTask.adapt(() -> {
				try {
					final HttpURLConnection conn = (HttpURLConnection) testResourceUrl.openConnection();
					final int lower = Math.min(pos1, pos2);
					final int upper = Math.max(pos1, pos2);
					conn.setRequestMethod("GET");
					conn.addRequestProperty("Range", "bytes=" + lower + "-" + upper);
					final int rangeResponse = conn.getResponseCode();
					final byte[] buffer = new byte[upper - lower + 1];
					final int bytesReceived = IOUtils.read(conn.getInputStream(), buffer);
					Assert.assertEquals(206, rangeResponse);
					Assert.assertEquals(buffer.length, bytesReceived);
					Assert.assertArrayEquals(Arrays.copyOfRange(plaintextData, lower, upper + 1), buffer);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}).fork();
			tasks.add(task);
		}

		for (ForkJoinTask<?> task : tasks) {
			task.join();
		}

		servlet.stop();

		FileUtils.deleteQuietly(tmpVault);
	}

}

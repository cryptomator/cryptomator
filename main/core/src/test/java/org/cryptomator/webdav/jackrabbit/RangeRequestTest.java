package org.cryptomator.webdav.jackrabbit;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.cryptomator.crypto.Cryptor;
import org.cryptomator.webdav.WebDavServer;
import org.cryptomator.webdav.WebDavServer.ServletLifeCycleAdapter;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

public class RangeRequestTest {

	private static final Logger LOG = LoggerFactory.getLogger(RangeRequestTest.class);
	private static final Cryptor CRYPTOR = new CryptorMock();
	private static final WebDavServer SERVER = new WebDavServer();
	private static final File TMP_VAULT = Files.createTempDir();
	private static ServletLifeCycleAdapter SERVLET;
	private static URI VAULT_BASE_URI;

	@BeforeClass
	public static void startServer() throws URISyntaxException {
		CRYPTOR.randomizeMasterKey();
		SERVER.start();
		SERVLET = SERVER.createServlet(TMP_VAULT.toPath(), CRYPTOR, new ArrayList<String>(), new ArrayList<String>(), "JUnitTestVault");
		SERVLET.start();
		VAULT_BASE_URI = new URI("http", SERVLET.getServletUri().getSchemeSpecificPart() + "/", null);
		Assert.assertTrue(SERVLET.isRunning());
		Assert.assertNotNull(VAULT_BASE_URI);
	}

	@AfterClass
	public static void stopServer() {
		SERVLET.stop();
		SERVER.stop();
		FileUtils.deleteQuietly(TMP_VAULT);
	}

	@Test
	public void testFullFileDecryption() throws IOException, URISyntaxException {
		final URL testResourceUrl = new URL(VAULT_BASE_URI.toURL(), "fullFileDecryptionTestFile.txt");
		final HttpClient client = new HttpClient();

		// prepare 64MiB test data:
		final byte[] plaintextData = new byte[16777216 * Integer.BYTES];
		final ByteBuffer bbIn = ByteBuffer.wrap(plaintextData);
		for (int i = 0; i < 16777216; i++) {
			bbIn.putInt(i);
		}
		final InputStream plaintextDataInputStream = new ByteArrayInputStream(plaintextData);

		// put request:
		final EntityEnclosingMethod putMethod = new PutMethod(testResourceUrl.toString());
		putMethod.setRequestEntity(new ByteArrayRequestEntity(plaintextData));
		final int putResponse = client.executeMethod(putMethod);
		putMethod.releaseConnection();
		Assert.assertEquals(201, putResponse);

		// get request:
		final HttpMethod getMethod = new GetMethod(testResourceUrl.toString());
		final int statusCode = client.executeMethod(getMethod);
		Assert.assertEquals(200, statusCode);
		// final byte[] received = new byte[plaintextData.length];
		// IOUtils.read(getMethod.getResponseBodyAsStream(), received);
		// Assert.assertArrayEquals(plaintextData, received);
		Assert.assertTrue(IOUtils.contentEquals(plaintextDataInputStream, getMethod.getResponseBodyAsStream()));
		getMethod.releaseConnection();
	}

	@Test
	public void testAsyncRangeRequests() throws IOException, URISyntaxException, InterruptedException {
		final URL testResourceUrl = new URL(VAULT_BASE_URI.toURL(), "asyncRangeRequestTestFile.txt");

		final MultiThreadedHttpConnectionManager cm = new MultiThreadedHttpConnectionManager();
		cm.getParams().setDefaultMaxConnectionsPerHost(50);
		final HttpClient client = new HttpClient(cm);

		// prepare 8MiB test data:
		final byte[] plaintextData = new byte[2097152 * Integer.BYTES];
		final ByteBuffer bbIn = ByteBuffer.wrap(plaintextData);
		for (int i = 0; i < 2097152; i++) {
			bbIn.putInt(i);
		}

		// put request:
		final EntityEnclosingMethod putMethod = new PutMethod(testResourceUrl.toString());
		putMethod.setRequestEntity(new ByteArrayRequestEntity(plaintextData));
		final int putResponse = client.executeMethod(putMethod);
		putMethod.releaseConnection();
		Assert.assertEquals(201, putResponse);

		// multiple async range requests:
		final List<ForkJoinTask<?>> tasks = new ArrayList<>();
		final Random generator = new Random(System.currentTimeMillis());

		final AtomicBoolean success = new AtomicBoolean(true);

		// 10 full interrupted requests:
		for (int i = 0; i < 10; i++) {
			final ForkJoinTask<?> task = ForkJoinTask.adapt(() -> {
				try {
					final HttpMethod getMethod = new GetMethod(testResourceUrl.toString());
					final int statusCode = client.executeMethod(getMethod);
					if (statusCode != 200) {
						LOG.error("Invalid status code for interrupted full request");
						success.set(false);
					}
					getMethod.getResponseBodyAsStream().read();
					getMethod.getResponseBodyAsStream().close();
					getMethod.releaseConnection();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			tasks.add(task);
		}

		// 50 crappy interrupted range requests:
		for (int i = 0; i < 50; i++) {
			final int lower = generator.nextInt(plaintextData.length);
			final ForkJoinTask<?> task = ForkJoinTask.adapt(() -> {
				try {
					final HttpMethod getMethod = new GetMethod(testResourceUrl.toString());
					getMethod.addRequestHeader("Range", "bytes=" + lower + "-");
					final int statusCode = client.executeMethod(getMethod);
					if (statusCode != 206) {
						LOG.error("Invalid status code for interrupted range request");
						success.set(false);
					}
					getMethod.getResponseBodyAsStream().read();
					getMethod.getResponseBodyAsStream().close();
					getMethod.releaseConnection();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			tasks.add(task);
		}

		// 50 normal open range requests:
		for (int i = 0; i < 50; i++) {
			final int lower = generator.nextInt(plaintextData.length - 512);
			final int upper = plaintextData.length - 1;
			final ForkJoinTask<?> task = ForkJoinTask.adapt(() -> {
				try {
					final HttpMethod getMethod = new GetMethod(testResourceUrl.toString());
					getMethod.addRequestHeader("Range", "bytes=" + lower + "-");
					final byte[] expected = Arrays.copyOfRange(plaintextData, lower, upper + 1);
					final int statusCode = client.executeMethod(getMethod);
					final byte[] responseBody = new byte[upper - lower + 10];
					final int bytesRead = IOUtils.read(getMethod.getResponseBodyAsStream(), responseBody);
					getMethod.releaseConnection();
					if (statusCode != 206) {
						LOG.error("Invalid status code for open range request");
						success.set(false);
					} else if (upper - lower + 1 != bytesRead) {
						LOG.error("Invalid response length for open range request");
						success.set(false);
					} else if (!Arrays.equals(expected, Arrays.copyOfRange(responseBody, 0, bytesRead))) {
						LOG.error("Invalid response body for open range request");
						success.set(false);
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			tasks.add(task);
		}

		// 200 normal closed range requests:
		for (int i = 0; i < 200; i++) {
			final int pos1 = generator.nextInt(plaintextData.length - 512);
			final int pos2 = pos1 + 512;
			final ForkJoinTask<?> task = ForkJoinTask.adapt(() -> {
				try {
					final int lower = Math.min(pos1, pos2);
					final int upper = Math.max(pos1, pos2);
					final HttpMethod getMethod = new GetMethod(testResourceUrl.toString());
					getMethod.addRequestHeader("Range", "bytes=" + lower + "-" + upper);
					final byte[] expected = Arrays.copyOfRange(plaintextData, lower, upper + 1);
					final int statusCode = client.executeMethod(getMethod);
					final byte[] responseBody = new byte[upper - lower + 1];
					final int bytesRead = IOUtils.read(getMethod.getResponseBodyAsStream(), responseBody);
					getMethod.releaseConnection();
					if (statusCode != 206) {
						LOG.error("Invalid status code for closed range request");
						success.set(false);
					} else if (upper - lower + 1 != bytesRead) {
						LOG.error("Invalid response length for closed range request");
						success.set(false);
					} else if (!Arrays.equals(expected, Arrays.copyOfRange(responseBody, 0, bytesRead))) {
						LOG.error("Invalid response body for closed range request");
						success.set(false);
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			tasks.add(task);
		}

		Collections.shuffle(tasks, generator);

		final ForkJoinPool pool = new ForkJoinPool(4);
		for (ForkJoinTask<?> task : tasks) {
			pool.execute(task);
		}
		for (ForkJoinTask<?> task : tasks) {
			task.join();
		}
		pool.shutdown();
		cm.shutdown();

		Assert.assertTrue(success.get());
	}

	@Test
	public void testUnsatisfiableRangeRequest() throws IOException, URISyntaxException {
		final URL testResourceUrl = new URL(VAULT_BASE_URI.toURL(), "unsatisfiableRangeRequestTestFile.txt");
		final HttpClient client = new HttpClient();

		// prepare file content:
		final byte[] fileContent = "This is some test file content.".getBytes();

		// put request:
		final EntityEnclosingMethod putMethod = new PutMethod(testResourceUrl.toString());
		putMethod.setRequestEntity(new ByteArrayRequestEntity(fileContent));
		final int putResponse = client.executeMethod(putMethod);
		putMethod.releaseConnection();
		Assert.assertEquals(201, putResponse);

		// get request:
		final HttpMethod getMethod = new GetMethod(testResourceUrl.toString());
		getMethod.addRequestHeader("Range", "chunks=1-2");
		final int getResponse = client.executeMethod(getMethod);
		final byte[] response = new byte[fileContent.length];
		IOUtils.read(getMethod.getResponseBodyAsStream(), response);
		getMethod.releaseConnection();
		Assert.assertEquals(416, getResponse);
		Assert.assertArrayEquals(fileContent, response);
	}

}

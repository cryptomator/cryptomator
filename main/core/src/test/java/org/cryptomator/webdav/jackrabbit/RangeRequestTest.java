package org.cryptomator.webdav.jackrabbit;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ForkJoinTask;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
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
	private static final File TMP_VAULT = Files.createTempDir();
	private static ServletLifeCycleAdapter SERVLET;
	private static URI VAULT_BASE_URI;

	@BeforeClass
	public static void startServer() throws URISyntaxException {
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
	public void testAsyncRangeRequests() throws IOException, URISyntaxException {
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
		final Collection<ForkJoinTask<?>> tasks = new ArrayList<>();
		final Random generator = new Random(System.currentTimeMillis());
		for (int i = 0; i < 200; i++) {
			final int pos1 = generator.nextInt(plaintextData.length);
			final int pos2 = generator.nextInt(plaintextData.length);
			final ForkJoinTask<?> task = ForkJoinTask.adapt(() -> {
				try {
					final int lower = Math.min(pos1, pos2);
					final int upper = Math.max(pos1, pos2);
					final HttpMethod getMethod = new GetMethod(testResourceUrl.toString());
					getMethod.addRequestHeader("Range", "bytes=" + lower + "-" + upper);
					final int statusCode = client.executeMethod(getMethod);
					final byte[] responseBody = new byte[upper - lower + 1];
					IOUtils.read(getMethod.getResponseBodyAsStream(), responseBody);
					getMethod.releaseConnection();
					Assert.assertEquals(206, statusCode);
					Assert.assertArrayEquals(Arrays.copyOfRange(plaintextData, lower, upper + 1), responseBody);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}).fork();
			tasks.add(task);
		}

		for (ForkJoinTask<?> task : tasks) {
			task.join();
		}

		cm.shutdown();
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
		final byte[] response = getMethod.getResponseBody();
		getMethod.releaseConnection();
		Assert.assertEquals(416, getResponse);
		Assert.assertArrayEquals(fileContent, response);
	}

}

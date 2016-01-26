package org.cryptomator.frontend.webdav;

import static org.hamcrest.collection.IsArrayContaining.hasItemInArray;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.filesystem.inmem.InMemoryFileSystem;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class WebDavServerTest {

	private static final WebDavServer SERVER = DaggerWebDavComponent.create().server();
	private String servletRoot;
	private FileSystem fs;
	private ServletContextHandler servlet;

	@BeforeClass
	public static void startServer() throws URISyntaxException {
		SERVER.start();
	}

	@AfterClass
	public static void stopServer() {
		SERVER.stop();
	}

	@Before
	public void startServlet() throws Exception {
		fs = new InMemoryFileSystem();
		servlet = SERVER.addServlet(fs, URI.create("http://localhost:" + SERVER.getPort() + "/test"));
		servlet.start();
		servletRoot = "http://localhost:" + SERVER.getPort() + "/test";
	}

	@After
	public void stopServlet() throws Exception {
		servlet.stop();
	}

	@Test
	public void testGet() throws HttpException, IOException {
		final HttpClient client = new HttpClient();

		// write test content:
		final byte[] testContent = "hello world".getBytes();
		try (WritableFile w = fs.file("foo.txt").openWritable()) {
			w.write(ByteBuffer.wrap(testContent));
		}

		// check get response body:
		final HttpMethod getMethod = new GetMethod(servletRoot + "/foo.txt");
		final int statusCode = client.executeMethod(getMethod);
		Assert.assertEquals(200, statusCode);
		Assert.assertThat(getMethod.getResponseHeaders(), hasItemInArray(new Header("Accept-Ranges", "bytes")));
		Assert.assertTrue(IOUtils.contentEquals(new ByteArrayInputStream(testContent), getMethod.getResponseBodyAsStream()));
		getMethod.releaseConnection();
	}

	@Test
	public void testMkcol() throws HttpException, IOException {
		final HttpClient client = new HttpClient();

		// create directory:
		final HttpMethod mkcolMethod = new MkColMethod(servletRoot + "/foo");
		final int statusCode = client.executeMethod(mkcolMethod);
		Assert.assertEquals(201, statusCode);
		Assert.assertTrue(fs.folder("foo").exists());

		mkcolMethod.releaseConnection();
	}

	@Test
	public void testPut() throws HttpException, IOException {
		final HttpClient client = new HttpClient();

		// create file:
		final byte[] testContent = "hello world".getBytes();
		final EntityEnclosingMethod putMethod = new PutMethod(servletRoot + "/foo.txt");
		putMethod.setRequestEntity(new ByteArrayRequestEntity(testContent));
		final int putResponse = client.executeMethod(putMethod);
		Assert.assertEquals(201, putResponse);
		Assert.assertTrue(fs.file("foo.txt").exists());

		// check file contents:
		ByteBuffer buf = ByteBuffer.allocate(testContent.length);
		try (ReadableFile r = fs.file("foo.txt").openReadable()) {
			r.read(buf);
		}
		Assert.assertArrayEquals(testContent, buf.array());

		putMethod.releaseConnection();
	}

}

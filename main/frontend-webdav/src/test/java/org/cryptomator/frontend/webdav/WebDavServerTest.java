/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav;

import static org.hamcrest.collection.IsArrayContaining.hasItemInArray;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.CopyMethod;
import org.apache.jackrabbit.webdav.client.methods.DavMethodBase;
import org.apache.jackrabbit.webdav.client.methods.LockMethod;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;
import org.apache.jackrabbit.webdav.client.methods.MoveMethod;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.lock.LockInfo;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.Type;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class WebDavServerTest {

	private static final WebDavServer SERVER = DaggerWebDavComponent.create().server();
	private static final Logger LOG = LoggerFactory.getLogger(WebDavServerTest.class);
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
		servletRoot = "http://localhost:" + SERVER.getPort() + "/test";
		servlet = SERVER.addServlet(fs, URI.create(servletRoot));
		servlet.start();
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

	/* PROPFIND */

	@Test
	public void testPropfind() throws HttpException, IOException, ParserConfigurationException, SAXException, XPathException, DavException {
		final HttpClient client = new HttpClient();

		fs.folder("folder1").create();
		fs.folder("folder2").create();
		try (WritableFile w = fs.file("file1").openWritable()) {
			w.write(ByteBuffer.allocate(0));
		}

		// get directory listing of /:
		final DavMethodBase propfindMethod = new PropFindMethod(servletRoot + "/");
		final int statusCode = client.executeMethod(propfindMethod);
		Assert.assertEquals(207, statusCode);

		// get hrefs contained in dirlisting response:
		MultiStatus ms = propfindMethod.getResponseBodyAsMultiStatus();
		propfindMethod.releaseConnection();
		Collection<String> hrefs = Arrays.asList(ms.getResponses()).stream().map(MultiStatusResponse::getHref).collect(Collectors.toSet());

		Assert.assertTrue(CollectionUtils.containsAll(hrefs, Arrays.asList(servletRoot + "/folder1/", servletRoot + "/folder2/", servletRoot + "/file1")));
	}

	/* LOCK */

	@Test
	public void testLockExisting() throws IOException, DavException {
		final HttpClient client = new HttpClient();

		// create file:
		try (WritableFile writable = fs.file("foo.txt").openWritable()) {
			writable.write(ByteBuffer.allocate(0));
		}

		// lock existing file:
		LockInfo lockInfo = new LockInfo(Scope.EXCLUSIVE, Type.WRITE, "le me", 3600, false);
		final DavMethodBase lockMethod = new LockMethod(servletRoot + "/foo.txt", lockInfo);
		final int lockResponse = client.executeMethod(lockMethod);
		Assert.assertEquals(200, lockResponse);

		lockMethod.releaseConnection();
	}

	@Test
	public void testLockNonExisting() throws IOException, DavException {
		final HttpClient client = new HttpClient();

		// lock nonexisting file:
		LockInfo lockInfo = new LockInfo(Scope.EXCLUSIVE, Type.WRITE, "le me", 3600, false);
		final DavMethodBase lockMethod = new LockMethod(servletRoot + "/foo.txt", lockInfo);
		final int lockResponse = client.executeMethod(lockMethod);
		Assert.assertEquals(201, lockResponse);
		Assert.assertTrue(fs.file("foo.txt").exists());

		lockMethod.releaseConnection();
	}

	/* MOVE */

	@Test
	public void testMoveFolder() throws HttpException, IOException, ParserConfigurationException, SAXException, XPathException, DavException {
		final HttpClient client = new HttpClient();

		fs.folder("srcFolder").create();

		final DavMethodBase moveMethod = new MoveMethod(servletRoot + "/srcFolder", servletRoot + "/dstFolder", false);
		client.executeMethod(moveMethod);
		Assert.assertTrue(moveMethod.succeeded());
		moveMethod.releaseConnection();

		Assert.assertFalse(fs.folder("srcFolder").exists());
		Assert.assertTrue(fs.folder("dstFolder").exists());
	}

	@Test
	public void testMoveFolderOverwrite() throws HttpException, IOException, ParserConfigurationException, SAXException, XPathException, DavException {
		final HttpClient client = new HttpClient();

		fs.folder("srcFolder").create();
		try (WritableFile w = fs.file("dstFolder").openWritable()) {
			w.write(ByteBuffer.allocate(0));
		}

		final DavMethodBase moveMethod = new MoveMethod(servletRoot + "/srcFolder", servletRoot + "/dstFolder", true);
		client.executeMethod(moveMethod);
		Assert.assertTrue(moveMethod.succeeded());
		moveMethod.releaseConnection();

		Assert.assertFalse(fs.folder("srcFolder").exists());
		Assert.assertTrue(fs.folder("dstFolder").exists());
		Assert.assertFalse(fs.file("dstFolder").exists());
	}

	@Test
	public void testMoveFile() throws HttpException, IOException, ParserConfigurationException, SAXException, XPathException, DavException {
		final HttpClient client = new HttpClient();

		try (WritableFile w = fs.file("srcFile").openWritable()) {
			w.write(ByteBuffer.allocate(0));
		}

		final DavMethodBase moveMethod = new MoveMethod(servletRoot + "/srcFile/", servletRoot + "/dstFile/", false);
		client.executeMethod(moveMethod);
		Assert.assertTrue(moveMethod.succeeded());
		moveMethod.releaseConnection();

		Assert.assertFalse(fs.file("srcFile").exists());
		Assert.assertTrue(fs.file("dstFile").exists());
	}

	@Test
	public void testMoveFileOverwrite() throws HttpException, IOException, ParserConfigurationException, SAXException, XPathException, DavException {
		final HttpClient client = new HttpClient();

		try (WritableFile w = fs.file("srcFile").openWritable()) {
			w.write(ByteBuffer.allocate(0));
		}
		fs.folder("dstFile").create();

		final DavMethodBase moveMethod = new MoveMethod(servletRoot + "/srcFile/", servletRoot + "/dstFile/", true);
		client.executeMethod(moveMethod);
		Assert.assertTrue(moveMethod.succeeded());
		moveMethod.releaseConnection();

		Assert.assertFalse(fs.file("srcFile").exists());
		Assert.assertTrue(fs.file("dstFile").exists());
		Assert.assertFalse(fs.folder("dstFile").exists());
	}

	/* COPY */

	@Test
	public void testCopyFolder() throws HttpException, IOException, ParserConfigurationException, SAXException, XPathException, DavException {
		final HttpClient client = new HttpClient();

		fs.folder("srcFolder").folder("sub").create();
		try (WritableFile w = fs.folder("srcFolder").file("file").openWritable()) {
			w.write(ByteBuffer.allocate(0));
		}

		final DavMethodBase copyMethod = new CopyMethod(servletRoot + "/srcFolder", servletRoot + "/dstFolder", false);
		client.executeMethod(copyMethod);
		Assert.assertTrue(copyMethod.succeeded());
		copyMethod.releaseConnection();

		Assert.assertTrue(fs.folder("srcFolder").folder("sub").exists());
		Assert.assertTrue(fs.folder("srcFolder").file("file").exists());
		Assert.assertTrue(fs.folder("dstFolder").folder("sub").exists());
		Assert.assertTrue(fs.folder("dstFolder").file("file").exists());
	}

	@Test
	public void testCopyFolderOverwrite() throws HttpException, IOException, ParserConfigurationException, SAXException, XPathException, DavException {
		final HttpClient client = new HttpClient();

		fs.folder("srcFolder").create();
		try (WritableFile w = fs.file("dstFolder").openWritable()) {
			w.write(ByteBuffer.allocate(0));
		}

		final DavMethodBase copyMethod = new CopyMethod(servletRoot + "/srcFolder", servletRoot + "/dstFolder/", true);
		client.executeMethod(copyMethod);
		Assert.assertTrue(copyMethod.succeeded());
		copyMethod.releaseConnection();

		Assert.assertTrue(fs.folder("srcFolder").exists());
		Assert.assertTrue(fs.folder("dstFolder").exists());
		Assert.assertFalse(fs.file("dstFolder").exists());
	}

	@Test
	public void testCopyFile() throws HttpException, IOException, ParserConfigurationException, SAXException, XPathException, DavException {
		final HttpClient client = new HttpClient();

		try (WritableFile w = fs.file("srcFile").openWritable()) {
			w.write(ByteBuffer.allocate(0));
		}

		final DavMethodBase copyMethod = new CopyMethod(servletRoot + "/srcFile/", servletRoot + "/dstFile/", false);
		client.executeMethod(copyMethod);
		Assert.assertTrue(copyMethod.succeeded());
		copyMethod.releaseConnection();

		Assert.assertTrue(fs.file("srcFile").exists());
		Assert.assertTrue(fs.file("dstFile").exists());
	}

	@Test
	public void testCopyFileOverwrite() throws HttpException, IOException, ParserConfigurationException, SAXException, XPathException, DavException {
		final HttpClient client = new HttpClient();

		try (WritableFile w = fs.file("srcFile").openWritable()) {
			w.write(ByteBuffer.allocate(0));
		}
		fs.folder("dstFile").create();

		final DavMethodBase copyMethod = new CopyMethod(servletRoot + "/srcFile/", servletRoot + "/dstFile/", true);
		client.executeMethod(copyMethod);
		Assert.assertTrue(copyMethod.succeeded());
		copyMethod.releaseConnection();

		Assert.assertTrue(fs.file("srcFile").exists());
		Assert.assertTrue(fs.file("dstFile").exists());
		Assert.assertFalse(fs.folder("dstFile").exists());
	}

	/* Range requests */

	@Test
	public void testGetWithUnsatisfiableRange() throws IOException {
		final HttpClient client = new HttpClient();

		// write test content:
		final byte[] testContent = "hello world".getBytes();
		try (WritableFile w = fs.file("foo.txt").openWritable()) {
			w.write(ByteBuffer.wrap(testContent));
		}

		// check get response body:
		final HttpMethod getMethod = new GetMethod(servletRoot + "/foo.txt");
		getMethod.addRequestHeader("Range", "chunks=1-2");
		final int statusCode = client.executeMethod(getMethod);
		Assert.assertEquals(416, statusCode);
		Assert.assertArrayEquals(testContent, getMethod.getResponseBody());
		getMethod.releaseConnection();
	}

	@Test
	public void testMultipleGetWithRangeAsync() throws IOException, URISyntaxException, InterruptedException {
		final String testResourceUrl = servletRoot + "/foo.txt";

		// prepare 8MiB test data:
		final byte[] plaintextData = new byte[2097152 * Integer.BYTES];
		final ByteBuffer plaintextDataByteBuffer = ByteBuffer.wrap(plaintextData);
		for (int i = 0; i < 2097152; i++) {
			plaintextDataByteBuffer.putInt(i);
		}
		try (WritableFile w = fs.file("foo.txt").openWritable()) {
			plaintextDataByteBuffer.flip();
			w.write(plaintextDataByteBuffer);
		}

		final MultiThreadedHttpConnectionManager cm = new MultiThreadedHttpConnectionManager();
		cm.getParams().setDefaultMaxConnectionsPerHost(50);
		final HttpClient client = new HttpClient(cm);

		// multiple async range requests:
		final List<ForkJoinTask<?>> tasks = new ArrayList<>();
		final Random generator = new Random(System.currentTimeMillis());

		final AtomicBoolean success = new AtomicBoolean(true);

		// 10 full interrupted requests:
		for (int i = 0; i < 10; i++) {
			final ForkJoinTask<?> task = ForkJoinTask.adapt(() -> {
				try {
					final HttpMethod getMethod = new GetMethod(testResourceUrl);
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
					final HttpMethod getMethod = new GetMethod(testResourceUrl);
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
					final HttpMethod getMethod = new GetMethod(testResourceUrl);
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
					final HttpMethod getMethod = new GetMethod(testResourceUrl);
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

}

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
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
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
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;
import org.apache.jackrabbit.webdav.client.methods.MoveMethod;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
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
import org.xml.sax.SAXException;

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

}

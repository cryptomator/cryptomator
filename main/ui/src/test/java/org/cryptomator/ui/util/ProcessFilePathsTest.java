package org.cryptomator.ui.util;

import org.junit.Assert;
import org.junit.Test;

public class ProcessFilePathsTest {

	@Test
	public void testSavedFilePathAtVaultCreation() {
		String uncBase = "\\\\server";
		String uncSSL = "@SSL";
		String port = "@1234";
		String path = "\\@test@Dir\\test@File.test";
		String driveLetter = "C:";
		String uncWithSSL = uncBase.concat(uncSSL);
		String uncWithPort = uncBase.concat(port);
		String uncWithPath = uncBase.concat(path);
		String uncWithSSLAndPath = uncWithSSL.concat(path);
		String uncWithPortAndPath = uncWithPort.concat(path);
		String uncWithSSLAndPort = uncWithSSL.concat(port);
		String uncWithSSLAndPortAndPath = uncWithSSLAndPort.concat(path);
		String pathWithDriveLetter = driveLetter.concat(path);
		String uriWithUserinfo = "file:\\\\userinfo@server\\test@dir";

		Assert.assertEquals(uncBase, ProcessFilePath.processFilePath(uncBase));
		Assert.assertEquals(path, ProcessFilePath.processFilePath(path));
		Assert.assertEquals(uncBase, ProcessFilePath.processFilePath(uncWithSSL));
		Assert.assertEquals(uncBase, ProcessFilePath.processFilePath(uncWithPort));
		Assert.assertEquals(uncBase, ProcessFilePath.processFilePath(uncWithSSLAndPort));
		Assert.assertEquals(uncWithPath, ProcessFilePath.processFilePath(uncWithPath));
		Assert.assertEquals(uncWithPath, ProcessFilePath.processFilePath(uncWithSSLAndPath));
		Assert.assertEquals(uncWithPath, ProcessFilePath.processFilePath(uncWithPortAndPath));
		Assert.assertEquals(uncWithPath, ProcessFilePath.processFilePath(uncWithSSLAndPortAndPath));
		Assert.assertEquals(pathWithDriveLetter, ProcessFilePath.processFilePath(pathWithDriveLetter));
		Assert.assertEquals(uriWithUserinfo, ProcessFilePath.processFilePath(uriWithUserinfo));
	}
}

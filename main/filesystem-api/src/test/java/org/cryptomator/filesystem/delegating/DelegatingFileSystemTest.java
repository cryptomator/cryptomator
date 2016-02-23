package org.cryptomator.filesystem.delegating;

import java.util.Optional;

import org.cryptomator.filesystem.FileSystem;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class DelegatingFileSystemTest {

	@Test
	public void testQuotaAvailableBytes() {
		FileSystem mockFs = Mockito.mock(FileSystem.class);
		Mockito.when(mockFs.fileSystem()).thenReturn(mockFs);
		Mockito.when(mockFs.quotaAvailableBytes()).thenReturn(Optional.of(42l));

		DelegatingFileSystem delegatingFs = TestDelegatingFileSystem.withRoot(mockFs);
		Assert.assertEquals(mockFs.quotaAvailableBytes(), delegatingFs.quotaAvailableBytes());
	}

	@Test
	public void testQuotaUsedBytes() {
		FileSystem mockFs = Mockito.mock(FileSystem.class);
		Mockito.when(mockFs.fileSystem()).thenReturn(mockFs);
		Mockito.when(mockFs.quotaUsedBytes()).thenReturn(Optional.of(23l));

		DelegatingFileSystem delegatingFs = TestDelegatingFileSystem.withRoot(mockFs);
		Assert.assertEquals(mockFs.quotaUsedBytes(), delegatingFs.quotaUsedBytes());
	}

}

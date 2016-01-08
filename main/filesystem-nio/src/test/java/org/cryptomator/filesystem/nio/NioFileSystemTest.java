package org.cryptomator.filesystem.nio;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.cryptomator.filesystem.FileSystemFeature;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NioFileSystemTest {

	private Path path;

	private InstanceFactory instanceFactory;

	private NioAccess nioAccess;

	private NioFileSystem inTest;

	@Before
	public void setUp() {
		path = mock(Path.class);
		instanceFactory = mock(InstanceFactory.class);
		nioAccess = mock(NioAccess.class);

		when(path.toAbsolutePath()).thenReturn(path);

		InstanceFactory.DEFAULT.set(instanceFactory);
		NioAccess.DEFAULT.set(nioAccess);

		inTest = NioFileSystem.rootedAt(path);
	}

	@Test
	public void testRootedAtCreatesNioFileSystemWithPath() {
		assertThat(inTest.instanceFactory, is(instanceFactory));
		assertThat(inTest.path, is(path));
		assertThat(inTest.nioAccess, is(nioAccess));
		assertThat(inTest.parent, is(Optional.empty()));
	}

	@Test
	public void testRootedAtCreatesFolder() throws IOException {
		verify(nioAccess).createDirectories(path);
	}

	@Test
	public void testSupportsCreationTimeDelegatesToNioAccessWithTrue() {
		when(nioAccess.supportsCreationTime(path)).thenReturn(true);

		boolean result = inTest.supports(FileSystemFeature.CREATION_TIME_FEATURE);

		assertThat(result, is(true));
	}

	@Test
	public void testSupportsWithOtherFeatureReturnsFalse() {
		boolean result = inTest.supports(null);

		assertThat(result, is(false));
	}

	@Test
	public void testSupportsCreationTimeDelegatesToNioAccessWithFalse() {
		when(nioAccess.supportsCreationTime(path)).thenReturn(false);

		boolean result = inTest.supports(FileSystemFeature.CREATION_TIME_FEATURE);

		assertThat(result, is(false));
	}

	@After
	public void tearDown() {
		InstanceFactory.DEFAULT.reset();
		NioAccess.DEFAULT.reset();
	}

}
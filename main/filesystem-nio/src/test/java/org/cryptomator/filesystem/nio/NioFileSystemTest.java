package org.cryptomator.filesystem.nio;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

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

	@After
	public void tearDown() {
		InstanceFactory.DEFAULT.reset();
		NioAccess.DEFAULT.reset();
	}

}
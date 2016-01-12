package org.cryptomator.filesystem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class PathResolverTest {

	private final Folder root = Mockito.mock(Folder.class);
	private final Folder foo = Mockito.mock(Folder.class);
	private final Folder bar = Mockito.mock(Folder.class);
	private final File baz = Mockito.mock(File.class);

	@Before
	public void configureMocks() throws IOException {
		Mockito.doReturn(Optional.empty()).when(root).parent();
		Mockito.doReturn(Optional.of(root)).when(foo).parent();
		Mockito.doReturn(Optional.of(foo)).when(bar).parent();

		Mockito.doReturn(foo).when(root).folder("foo");
		Mockito.doReturn(bar).when(foo).folder("bar");
		Mockito.doReturn(baz).when(bar).file("baz");
	}

	@Test
	public void testResolveSameFolder() {
		Assert.assertEquals(foo, PathResolver.resolveFolder(foo, ""));
		Assert.assertEquals(foo, PathResolver.resolveFolder(foo, "/"));
		Assert.assertEquals(foo, PathResolver.resolveFolder(foo, "///"));
	}

	@Test
	public void testResolveChildFolder() {
		Assert.assertEquals(bar, PathResolver.resolveFolder(root, "foo/bar"));
		Assert.assertEquals(bar, PathResolver.resolveFolder(root, "foo/./bar"));
		Assert.assertEquals(bar, PathResolver.resolveFolder(root, "./foo/././bar"));
	}

	@Test
	public void testResolveParentFolder() {
		Assert.assertEquals(foo, PathResolver.resolveFolder(bar, ".."));
		Assert.assertEquals(root, PathResolver.resolveFolder(bar, "../.."));
	}

	@Test
	public void testResolveSiblingFolder() {
		Assert.assertEquals(foo, PathResolver.resolveFolder(bar, "../../foo"));
	}

	@Test(expected = UncheckedIOException.class)
	public void testResolveUnresolvableFolder() {
		PathResolver.resolveFolder(root, "..");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testResolveFileWithEmptyPath() {
		PathResolver.resolveFile(root, "");
	}

	@Test
	public void testResolveFile() {
		Assert.assertEquals(baz, PathResolver.resolveFile(foo, "../foo/bar/./baz"));
	}

}

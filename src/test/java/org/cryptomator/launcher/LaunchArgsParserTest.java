package org.cryptomator.launcher;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.net.URI;
import java.util.List;

public class LaunchArgsParserTest {

	private FileOpenRequestHandler fileOpenRequestHandler;
	private URIOpenRequestHandler uriOpenRequestHandler;
	private NoopRequestHandler noopRequestHandler;
	private LaunchArgsParser inTest;

	@BeforeEach
	public void setup() {
		fileOpenRequestHandler = Mockito.mock(FileOpenRequestHandler.class);
		uriOpenRequestHandler = Mockito.mock(URIOpenRequestHandler.class);
		noopRequestHandler = Mockito.mock(NoopRequestHandler.class);
		inTest = new LaunchArgsParser(fileOpenRequestHandler, uriOpenRequestHandler, noopRequestHandler);
	}

	@Test
	@DisplayName("only file paths are forwarded to the FileOpenRequestHandler")
	public void testOnlyPaths() {
		inTest.process(List.of("foo", "bar"));

		Mockito.verify(fileOpenRequestHandler).handleLaunchArgs(List.of("foo", "bar"));
		Mockito.verifyNoInteractions(uriOpenRequestHandler);
		Mockito.verifyNoInteractions(noopRequestHandler);
	}

	@Test
	@DisplayName("empty args are forwarded to the NoopRequestHandler")
	public void testEmptyArgs() {
		inTest.process(List.of());

		Mockito.verify(noopRequestHandler).revealApp();
		Mockito.verifyNoInteractions(uriOpenRequestHandler, fileOpenRequestHandler);
	}

	@Test
	@DisplayName("a Windows path is not mistaken for a URI")
	public void testWindowsPathIsNotAUri() {
		inTest.process(List.of("C:\\Users\\foo\\vault.cryptomator"));

		Mockito.verify(fileOpenRequestHandler).handleLaunchArgs(List.of("C:\\Users\\foo\\vault.cryptomator"));
		Mockito.verifyNoInteractions(uriOpenRequestHandler, noopRequestHandler);
	}

	@Test
	@DisplayName("a single cryptomator:// URI is forwarded to the URIOpenRequestHandler")
	public void testSingleUri() {
		inTest.process(List.of("cryptomator://vault/foo"));

		Mockito.verify(uriOpenRequestHandler).handleLaunchArgs(URI.create("cryptomator://vault/foo"));
		Mockito.verifyNoInteractions(fileOpenRequestHandler, noopRequestHandler);
	}

	@Test
	@DisplayName("a file:// URI is converted to a path and forwarded to the FileOpenRequestHandler")
	public void testFileUriIsTreatedAsPath() {
		inTest.process(List.of("file:///tmp/vault.cryptomator"));

		var captor = ArgumentCaptor.forClass(List.class);
		Mockito.verify(fileOpenRequestHandler).handleLaunchArgs(captor.capture());
		Mockito.verifyNoInteractions(uriOpenRequestHandler, noopRequestHandler);
		Assertions.assertEquals(1, captor.getValue().size());
		Assertions.assertFalse(captor.getValue().getFirst().toString().startsWith("file:"));
	}

	@Test
	@DisplayName("mixing a URI with a file path fails")
	public void testMixedUriAndPathFails() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> inTest.process(List.of("cryptomator://vault/foo", "bar")));
		Mockito.verifyNoInteractions(fileOpenRequestHandler, uriOpenRequestHandler, noopRequestHandler);
	}

	@Test
	@DisplayName("more than one URI fails")
	public void testMultipleUrisFail() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> inTest.process(List.of("cryptomator://vault/foo", "cryptomator://vault/bar")));
		Mockito.verifyNoInteractions(fileOpenRequestHandler, uriOpenRequestHandler, noopRequestHandler);
	}

	@Test
	@DisplayName("a URI that is not the first parameter fails")
	public void testUriNotFirstFails() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> inTest.process(List.of("foo", "cryptomator://vault/bar")));
		Mockito.verifyNoInteractions(fileOpenRequestHandler, uriOpenRequestHandler, noopRequestHandler);
	}

}

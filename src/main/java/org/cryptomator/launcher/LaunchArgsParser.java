package org.cryptomator.launcher;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Preprocesses the launch arguments and delegates them to the matching handler.
 * <p>
 * An argument is treated as a URI if it starts with a (non-{@code file}) scheme of at least two characters, e.g.
 * {@code cryptomator://…}. Everything else - including plain paths and {@code file://…} URIs - is treated as a file path
 * and forwarded to the {@link FileOpenRequestHandler}. The two-character minimum prevents Windows drive letters
 * (e.g. {@code C:\…}) from being misinterpreted as URIs.
 * <p>
 * URIs and file paths must not be mixed and at most a single URI is accepted, which has to be the first argument.
 */
@Singleton
class LaunchArgsParser {

	private static final Pattern SCHEME_PATTERN = Pattern.compile("^([a-zA-Z][a-zA-Z0-9+.-]+):.*");
	private static final String FILE_SCHEME = "file";

	private final FileOpenRequestHandler fileOpenRequestHandler;
	private final URIOpenRequestHandler uriOpenRequestHandler;
	private final NoopRequestHandler noopRequestHandler;

	@Inject
	public LaunchArgsParser(FileOpenRequestHandler fileOpenRequestHandler, URIOpenRequestHandler uriOpenRequestHandler, NoopRequestHandler noopRequestHandler) {
		this.fileOpenRequestHandler = fileOpenRequestHandler;
		this.uriOpenRequestHandler = uriOpenRequestHandler;
		this.noopRequestHandler = noopRequestHandler;
	}

	/**
	 * Classifies the given launch arguments and delegates them to the responsible handler.
	 *
	 * @param args the raw launch arguments
	 * @throws IllegalArgumentException if URIs and file paths are mixed, if more than one URI is given, if a URI is not
	 *                                  the first argument, or if a URI argument is malformed
	 */
	public void process(List<String> args) {
		if(args.isEmpty()) {
			noopRequestHandler.revealApp();
			return;
		}

		var classified = args.stream().map(LaunchArgsParser::classify).toList();
		var uris = classified.stream().filter(arg -> arg.kind() == Kind.URI).toList();

		if (uris.isEmpty()) {
			var paths = classified.stream().map(Arg::value).toList();
			fileOpenRequestHandler.handleLaunchArgs(paths);
			return;
		}

		if (uris.size() > 1) {
			throw new IllegalArgumentException("Only a single URI argument is accepted, but got " + uris.size() + ".");
		}
		if (classified.getFirst().kind() != Kind.URI) {
			throw new IllegalArgumentException("URI argument must be the first parameter.");
		}
		if (classified.size() > 1) {
			throw new IllegalArgumentException("Mixing a URI with file paths is not supported.");
		}
		uriOpenRequestHandler.handleLaunchArgs(URI.create(classified.getFirst().value()));
	}

	private static Arg classify(String arg) {
		var matcher = SCHEME_PATTERN.matcher(arg);
		if (!matcher.matches()) {
			return new Arg(Kind.PATH, arg);
		}
		var scheme = matcher.group(1);
		if (FILE_SCHEME.equalsIgnoreCase(scheme)) {
			// file:// URIs (e.g. passed by Linux file managers) are file paths in disguise
			return new Arg(Kind.PATH, Path.of(URI.create(arg)).toString());
		}
		return new Arg(Kind.URI, arg);
	}

	private enum Kind {PATH, URI}

	private record Arg(Kind kind, String value) {}

}

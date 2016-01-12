package org.cryptomator.filesystem;

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

final class PathResolver {

	private static final String DOT = ".";
	private static final String DOTDOT = "..";

	private PathResolver() {
	}

	/**
	 * Resolves a relative path (separated by '/') to a folder, e.g.
	 * <!-- @formatter:off -->
	 * <table>
	 * <thead>
	 * <tr>
	 * <th>dir</th>
	 * <th>path</th>
	 * <th>result</th>
	 * </tr>
	 * </thead>
	 * <tbody>
	 * <tr>
	 * <td>/foo/bar</td>
	 * <td>foo/bar</td>
	 * <td>/foo/bar/foo/bar</td>
	 * </tr>
	 * <tr>
	 * <td>/foo/bar</td>
	 * <td>../baz</td>
	 * <td>/foo/baz</td>
	 * </tr>
	 * <tr>
	 * <td>/foo/bar</td>
	 * <td>./foo/..</td>
	 * <td>/foo/bar</td>
	 * </tr>
	 * <tr>
	 * <td>/foo/bar</td>
	 * <td>/</td>
	 * <td>/foo/bar</td>
	 * </tr>
	 * <tr>
	 * <td>/foo/bar</td>
	 * <td></td>
	 * <td>/foo/bar</td>
	 * </tr>
	 * <tr>
	 * <td>/foo/bar</td>
	 * <td>../../..</td>
	 * <td>Exception</td>
	 * </tr>
	 * </tbody>
	 * </table>
	 * 
	 * @param dir The directory from which to resolve the path.
	 * @param relativePath The path relative to a given directory.
	 * @return The folder with the given path relative to the given dir.
	 */
	public static Folder resolveFolder(Folder dir, String relativePath) {
		final String[] fragments = StringUtils.split(relativePath, '/');
		if (ArrayUtils.isEmpty(fragments)) {
			return dir;
		}
		return resolveFolder(dir, Arrays.stream(fragments).iterator());
	}

	/**
	 * Resolves a relative path (separated by '/') to a file. Besides returning a File, this method is identical to {@link #resolveFile(Folder, String)}.
	 * 
	 * @param dir The directory from which to resolve the path.
	 * @param relativePath The path relative to a given directory.
	 * @return The file with the given path relative to the given dir.
	 * @throws IllegalArgumentException
	 *             if relativePath is empty, as this path would resolve to the directory itself, which obviously can't be a file.
	 */
	public static File resolveFile(Folder dir, String relativePath) {
		final String[] fragments = StringUtils.split(relativePath, '/');
		if (ArrayUtils.isEmpty(fragments)) {
			throw new IllegalArgumentException("Empty relativePath");
		}
		final Folder folder = resolveFolder(dir, Arrays.stream(fragments).limit(fragments.length - 1).iterator());
		final String filename = fragments[fragments.length - 1];
		return folder.file(filename);
	}

	private static Folder resolveFolder(Folder dir, Iterator<String> remainingPathFragments) {
		if (!remainingPathFragments.hasNext()) {
			return dir;
		}
		final String fragment = remainingPathFragments.next();
		assert fragment.length() > 0 : "iterator must not contain empty fragments";
		if (DOT.equals(fragment)) {
			return resolveFolder(dir, remainingPathFragments);
		} else if (DOTDOT.equals(fragment) && dir.parent().isPresent()) {
			return resolveFolder(dir.parent().get(), remainingPathFragments);
		} else if (DOTDOT.equals(fragment) && !dir.parent().isPresent()) {
			throw new UncheckedIOException(new FileNotFoundException("Unresolvable path"));
		} else {
			return resolveFolder(dir.folder(fragment), remainingPathFragments);
		}
	}

}

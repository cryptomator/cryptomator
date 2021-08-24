package org.cryptomator.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Singleton
public class PluginClassLoader extends URLClassLoader {

	private static final Logger LOG = LoggerFactory.getLogger(PluginClassLoader.class);
	private static final String NAME = "PluginClassLoader";
	private static final String JAR_SUFFIX = ".jar";

	@Inject
	public PluginClassLoader(Environment env) {
		super(NAME, env.getPluginDir().map(PluginClassLoader::findJars).orElse(new URL[0]), PluginClassLoader.class.getClassLoader());
	}

	private static URL[] findJars(Path path) {
		if (!Files.isDirectory(path)) {
			return new URL[0];
		} else {
			try {
				var visitor = new JarVisitor();
				Files.walkFileTree(path, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, visitor);
				return visitor.urls.toArray(URL[]::new);
			} catch (IOException e) {
				LOG.warn("Failed to scan plugin dir " + path, e);
				return new URL[0];
			}
		}
	}

	private static final class JarVisitor extends SimpleFileVisitor<Path> {

		private final List<URL> urls = new ArrayList<>();

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
			if (attrs.isRegularFile() && file.getFileName().toString().toLowerCase().endsWith(JAR_SUFFIX)) {
				try {
					urls.add(file.toUri().toURL());
				} catch (MalformedURLException e) {
					LOG.warn("Failed to create URL for jar file {}", file);
				}
			}
			return FileVisitResult.CONTINUE;
		}
	}

}

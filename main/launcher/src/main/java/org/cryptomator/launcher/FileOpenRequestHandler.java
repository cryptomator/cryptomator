/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved.
 * 
 * This class is licensed under the LGPL 3.0 (https://www.gnu.org/licenses/lgpl-3.0.de.html).
 *******************************************************************************/
package org.cryptomator.launcher;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FileOpenRequestHandler {

	private static final Logger LOG = LoggerFactory.getLogger(FileOpenRequestHandler.class);
	private final BlockingQueue<Path> fileOpenRequests;

	public FileOpenRequestHandler(BlockingQueue<Path> fileOpenRequests) {
		this.fileOpenRequests = fileOpenRequests;
		if (SystemUtils.IS_OS_MAC_OSX) {
			addOsxFileOpenHandler();
		}
	}

	public void handleLaunchArgs(String[] args) {
		handleLaunchArgs(FileSystems.getDefault(), args);
	}

	// visible for testing
	void handleLaunchArgs(FileSystem fs, String[] args) {
		for (String arg : args) {
			try {
				Path path = fs.getPath(arg);
				tryToEnqueueFileOpenRequest(path);
			} catch (InvalidPathException e) {
				LOG.trace("{} not a valid path", arg);
			}
		}
	}

	private void tryToEnqueueFileOpenRequest(Path path) {
		if (!fileOpenRequests.offer(path)) {
			LOG.warn("{} could not be enqueued for opening.", path);
		}
	}

	/**
	 * Event subscription code inspired by https://gitlab.com/axet/desktop/blob/master/java/src/main/java/com/github/axet/desktop/os/mac/AppleHandlers.java
	 */
	private void addOsxFileOpenHandler() {
		try {
			final Class<?> applicationClass = Class.forName("com.apple.eawt.Application");
			final Class<?> openFilesHandlerClass = Class.forName("com.apple.eawt.OpenFilesHandler");
			final Method getApplication = applicationClass.getMethod("getApplication");
			final Object application = getApplication.invoke(null);
			final Method setOpenFileHandler = applicationClass.getMethod("setOpenFileHandler", openFilesHandlerClass);
			final ClassLoader openFilesHandlerClassLoader = openFilesHandlerClass.getClassLoader();
			final OpenFilesEventInvocationHandler openFilesHandler = new OpenFilesEventInvocationHandler();
			final Object openFilesHandlerObject = Proxy.newProxyInstance(openFilesHandlerClassLoader, new Class<?>[] {openFilesHandlerClass}, openFilesHandler);
			setOpenFileHandler.invoke(application, openFilesHandlerObject);
		} catch (ReflectiveOperationException | RuntimeException e) {
			// Since we're trying to call OS-specific code, we'll just have to hope for the best.
			LOG.error("Exception adding OS X file open handler", e);
		}
	}

	/**
	 * Handler class inspired by https://gitlab.com/axet/desktop/blob/master/java/src/main/java/com/github/axet/desktop/os/mac/AppleHandlers.java
	 */
	private class OpenFilesEventInvocationHandler implements InvocationHandler {
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.getName().equals("openFiles")) {
				final Class<?> openFilesEventClass = Class.forName("com.apple.eawt.AppEvent$OpenFilesEvent");
				final Method getFiles = openFilesEventClass.getMethod("getFiles");
				Object e = args[0];
				try {
					@SuppressWarnings("unchecked")
					final List<File> ff = (List<File>) getFiles.invoke(e);
					ff.stream().map(File::toPath).forEach(fileOpenRequests::add);
				} catch (RuntimeException ee) {
					throw ee;
				} catch (Exception ee) {
					throw new RuntimeException(ee);
				}
			}
			return null;
		}
	}

}

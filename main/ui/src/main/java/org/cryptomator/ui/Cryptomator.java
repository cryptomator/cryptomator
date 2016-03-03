/*******************************************************************************
 * Copyright (c) 2014, 2016 cryptomator.org
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Tillmann Gaida - initial implementation
 *     Sebastian Stenzel - refactoring
 ******************************************************************************/
package org.cryptomator.ui;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.ui.util.SingleInstanceManager;
import org.cryptomator.ui.util.SingleInstanceManager.RemoteInstance;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;

public class Cryptomator {

	public static final CompletableFuture<Consumer<File>> OPEN_FILE_HANDLER = new CompletableFuture<>();
	private static final Logger LOG = LoggerFactory.getLogger(Cryptomator.class);
	private static final Set<Runnable> SHUTDOWN_TASKS = new ConcurrentHashSet<>();
	private static final CleanShutdownPerformer CLEAN_SHUTDOWN_PERFORMER = new CleanShutdownPerformer();

	public static void main(String[] args) {
		if (SystemUtils.IS_OS_MAC_OSX) {
			/*
			 * On OSX we're in an awkward position. We need to register a handler in the main thread of this application. However, we can't
			 * even pass objects to the application, so we're forced to use a static CompletableFuture for the handler, which actually opens
			 * the file in the application.
			 * 
			 * Code taken from https://github.com/axet/desktop/blob/master/src/main/java/com/github/axet/desktop/os/mac/AppleHandlers.java
			 */
			try {
				final Class<?> applicationClass = Class.forName("com.apple.eawt.Application");
				final Class<?> openFilesHandlerClass = Class.forName("com.apple.eawt.OpenFilesHandler");
				final Method getApplication = applicationClass.getMethod("getApplication");
				final Object application = getApplication.invoke(null);
				final Method setOpenFileHandler = applicationClass.getMethod("setOpenFileHandler", openFilesHandlerClass);

				final ClassLoader openFilesHandlerClassLoader = openFilesHandlerClass.getClassLoader();
				final OpenFilesHandlerClassHandler openFilesHandlerHandler = new OpenFilesHandlerClassHandler();
				final Object openFilesHandlerObject = Proxy.newProxyInstance(openFilesHandlerClassLoader, new Class<?>[] {openFilesHandlerClass}, openFilesHandlerHandler);

				setOpenFileHandler.invoke(application, openFilesHandlerObject);
			} catch (ReflectiveOperationException | RuntimeException e) {
				// Since we're trying to call OS-specific code, we'll just have
				// to hope for the best.
				LOG.error("exception adding OSX file open handler", e);
			}
		}

		/*
		 * Perform certain things on VM termination.
		 */
		Runtime.getRuntime().addShutdownHook(CLEAN_SHUTDOWN_PERFORMER);

		/*
		 * Before starting the application, we check if there is already an instance running on this computer. If so, we send our command
		 * line arguments to that instance and quit.
		 */
		final Optional<RemoteInstance> remoteInstance = SingleInstanceManager.getRemoteInstance(MainApplication.APPLICATION_KEY);

		if (remoteInstance.isPresent()) {
			try (RemoteInstance instance = remoteInstance.get()) {
				LOG.info("An instance of Cryptomator is already running at {}.", instance.getRemotePort());
				for (int i = 0; i < args.length; i++) {
					remoteInstance.get().sendMessage(args[i], 100);
				}
			} catch (Exception e) {
				LOG.error("Error forwarding arguments to remote instance", e);
			}
		} else {
			Application.launch(MainApplication.class, args);
		}
	}

	public static void addShutdownTask(Runnable r) {
		SHUTDOWN_TASKS.add(r);
	}

	public static void removeShutdownTask(Runnable r) {
		SHUTDOWN_TASKS.remove(r);
	}

	private static class CleanShutdownPerformer extends Thread {
		@Override
		public void run() {
			LOG.debug("Shutting down");
			SHUTDOWN_TASKS.forEach(r -> {
				try {
					r.run();
				} catch (RuntimeException e) {
					LOG.error("exception while shutting down", e);
				}
			});
			SHUTDOWN_TASKS.clear();
		}
	}

	private static void handleOpenFileRequest(File file) {
		try {
			OPEN_FILE_HANDLER.get().accept(file);
		} catch (Exception e) {
			LOG.error("exception handling file open event for file " + file.getAbsolutePath(), e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Handler class taken from https://github.com/axet/desktop/blob/master/src/main/java/com/github/axet/desktop/os/mac/AppleHandlers.java
	 */
	private static class OpenFilesHandlerClassHandler implements InvocationHandler {
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.getName().equals("openFiles")) {
				final Class<?> openFilesEventClass = Class.forName("com.apple.eawt.AppEvent$OpenFilesEvent");
				final Method getFiles = openFilesEventClass.getMethod("getFiles");
				Object e = args[0];
				try {
					@SuppressWarnings("unchecked")
					final List<File> ff = (List<File>) getFiles.invoke(e);
					for (File f : ff) {
						handleOpenFileRequest(f);
					}
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

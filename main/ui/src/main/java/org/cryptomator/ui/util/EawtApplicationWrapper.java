/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.util;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.SupplierThrowingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reflection-based wrapper for com.apple.eawt.Application.
 */
public class EawtApplicationWrapper {

	private static final Logger LOG = LoggerFactory.getLogger(EawtApplicationWrapper.class);

	private final Class<?> applicationClass;
	private final Object application;

	private EawtApplicationWrapper() throws ReflectiveOperationException {
		this.applicationClass = Class.forName("com.apple.eawt.Application");
		this.application = applicationClass.getMethod("getApplication").invoke(null);
	}

	/**
	 * @return A wrapper for com.apple.ewat.Application if the current OS is macOS and the class is available in this JVM.
	 */
	public static Optional<EawtApplicationWrapper> getApplication() {
		if (!SystemUtils.IS_OS_MAC_OSX) {
			return Optional.empty();
		}
		try {
			return Optional.of(new EawtApplicationWrapper());
		} catch (ReflectiveOperationException e) {
			return Optional.empty();
		}
	}

	private void setOpenFileHandler(InvocationHandler handler) throws ReflectiveOperationException {
		Class<?> handlerClass = Class.forName("com.apple.eawt.OpenFilesHandler");
		Method setter = applicationClass.getMethod("setOpenFileHandler", handlerClass);
		Object proxy = Proxy.newProxyInstance(applicationClass.getClassLoader(), new Class<?>[] {handlerClass}, handler);
		setter.invoke(application, proxy);
	}

	public void setOpenFileHandler(Consumer<List<File>> handler) {
		try {
			Class<?> openFilesEventClass = Class.forName("com.apple.eawt.AppEvent$OpenFilesEvent");
			Method getFiles = openFilesEventClass.getMethod("getFiles");
			setOpenFileHandler(methodSpecificInvocationHandler("openFiles", args -> {
				Object openFilesEvent = args[0];
				assert openFilesEventClass.isInstance(openFilesEvent);
				@SuppressWarnings("unchecked")
				List<File> files = (List<File>) uncheckedReflectiveOperation(() -> getFiles.invoke(openFilesEvent));
				handler.accept(files);
				return null;
			}));
		} catch (ReflectiveOperationException e) {
			LOG.error("Exception setting openFileHandler.", e);
		}
	}

	private void setPreferencesHandler(InvocationHandler handler) throws ReflectiveOperationException {
		Class<?> handlerClass = Class.forName("com.apple.eawt.PreferencesHandler");
		Method setter = applicationClass.getMethod("setPreferencesHandler", handlerClass);
		Object proxy = Proxy.newProxyInstance(applicationClass.getClassLoader(), new Class<?>[] {handlerClass}, handler);
		setter.invoke(application, proxy);
	}

	public void setPreferencesHandler(Runnable handler) {
		try {
			setPreferencesHandler(methodSpecificInvocationHandler("handlePreferences", args -> {
				handler.run();
				return null;
			}));
		} catch (ReflectiveOperationException e) {
			LOG.error("Exception setting preferencesHandler.", e);
		}
	}

	private static InvocationHandler methodSpecificInvocationHandler(String methodName, Function<Object[], Object> handler) {
		return new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				if (method.getName().equals(methodName)) {
					return handler.apply(args);
				} else {
					throw new UnsupportedOperationException("Unexpected invocation " + method.getName() + ", expected " + methodName);
				}
			}
		};
	}

	/**
	 * Wraps {@link ReflectiveOperationException}s as {@link UncheckedReflectiveOperationException}.
	 * 
	 * @param operation Invokation throwing an ReflectiveOperationException
	 * @return Result returned by <code>operation</code>
	 * @throws UncheckedReflectiveOperationException in case <code>operation</code> throws an ReflectiveOperationException.
	 */
	private static <T> T uncheckedReflectiveOperation(SupplierThrowingException<T, ReflectiveOperationException> operation) throws UncheckedReflectiveOperationException {
		try {
			return operation.get();
		} catch (ReflectiveOperationException e) {
			throw new UncheckedReflectiveOperationException(e);
		}
	}

	private static class UncheckedReflectiveOperationException extends RuntimeException {
		public UncheckedReflectiveOperationException(ReflectiveOperationException cause) {
			super(cause);
		}
	}

}

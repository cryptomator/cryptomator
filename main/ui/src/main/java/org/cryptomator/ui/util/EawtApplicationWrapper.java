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

	public static Optional<EawtApplicationWrapper> getApplication() {
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
			setOpenFileHandler(newMethodSpecificInvocationHandler("openFiles", args -> {
				try {
					Object openFilesEvent = args[0];
					@SuppressWarnings("unchecked")
					List<File> files = (List<File>) getFiles.invoke(openFilesEvent);
					handler.accept(files);
				} catch (ReflectiveOperationException e) {
					LOG.error("Error invoking openFileHandler.", e);
				}
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
			setPreferencesHandler(newMethodSpecificInvocationHandler("handlePreferences", args -> {
				handler.run();
				return null;
			}));
		} catch (ReflectiveOperationException e) {
			LOG.error("Exception setting preferencesHandler.", e);
		}
	}

	@FunctionalInterface
	private static interface MethodSpecificInvocationHandler {
		Object invoke(Object[] args);
	}

	private static InvocationHandler newMethodSpecificInvocationHandler(String methodName, MethodSpecificInvocationHandler handler) {
		return new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				if (method.getName().equals(methodName)) {
					return handler.invoke(args);
				} else {
					return null;
				}
			}
		};
	}

}

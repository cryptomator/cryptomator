/*******************************************************************************
 * Copyright (c) 2014, 2016 cryptomator.org
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Tillmann Gaida - initial implementation
 ******************************************************************************/
package org.cryptomator.ui.util;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.cryptomator.common.ConsumerThrowingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

/**
 * <p>
 * Tries to bring open-close symmetry in contexts where the resource outlives
 * the current scope by introducing a manager, which closes the resources if
 * they haven't been closed before.
 * </p>
 * 
 * <p>
 * If you have a {@link DeferredCloser} instance present, call
 * {@link #closeLater(Object, Closer)} immediately after you have opened the
 * resource and return a resource handle. If {@link #close()} is called, the
 * resource will be closed. Calling {@link DeferredClosable#close()} on the resource
 * handle will also close the resource and prevent a second closing by
 * {@link #close()}.
 * </p>
 * 
 * @author Tillmann Gaida
 */
public class DeferredCloser implements AutoCloseable {

	private static final Logger LOG = LoggerFactory.getLogger(DeferredCloser.class);

	@VisibleForTesting
	final Map<Long, ManagedResource<?>> cleanups = new ConcurrentSkipListMap<>();

	@VisibleForTesting
	final AtomicLong counter = new AtomicLong();

	private class ManagedResource<T> implements DeferredClosable<T> {
		private final long number = counter.incrementAndGet();

		private final AtomicReference<T> object = new AtomicReference<>();
		private final ConsumerThrowingException<T, Exception> closer;

		public ManagedResource(T object, ConsumerThrowingException<T, Exception> closer) {
			super();
			this.object.set(object);
			this.closer = closer;
		}

		@Override
		public void close() {
			final T oldObject = object.getAndSet(null);
			if (oldObject != null) {
				cleanups.remove(number);
				try {
					closer.accept(oldObject);
				} catch (Exception e) {
					LOG.error("Closing resource failed.", e);
				}
			}
		}

		@Override
		public Optional<T> get() throws IllegalStateException {
			return Optional.ofNullable(object.get());
		}
	}

	/**
	 * Closes all added objects which have not been closed before and releases references.
	 */
	@Override
	public void close() {
		for (Iterator<ManagedResource<?>> iterator = cleanups.values().iterator(); iterator.hasNext();) {
			final ManagedResource<?> closableProvider = iterator.next();
			closableProvider.close();
			iterator.remove();
		}
	}

	public <T> DeferredClosable<T> closeLater(T object, ConsumerThrowingException<T, Exception> closer) {
		Objects.requireNonNull(object);
		Objects.requireNonNull(closer);
		final ManagedResource<T> resource = new ManagedResource<T>(object, closer);
		cleanups.put(resource.number, resource);
		return resource;
	}

	public <T extends AutoCloseable> DeferredClosable<T> closeLater(T object) {
		Objects.requireNonNull(object);
		final ManagedResource<T> resource = new ManagedResource<T>(object, AutoCloseable::close);
		cleanups.put(resource.number, resource);
		return resource;
	}

	private static final EmptyResource<?> EMPTY_RESOURCE = new EmptyResource<>();

	@SuppressWarnings("unchecked")
	public static <T> DeferredClosable<T> empty() {
		return (DeferredClosable<T>) EMPTY_RESOURCE;
	}

	static class EmptyResource<T> implements DeferredClosable<T> {
		@Override
		public Optional<T> get() {
			return Optional.empty();
		}

		@Override
		public void close() {

		}
	}
}

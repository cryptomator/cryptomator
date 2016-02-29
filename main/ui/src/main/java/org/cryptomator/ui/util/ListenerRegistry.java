/*******************************************************************************
 * Copyright (c) 2014, 2016 cryptomator.org
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Tillmann Gaida - initial implementation
 ******************************************************************************/
package org.cryptomator.ui.util;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

/**
 * Manages and broadcasts events to a set of listeners. The types of the
 * listener and event are entirely unbound. Instead, a method must be supplied
 * to broadcast an event to a single listener.
 * 
 * @author Tillmann Gaida
 *
 * @param <LISTENER>
 *            The type of listener.
 * @param <EVENT>
 *            The type of event.
 */
public class ListenerRegistry<LISTENER, EVENT> {
	final BiConsumer<LISTENER, EVENT> listenerCaller;

	/**
	 * Constructs a new registry.
	 * 
	 * @param listenerCaller
	 *            The method which broadcasts an event to a single listener.
	 */
	public ListenerRegistry(BiConsumer<LISTENER, EVENT> listenerCaller) {
		super();
		this.listenerCaller = listenerCaller;
	}

	/**
	 * The handle of a registered listener.
	 */
	public interface ListenerRegistration {
		void unregister();
	}

	final AtomicLong serial = new AtomicLong();
	/*
	 * Since this is a {@link ConcurrentSkipListMap}, we can at the same time
	 * add to, remove from, and iterate over it. More importantly, a Listener
	 * can remove itself while being called from the {@link #broadcast(Object)}
	 * method.
	 */
	final Map<Long, LISTENER> listeners = new ConcurrentSkipListMap<>();

	public ListenerRegistration registerListener(LISTENER listener) {
		final long s = serial.incrementAndGet();

		listeners.put(s, listener);

		return () -> {
			listeners.remove(s);
		};
	}

	/**
	 * Broadcasts the given event to all registered listeners. If a listener
	 * causes an unchecked exception, that exception is thrown immediately
	 * without calling the other listeners.
	 * 
	 * @param event
	 */
	public void broadcast(EVENT event) {
		for (LISTENER listener : listeners.values()) {
			listenerCaller.accept(listener, event);
		}
	}
}

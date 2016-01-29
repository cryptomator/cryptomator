/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.collections.SetChangeListener.Change;

import com.google.common.collect.ImmutableList;

class ObservableSetOnMainThread<E> implements ObservableSet<E> {

	private final ObservableSet<E> set;
	private final Collection<InvalidationListener> invalidationListeners;
	private final Collection<SetChangeListener<? super E>> setChangeListeners;

	public ObservableSetOnMainThread(ObservableSet<E> set) {
		this.set = set;
		this.invalidationListeners = new HashSet<>();
		this.setChangeListeners = new HashSet<>();
		this.set.addListener(this::invalidated);
		this.set.addListener(this::onChanged);
	}

	@Override
	public int size() {
		return set.size();
	}

	@Override
	public boolean isEmpty() {
		return set.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return set.contains(o);
	}

	@Override
	public Iterator<E> iterator() {
		return set.iterator();
	}

	@Override
	public Object[] toArray() {
		return set.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return set.toArray(a);
	}

	@Override
	public boolean add(E e) {
		return set.add(e);
	}

	@Override
	public boolean remove(Object o) {
		return set.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return set.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		return set.addAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return set.retainAll(c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return set.removeAll(c);
	}

	@Override
	public void clear() {
		set.clear();
	}

	private void invalidated(Observable observable) {
		final Collection<InvalidationListener> listeners = ImmutableList.copyOf(invalidationListeners);
		Platform.runLater(() -> {
			for (InvalidationListener listener : listeners) {
				listener.invalidated(this);
			}
		});
	}

	@Override
	public void addListener(InvalidationListener listener) {
		invalidationListeners.add(listener);
	}

	@Override
	public void removeListener(InvalidationListener listener) {
		invalidationListeners.remove(listener);
	}

	private void onChanged(Change<? extends E> change) {
		final Change<? extends E> c = new SetChange(this, change.getElementAdded(), change.getElementRemoved());
		final Collection<SetChangeListener<? super E>> listeners = ImmutableList.copyOf(setChangeListeners);
		Platform.runLater(() -> {
			for (SetChangeListener<? super E> listener : listeners) {
				listener.onChanged(c);
			}
		});
	}

	@Override
	public void addListener(SetChangeListener<? super E> listener) {
		setChangeListeners.add(listener);
	}

	@Override
	public void removeListener(SetChangeListener<? super E> listener) {
		setChangeListeners.add(listener);
	}

	private class SetChange extends SetChangeListener.Change<E> {

		private final E added;
		private final E removed;

		public SetChange(ObservableSet<E> set, E added, E removed) {
			super(set);
			this.added = added;
			this.removed = removed;
		}

		@Override
		public boolean wasAdded() {
			return added != null;
		}

		@Override
		public boolean wasRemoved() {
			return removed != null;
		}

		@Override
		public E getElementAdded() {
			return added;
		}

		@Override
		public E getElementRemoved() {
			return removed;
		}

	}

}
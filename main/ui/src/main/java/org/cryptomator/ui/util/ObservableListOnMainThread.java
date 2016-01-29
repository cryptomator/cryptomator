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
import java.util.List;
import java.util.ListIterator;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;

import com.google.common.collect.ImmutableList;

class ObservableListOnMainThread<E> implements ObservableList<E> {

	private final ObservableList<E> list;
	private final Collection<InvalidationListener> invalidationListeners;
	private final Collection<ListChangeListener<? super E>> listChangeListeners;

	public ObservableListOnMainThread(ObservableList<E> list) {
		this.list = list;
		this.invalidationListeners = new HashSet<>();
		this.listChangeListeners = new HashSet<>();
		this.list.addListener(this::invalidated);
		this.list.addListener(this::onChanged);
	}

	@Override
	public int size() {
		return list.size();
	}

	@Override
	public boolean isEmpty() {
		return list.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return list.contains(o);
	}

	@Override
	public Iterator<E> iterator() {
		return list.iterator();
	}

	@Override
	public Object[] toArray() {
		return list.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return list.toArray(a);
	}

	@Override
	public boolean add(E e) {
		return list.add(e);
	}

	@Override
	public boolean remove(Object o) {
		return list.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return list.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		return list.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		return list.addAll(index, c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return list.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return list.retainAll(c);
	}

	@Override
	public void clear() {
		list.clear();
	}

	@Override
	public E get(int index) {
		return list.get(index);
	}

	@Override
	public E set(int index, E element) {
		return list.set(index, element);
	}

	@Override
	public void add(int index, E element) {
		list.add(index, element);
	}

	@Override
	public E remove(int index) {
		return list.remove(index);
	}

	@Override
	public int indexOf(Object o) {
		return list.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return list.lastIndexOf(o);
	}

	@Override
	public ListIterator<E> listIterator() {
		return list.listIterator();
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return list.listIterator(index);
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		return list.subList(fromIndex, toIndex);
	}

	@Override
	public boolean addAll(@SuppressWarnings("unchecked") E... elements) {
		return list.addAll(elements);
	}

	@Override
	public boolean setAll(@SuppressWarnings("unchecked") E... elements) {
		return list.addAll(elements);
	}

	@Override
	public boolean setAll(Collection<? extends E> col) {
		return list.setAll(col);
	}

	@Override
	public boolean removeAll(@SuppressWarnings("unchecked") E... elements) {
		return list.removeAll(elements);
	}

	@Override
	public boolean retainAll(@SuppressWarnings("unchecked") E... elements) {
		return list.retainAll(elements);
	}

	@Override
	public void remove(int from, int to) {
		list.remove(from, to);
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
		final Change<? extends E> c = new ListChange(change);
		final Collection<ListChangeListener<? super E>> listeners = ImmutableList.copyOf(listChangeListeners);
		Platform.runLater(() -> {
			for (ListChangeListener<? super E> listener : listeners) {
				listener.onChanged(c);
			}
		});
	}

	@Override
	public void addListener(ListChangeListener<? super E> listener) {
		listChangeListeners.add(listener);
	}

	@Override
	public void removeListener(ListChangeListener<? super E> listener) {
		listChangeListeners.remove(listener);
	}

	private class ListChange extends ListChangeListener.Change<E> {

		private final Change<? extends E> originalChange;

		public ListChange(Change<? extends E> change) {
			super(ObservableListOnMainThread.this);
			this.originalChange = change;
		}

		@Override
		public boolean wasAdded() {
			return originalChange.wasAdded();
		}

		@Override
		public boolean wasRemoved() {
			return originalChange.wasRemoved();
		}

		@Override
		public boolean next() {
			return originalChange.next();
		}

		@Override
		public void reset() {
			originalChange.reset();
		}

		@Override
		public int getFrom() {
			return originalChange.getFrom();
		}

		@Override
		public int getTo() {
			return originalChange.getTo();
		}

		@Override
		@SuppressWarnings("unchecked")
		public List<E> getRemoved() {
			return (List<E>) originalChange.getRemoved();
		}

		@Override
		protected int[] getPermutation() {
			if (originalChange.wasPermutated()) {
				int[] permutations = new int[originalChange.getTo() - originalChange.getFrom()];
				for (int i = 0; i < permutations.length; i++) {
					permutations[i] = originalChange.getPermutation(i);
				}
				return permutations;
			} else {
				return new int[0];
			}
		}

	}

}
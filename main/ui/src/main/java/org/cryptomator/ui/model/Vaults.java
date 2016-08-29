package org.cryptomator.ui.model;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.cryptomator.ui.settings.Settings;

import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;

@Singleton
public class Vaults implements ObservableList<Vault> {

	private final ObservableList<Vault> delegate;
	
	@Inject
	public Vaults(Settings settings) {
		this.delegate = FXCollections.observableList(settings.getDirectories());
		addListener((Change<? extends Vault> change) -> settings.save());
	}
	
	public void addListener(ListChangeListener<? super Vault> listener) {
		delegate.addListener(listener);
	}

	public void removeListener(ListChangeListener<? super Vault> listener) {
		delegate.removeListener(listener);
	}

	public void addListener(InvalidationListener listener) {
		delegate.addListener(listener);
	}

	public boolean addAll(Vault... elements) {
		return delegate.addAll(elements);
	}

	public boolean setAll(Vault... elements) {
		return delegate.setAll(elements);
	}

	public boolean setAll(Collection<? extends Vault> col) {
		return delegate.setAll(col);
	}

	public boolean removeAll(Vault... elements) {
		return delegate.removeAll(elements);
	}

	public void removeListener(InvalidationListener listener) {
		delegate.removeListener(listener);
	}

	public boolean retainAll(Vault... elements) {
		return delegate.retainAll(elements);
	}

	public void remove(int from, int to) {
		delegate.remove(from, to);
	}

	public int size() {
		return delegate.size();
	}

	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	public boolean contains(Object o) {
		return delegate.contains(o);
	}

	public Iterator<Vault> iterator() {
		return delegate.iterator();
	}

	public Object[] toArray() {
		return delegate.toArray();
	}

	public <T> T[] toArray(T[] a) {
		return delegate.toArray(a);
	}

	public boolean add(Vault e) {
		return delegate.add(e);
	}

	public boolean remove(Object o) {
		return delegate.remove(o);
	}

	public boolean containsAll(Collection<?> c) {
		return delegate.containsAll(c);
	}

	public boolean addAll(Collection<? extends Vault> c) {
		return delegate.addAll(c);
	}

	public boolean addAll(int index, Collection<? extends Vault> c) {
		return delegate.addAll(index, c);
	}

	public boolean removeAll(Collection<?> c) {
		return delegate.removeAll(c);
	}

	public boolean retainAll(Collection<?> c) {
		return delegate.retainAll(c);
	}

	public void clear() {
		delegate.clear();
	}

	public Vault get(int index) {
		return delegate.get(index);
	}

	public Vault set(int index, Vault element) {
		return delegate.set(index, element);
	}

	public void add(int index, Vault element) {
		delegate.add(index, element);
	}

	public Vault remove(int index) {
		return delegate.remove(index);
	}

	public int indexOf(Object o) {
		return delegate.indexOf(o);
	}

	public int lastIndexOf(Object o) {
		return delegate.lastIndexOf(o);
	}

	public ListIterator<Vault> listIterator() {
		return delegate.listIterator();
	}

	public ListIterator<Vault> listIterator(int index) {
		return delegate.listIterator(index);
	}

	public List<Vault> subList(int fromIndex, int toIndex) {
		return delegate.subList(fromIndex, toIndex);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		return internalEquals((Vaults)obj);
	}

	private boolean internalEquals(Vaults other) {
		return delegate.equals(other.delegate);
	}

	public int hashCode() {
		return delegate.hashCode();
	}
	
}

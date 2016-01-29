/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav.mount;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import java.util.Collection;
import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
class MountStrategies implements Collection<WebDavMounterStrategy> {
	
	private final Collection<WebDavMounterStrategy> delegate;
	
	@Inject
	MountStrategies(LinuxGvfsWebDavMounter linuxMounter, MacOsXWebDavMounter osxMounter, WindowsWebDavMounter winMounter) {
		delegate = unmodifiableList(asList(linuxMounter, osxMounter, winMounter));
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

	public Iterator<WebDavMounterStrategy> iterator() {
		return delegate.iterator();
	}

	public Object[] toArray() {
		return delegate.toArray();
	}

	public <T> T[] toArray(T[] a) {
		return delegate.toArray(a);
	}

	public boolean add(WebDavMounterStrategy e) {
		return delegate.add(e);
	}

	public boolean remove(Object o) {
		return delegate.remove(o);
	}

	public boolean containsAll(Collection<?> c) {
		return delegate.containsAll(c);
	}

	public boolean addAll(Collection<? extends WebDavMounterStrategy> c) {
		return delegate.addAll(c);
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

	public boolean equals(Object o) {
		return delegate.equals(o);
	}

	public int hashCode() {
		return delegate.hashCode();
	}



}

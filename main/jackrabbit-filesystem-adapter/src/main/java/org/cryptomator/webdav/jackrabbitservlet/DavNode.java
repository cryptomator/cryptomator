/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.webdav.jackrabbitservlet;

import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.lock.ActiveLock;
import org.apache.jackrabbit.webdav.lock.LockInfo;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.Type;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.PropEntry;
import org.cryptomator.filesystem.jackrabbit.FileSystemResourceLocator;

abstract class DavNode<T extends FileSystemResourceLocator> implements DavResource {

	private static final String DAV_COMPLIANCE_CLASSES = "1, 2";
	private static final String[] DAV_CREATIONDATE_PROPNAMES = {DavPropertyName.CREATIONDATE.getName(), "Win32CreationTime"};
	private static final String[] DAV_MODIFIEDDATE_PROPNAMES = {DavPropertyName.GETLASTMODIFIED.getName(), "Win32LastModifiedTime"};

	protected final FilesystemResourceFactory factory;
	protected final LockManager lockManager;
	protected final DavSession session;
	protected final T node;
	protected final DavPropertySet properties;

	public DavNode(FilesystemResourceFactory factory, LockManager lockManager, DavSession session, T node) {
		this.factory = factory;
		this.lockManager = lockManager;
		this.session = session;
		this.node = node;
		this.properties = new DavPropertySet();
	}

	@Override
	public String getComplianceClass() {
		return DAV_COMPLIANCE_CLASSES;
	}

	@Override
	public String getSupportedMethods() {
		return METHODS;
	}

	@Override
	public boolean exists() {
		return node.exists();
	}

	@Override
	public String getDisplayName() {
		return node.name();
	}

	@Override
	public DavResourceLocator getLocator() {
		return node;
	}

	@Override
	public String getResourcePath() {
		return node.getResourcePath();
	}

	@Override
	public String getHref() {
		return node.getHref();
	}

	@Override
	public long getModificationTime() {
		try {
			return node.lastModified().toEpochMilli();
		} catch (UncheckedIOException e) {
			return -1l;
		}
	}

	protected abstract void setModificationTime(Instant instant);

	protected abstract void setCreationTime(Instant instant);

	@Override
	public DavPropertyName[] getPropertyNames() {
		return getProperties().getPropertyNames();
	}

	@Override
	public DavProperty<?> getProperty(DavPropertyName name) {
		return getProperties().get(name);
	}

	@Override
	public DavPropertySet getProperties() {
		return properties;
	}

	@Override
	public void setProperty(DavProperty<?> property) throws DavException {
		getProperties().add(property);

		final String namespacelessPropertyName = property.getName().getName();
		if (Arrays.asList(DAV_CREATIONDATE_PROPNAMES).contains(namespacelessPropertyName) && property.getValue() instanceof String) {
			final String createDateStr = (String) property.getValue();
			final Instant createTime = Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(createDateStr));
			this.setCreationTime(createTime);
		} else if (Arrays.asList(DAV_MODIFIEDDATE_PROPNAMES).contains(namespacelessPropertyName) && property.getValue() instanceof String) {
			final String lastModifiedTimeStr = (String) property.getValue();
			final Instant createTime = Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(lastModifiedTimeStr));
			this.setCreationTime(createTime);
		}
	}

	@Override
	public void removeProperty(DavPropertyName propertyName) throws DavException {
		getProperties().remove(propertyName);
	}

	@Override
	public MultiStatusResponse alterProperties(List<? extends PropEntry> changeList) throws DavException {
		final DavPropertyNameSet names = new DavPropertyNameSet();
		for (final PropEntry entry : changeList) {
			if (entry instanceof DavProperty) {
				final DavProperty<?> prop = (DavProperty<?>) entry;
				this.setProperty(prop);
				names.add(prop.getName());
			} else if (entry instanceof DavPropertyName) {
				final DavPropertyName name = (DavPropertyName) entry;
				this.removeProperty(name);
				names.add(name);
			}
		}
		return new MultiStatusResponse(this, names);
	}

	@Override
	public DavResource getCollection() {
		if (node.isRootLocation()) {
			return null;
		}

		assert node.parent().isPresent() : "as my mom always sais: if it's not root, it has a parent";
		final FileSystemResourceLocator parentPath = node.parent().get();
		try {
			return factory.createResource(parentPath, session);
		} catch (DavException e) {
			throw new IllegalStateException("Unable to get parent resource with path " + parentPath, e);
		}
	}

	@Override
	public boolean isLockable(Type type, Scope scope) {
		return true;
	}

	@Override
	public boolean hasLock(Type type, Scope scope) {
		return getLock(type, scope) != null;
	}

	@Override
	public ActiveLock getLock(Type type, Scope scope) {
		return lockManager.getLock(type, scope, this);
	}

	@Override
	public ActiveLock[] getLocks() {
		final ActiveLock exclusiveWriteLock = getLock(Type.WRITE, Scope.EXCLUSIVE);
		if (exclusiveWriteLock != null) {
			return new ActiveLock[] {exclusiveWriteLock};
		} else {
			return new ActiveLock[0];
		}
	}

	@Override
	public ActiveLock lock(LockInfo reqLockInfo) throws DavException {
		return lockManager.createLock(reqLockInfo, this);
	}

	@Override
	public ActiveLock refreshLock(LockInfo reqLockInfo, String lockToken) throws DavException {
		return lockManager.refreshLock(reqLockInfo, lockToken, this);
	}

	@Override
	public void unlock(String lockToken) throws DavException {
		lockManager.releaseLock(lockToken, this);
	}

	@Override
	public void addLockManager(LockManager lockmgr) {
		throw new UnsupportedOperationException("Locks are managed");
	}

	@Override
	public FilesystemResourceFactory getFactory() {
		return factory;
	}

	@Override
	public DavSession getSession() {
		return session;
	}

}

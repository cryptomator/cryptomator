/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav.jackrabbitservlet;

import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
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
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.property.PropEntry;
import org.cryptomator.filesystem.jackrabbit.FileSystemResourceLocator;
import org.cryptomator.filesystem.jackrabbit.FolderLocator;

abstract class DavNode<T extends FileSystemResourceLocator> implements DavResource {

	private static final String DAV_COMPLIANCE_CLASSES = "1, 2";
	private static final String[] DAV_CREATIONDATE_PROPNAMES = {DavConstants.PROPERTY_CREATIONDATE, "Win32CreationTime"};
	private static final String[] DAV_MODIFIEDDATE_PROPNAMES = {DavConstants.PROPERTY_GETLASTMODIFIED, "Win32LastModifiedTime"};

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
	public FileSystemResourceLocator getLocator() {
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
		final String namespacelessPropertyName = name.getName();
		if (Arrays.asList(DAV_CREATIONDATE_PROPNAMES).contains(namespacelessPropertyName)) {
			return creationDateProperty(name).orElse(null);
		} else if (Arrays.asList(DAV_MODIFIEDDATE_PROPNAMES).contains(namespacelessPropertyName)) {
			Temporal lastModifiedDate = OffsetDateTime.ofInstant(node.lastModified(), ZoneOffset.UTC);
			return new DefaultDavProperty<>(name, DateTimeFormatter.RFC_1123_DATE_TIME.format(lastModifiedDate));
		} else {
			return properties.get(name);
		}
	}

	/**
	 * Returns a current snapshot of all available properties.
	 */
	@Override
	public DavPropertySet getProperties() {
		// creation date:
		if (node.exists()) {
			creationDateProperty(DavPropertyName.CREATIONDATE).ifPresent(properties::add);
		}
		// modification date:
		if (node.exists()) {
			Temporal lastModifiedDate = OffsetDateTime.ofInstant(node.lastModified(), ZoneOffset.UTC);
			String lastModifiedDateStr = DateTimeFormatter.RFC_1123_DATE_TIME.format(lastModifiedDate);
			DavProperty<String> lastModifiedDateProp = new DefaultDavProperty<>(DavPropertyName.GETLASTMODIFIED, lastModifiedDateStr);
			properties.add(lastModifiedDateProp);
		}
		return properties;
	}

	private Optional<DavProperty<?>> creationDateProperty(DavPropertyName name) {
		return node.creationTime() //
				.map(creationTime -> OffsetDateTime.ofInstant(creationTime, ZoneOffset.UTC)) //
				.map(creationDate -> new DefaultDavProperty<>(name, DateTimeFormatter.RFC_1123_DATE_TIME.format(creationDate)));
	}

	@Override
	public void setProperty(DavProperty<?> property) throws DavException {
		final String namespacelessPropertyName = property.getName().getName();
		if (Arrays.asList(DAV_CREATIONDATE_PROPNAMES).contains(namespacelessPropertyName) && property.getValue() instanceof String) {
			String createDateStr = (String) property.getValue();
			OffsetDateTime creationDate = OffsetDateTime.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(createDateStr));
			this.setCreationTime(creationDate.toInstant());
		} else if (Arrays.asList(DAV_MODIFIEDDATE_PROPNAMES).contains(namespacelessPropertyName) && property.getValue() instanceof String) {
			String lastModifiedDateStr = (String) property.getValue();
			OffsetDateTime lastModifiedDate = OffsetDateTime.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(lastModifiedDateStr));
			this.setModificationTime(lastModifiedDate.toInstant());
		}
		properties.add(property);
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
	public DavFolder getCollection() {
		if (node.isRootLocation()) {
			return null;
		}

		assert node.parent().isPresent() : "as my mom always sais: if it's not root, it has a parent";
		final FolderLocator parentPath = node.parent().get();
		return factory.createFolder(parentPath, session);
	}

	@Override
	public boolean isLockable(Type type, Scope scope) {
		return Type.WRITE.equals(type) && Scope.EXCLUSIVE.equals(scope) || Scope.SHARED.equals(scope);
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
		final ActiveLock sharedWriteLock = getLock(Type.WRITE, Scope.SHARED);
		return Stream.of(exclusiveWriteLock, sharedWriteLock).filter(Objects::nonNull).toArray(ActiveLock[]::new);
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

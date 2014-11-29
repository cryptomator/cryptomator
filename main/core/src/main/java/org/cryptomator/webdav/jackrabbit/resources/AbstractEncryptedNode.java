/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.webdav.jackrabbit.resources;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
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
import org.cryptomator.crypto.Cryptor;
import org.cryptomator.webdav.exceptions.IORuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractEncryptedNode implements DavResource {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractEncryptedNode.class);
	private static final String DAV_COMPLIANCE_CLASSES = "1, 2";

	protected final DavResourceFactory factory;
	protected final DavResourceLocator locator;
	protected final DavSession session;
	protected final LockManager lockManager;
	protected final Cryptor cryptor;
	protected final DavPropertySet properties;

	protected AbstractEncryptedNode(DavResourceFactory factory, DavResourceLocator locator, DavSession session, LockManager lockManager, Cryptor cryptor) {
		this.factory = factory;
		this.locator = locator;
		this.session = session;
		this.lockManager = lockManager;
		this.cryptor = cryptor;
		this.properties = new DavPropertySet();
		this.determineProperties();
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
		final Path path = PathUtils.getPhysicalPath(this);
		return Files.exists(path);
	}

	@Override
	public String getDisplayName() {
		final String resourcePath = getResourcePath();
		final int lastSlash = resourcePath.lastIndexOf('/');
		if (lastSlash == -1) {
			return resourcePath;
		} else {
			return resourcePath.substring(lastSlash);
		}
	}

	@Override
	public DavResourceLocator getLocator() {
		return locator;
	}

	@Override
	public String getResourcePath() {
		return locator.getResourcePath();
	}

	@Override
	public String getHref() {
		return locator.getHref(this.isCollection());
	}

	@Override
	public long getModificationTime() {
		final Path path = PathUtils.getPhysicalPath(this);
		try {
			return Files.getLastModifiedTime(path).toMillis();
		} catch (IOException e) {
			return -1;
		}
	}

	protected abstract void determineProperties();

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
		if (locator.isRootLocation()) {
			return null;
		}

		final String parentResource = FilenameUtils.getPath(locator.getResourcePath());
		final DavResourceLocator parentLocator = locator.getFactory().createResourceLocator(locator.getPrefix(), locator.getWorkspacePath(), parentResource);
		try {
			return getFactory().createResource(parentLocator, session);
		} catch (DavException e) {
			throw new IllegalStateException("Unable to get parent resource with path " + parentLocator.getResourcePath(), e);
		}
	}

	@Override
	public void move(DavResource dest) throws DavException {
		final Path src = PathUtils.getPhysicalPath(this);
		final Path dst = PathUtils.getPhysicalPath(dest);
		try {
			// check for conflicts:
			if (Files.exists(dst) && Files.getLastModifiedTime(dst).toMillis() > Files.getLastModifiedTime(src).toMillis()) {
				throw new DavException(DavServletResponse.SC_CONFLICT, "File at destination already exists: " + dst.toString());
			}

			// move:
			try {
				Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			LOG.error("Error moving file from " + src.toString() + " to " + dst.toString());
			throw new IORuntimeException(e);
		}
	}

	@Override
	public void copy(DavResource dest, boolean shallow) throws DavException {
		final Path src = PathUtils.getPhysicalPath(this);
		final Path dst = PathUtils.getPhysicalPath(dest);
		try {
			// check for conflicts:
			if (Files.exists(dst) && Files.getLastModifiedTime(dst).toMillis() > Files.getLastModifiedTime(src).toMillis()) {
				throw new DavException(DavServletResponse.SC_CONFLICT, "File at destination already exists: " + dst.toString());
			}

			// copy:
			try {
				Files.copy(src, dst, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.copy(src, dst, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			LOG.error("Error copying file from " + src.toString() + " to " + dst.toString());
			throw new IORuntimeException(e);
		}
	}

	@Override
	public boolean isLockable(Type type, Scope scope) {
		return true;
	}

	@Override
	public boolean hasLock(Type type, Scope scope) {
		return lockManager.getLock(type, scope, this) != null;
	}

	@Override
	public ActiveLock getLock(Type type, Scope scope) {
		return lockManager.getLock(type, scope, this);
	}

	@Override
	public ActiveLock[] getLocks() {
		final ActiveLock exclusiveWriteLock = getLock(Type.WRITE, Scope.EXCLUSIVE);
		return new ActiveLock[] {exclusiveWriteLock};
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
	public DavResourceFactory getFactory() {
		return factory;
	}

	@Override
	public DavSession getSession() {
		return session;
	}

}

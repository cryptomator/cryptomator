/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav.jackrabbitservlet;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.lock.ActiveLock;
import org.apache.jackrabbit.webdav.lock.LockInfo;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.Type;
import org.cryptomator.filesystem.jackrabbit.FileSystemResourceLocator;
import org.cryptomator.filesystem.jackrabbit.FolderLocator;

class ExclusiveSharedLockManager implements LockManager {

	private final ConcurrentMap<FileSystemResourceLocator, Map<String, ActiveLock>> lockedResources = new ConcurrentHashMap<>();

	@Override
	public ActiveLock createLock(LockInfo lockInfo, DavResource resource) throws DavException {
		Objects.requireNonNull(lockInfo);
		Objects.requireNonNull(resource);
		if (resource instanceof DavNode) {
			return createLockInternal(lockInfo, (DavNode<?>) resource);
		} else {
			throw new IllegalArgumentException("Unsupported resource type " + resource.getClass());
		}
	}

	private synchronized ActiveLock createLockInternal(LockInfo lockInfo, DavNode<?> resource) throws DavException {
		FileSystemResourceLocator locator = resource.getLocator();
		removedExpiredLocksInLocatorHierarchy(locator);

		// look for existing locks on this resource or its ancestors:
		ActiveLock existingExclusiveLock = getLock(lockInfo.getType(), Scope.EXCLUSIVE, resource);
		ActiveLock existingSharedLock = getLock(lockInfo.getType(), Scope.SHARED, resource);
		boolean hasExclusiveLock = existingExclusiveLock != null;
		boolean hasSharedLock = existingSharedLock != null;
		boolean isLocked = hasExclusiveLock || hasSharedLock;
		if ((Scope.EXCLUSIVE.equals(lockInfo.getScope()) && isLocked) || (Scope.SHARED.equals(lockInfo.getScope()) && hasExclusiveLock)) {
			throw new DavException(DavServletResponse.SC_LOCKED, "Resource (or parent resource) already locked.");
		}

		// look for locked children:
		for (Entry<FileSystemResourceLocator, Map<String, ActiveLock>> potentialChild : lockedResources.entrySet()) {
			final FileSystemResourceLocator childLocator = potentialChild.getKey();
			final Collection<ActiveLock> childLocks = potentialChild.getValue().values();
			if (isChild(locator, childLocator) && isAffectedByChildLocks(lockInfo, locator, childLocks, childLocator)) {
				throw new DavException(DavServletResponse.SC_CONFLICT, "Subresource already locked. " + childLocator);
			}
		}

		String token = DavConstants.OPAQUE_LOCK_TOKEN_PREFIX + UUID.randomUUID();
		return lockedResources.computeIfAbsent(locator, loc -> new HashMap<>()).computeIfAbsent(token, t -> new ExclusiveSharedLock(t, lockInfo));
	}

	private void removedExpiredLocksInLocatorHierarchy(FileSystemResourceLocator locator) {
		Objects.requireNonNull(locator);
		lockedResources.getOrDefault(locator, Collections.emptyMap()).values().removeIf(ActiveLock::isExpired);
		locator.parent().ifPresent(this::removedExpiredLocksInLocatorHierarchy);
	}

	private boolean isChild(FileSystemResourceLocator parent, FileSystemResourceLocator child) {
		if (parent instanceof FolderLocator) {
			FolderLocator folder = (FolderLocator) parent;
			return folder.isAncestorOf(child);
		} else {
			return false;
		}
	}

	private boolean isAffectedByChildLocks(LockInfo parentLockInfo, FileSystemResourceLocator parentLocator, Collection<ActiveLock> childLocks, FileSystemResourceLocator childLocator) {
		assert childLocator.parent().isPresent();
		for (ActiveLock lock : childLocks) {
			if (Scope.SHARED.equals(lock.getScope()) && Scope.SHARED.equals(parentLockInfo.getScope())) {
				continue;
			} else if (parentLockInfo.isDeep() || childLocator.parent().get().equals(parentLocator)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public ActiveLock refreshLock(LockInfo lockInfo, String lockToken, DavResource resource) throws DavException {
		ActiveLock lock = getLock(lockInfo.getType(), lockInfo.getScope(), resource);
		if (lock == null) {
			throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED);
		} else if (!lock.isLockedByToken(lockToken)) {
			throw new DavException(DavServletResponse.SC_LOCKED);
		}
		lock.setTimeout(lockInfo.getTimeout());
		return lock;
	}

	@Override
	public synchronized void releaseLock(String lockToken, DavResource resource) throws DavException {
		if (resource instanceof DavNode) {
			try {
				releaseLockInternal(lockToken, (DavNode<?>) resource);
			} catch (UncheckedDavException e) {
				throw e.toDavException();
			}
		} else {
			throw new IllegalArgumentException("Unsupported resource type " + resource.getClass());
		}
	}

	private synchronized void releaseLockInternal(String lockToken, DavNode<?> resource) throws UncheckedDavException {
		lockedResources.compute(resource.getLocator(), (loc, locks) -> {
			if (locks == null || locks.isEmpty()) {
				// no lock exists, nothing needs to change.
				return null;
			} else if (!locks.containsKey(lockToken)) {
				throw new UncheckedDavException(DavServletResponse.SC_LOCKED, "Resource locked with different token.");
			} else {
				locks.remove(lockToken);
				return locks.isEmpty() ? null : locks;
			}
		});
	}

	@Override
	public ActiveLock getLock(Type type, Scope scope, DavResource resource) {
		if (resource instanceof DavNode) {
			return getLockInternal(type, scope, ((DavNode<?>) resource).getLocator(), 0);
		} else {
			throw new IllegalArgumentException("Unsupported resource type " + resource.getClass());
		}
	}

	private ActiveLock getLockInternal(Type type, Scope scope, FileSystemResourceLocator locator, int depth) {
		// try to find a lock directly on this resource:
		if (lockedResources.containsKey(locator)) {
			for (ActiveLock lock : lockedResources.get(locator).values()) {
				if (type.equals(lock.getType()) && scope.equals(lock.getScope()) && (depth == 0 || lock.isDeep())) {
					return lock;
				}
			}
		}
		// or otherwise look for parent locks:
		if (locator.parent().isPresent()) {
			return getLockInternal(type, scope, locator.parent().get(), depth + 1);
		} else {
			return null;
		}
	}

	@Override
	public boolean hasLock(String lockToken, DavResource resource) {
		if (resource instanceof DavNode) {
			return hasLockInternal(lockToken, (DavNode<?>) resource);
		} else {
			throw new IllegalArgumentException("Unsupported resource type " + resource.getClass());
		}
	}

	private boolean hasLockInternal(String lockToken, DavNode<?> resource) {
		return lockedResources.getOrDefault(resource.getLocator(), Collections.emptyMap()).containsKey(lockToken);
	}

}

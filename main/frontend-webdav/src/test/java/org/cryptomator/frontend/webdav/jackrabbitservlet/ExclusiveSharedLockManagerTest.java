/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav.jackrabbitservlet;

import java.util.Optional;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.lock.ActiveLock;
import org.apache.jackrabbit.webdav.lock.LockInfo;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.Type;
import org.cryptomator.filesystem.jackrabbit.FileLocator;
import org.cryptomator.filesystem.jackrabbit.FolderLocator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ExclusiveSharedLockManagerTest {

	private FolderLocator parentLocator;
	private DavFolder parentResource;
	private FileLocator childLocator;
	private DavFile childResource;
	private LockInfo lockInfo;
	private ExclusiveSharedLockManager lockManager;

	@Before
	public void setup() {
		parentLocator = Mockito.mock(FolderLocator.class);
		Mockito.when(parentLocator.name()).thenReturn("parent");
		Mockito.when(parentLocator.parent()).thenReturn(Optional.empty());

		parentResource = Mockito.mock(DavFolder.class);
		Mockito.when(parentResource.getLocator()).thenReturn(parentLocator);

		childLocator = Mockito.mock(FileLocator.class);
		Mockito.when(childLocator.name()).thenReturn("child");
		Mockito.when(childLocator.parent()).thenReturn(Optional.of(parentLocator));
		Mockito.when(parentLocator.isAncestorOf(childLocator)).thenReturn(true);

		childResource = Mockito.mock(DavFile.class);
		Mockito.when(childResource.getLocator()).thenReturn(childLocator);

		lockInfo = Mockito.mock(LockInfo.class);
		Mockito.when(lockInfo.getScope()).thenReturn(Scope.EXCLUSIVE);
		Mockito.when(lockInfo.getType()).thenReturn(Type.WRITE);
		Mockito.when(lockInfo.getOwner()).thenReturn("test");
		Mockito.when(lockInfo.getTimeout()).thenReturn(3600l);
		Mockito.when(lockInfo.isDeep()).thenReturn(true);

		lockManager = new ExclusiveSharedLockManager();
	}

	@Test
	public void testLockCreation() throws DavException {
		ActiveLock lock = lockManager.createLock(lockInfo, parentResource);
		Assert.assertEquals(Scope.EXCLUSIVE, lock.getScope());
		Assert.assertEquals(Type.WRITE, lock.getType());
		Assert.assertEquals("test", lock.getOwner());
		Assert.assertFalse(lock.isExpired());
		Assert.assertTrue(lock.isDeep());
	}

	@Test
	public void testLockCreationInParent() throws DavException {
		ActiveLock lock = lockManager.createLock(lockInfo, parentResource);

		Assert.assertTrue(lockManager.hasLock(lock.getToken(), parentResource));
		Assert.assertFalse(lockManager.hasLock(lock.getToken(), childResource));

		ActiveLock parentLock = lockManager.getLock(Type.WRITE, Scope.EXCLUSIVE, parentResource);
		ActiveLock childLock = lockManager.getLock(Type.WRITE, Scope.EXCLUSIVE, childResource);
		Assert.assertEquals(lock, parentLock);
		Assert.assertEquals(lock, childLock);
	}

	@Test
	public void testLockCreationInChild() throws DavException {
		ActiveLock lock = lockManager.createLock(lockInfo, childResource);

		Assert.assertTrue(lockManager.hasLock(lock.getToken(), childResource));
		Assert.assertFalse(lockManager.hasLock(lock.getToken(), parentResource));

		ActiveLock parentLock = lockManager.getLock(Type.WRITE, Scope.EXCLUSIVE, parentResource);
		ActiveLock childLock = lockManager.getLock(Type.WRITE, Scope.EXCLUSIVE, childResource);
		Assert.assertNull(parentLock);
		Assert.assertEquals(lock, childLock);
	}

	@Test
	public void testMultipleSharedLocks() throws DavException {
		Mockito.when(lockInfo.getScope()).thenReturn(Scope.SHARED);

		ActiveLock lock1 = lockManager.createLock(lockInfo, parentResource);
		ActiveLock lock2 = lockManager.createLock(lockInfo, childResource);

		Assert.assertTrue(lockManager.hasLock(lock1.getToken(), parentResource));
		Assert.assertTrue(lockManager.hasLock(lock2.getToken(), childResource));

		Assert.assertNotEquals(lock1, lock2);
	}

	@Test
	public void testReleaseLock() throws DavException {
		ActiveLock lock = lockManager.createLock(lockInfo, parentResource);
		Assert.assertNotNull(lockManager.getLock(Type.WRITE, Scope.EXCLUSIVE, parentResource));

		lockManager.releaseLock(lock.getToken(), parentResource);
		Assert.assertNull(lockManager.getLock(Type.WRITE, Scope.EXCLUSIVE, parentResource));
	}

	@Test
	public void testReleaseNonExistingLock() throws DavException {
		lockManager.releaseLock("doesn't exist", parentResource);
		Assert.assertNull(lockManager.getLock(Type.WRITE, Scope.EXCLUSIVE, parentResource));
	}

	@Test
	public void testRefreshLock() throws DavException {
		ActiveLock originalLock = lockManager.createLock(lockInfo, parentResource);
		long originalTimeout = originalLock.getTimeout();

		Mockito.when(lockInfo.getTimeout()).thenReturn(7200l);
		ActiveLock updatedLock = lockManager.refreshLock(lockInfo, originalLock.getToken(), parentResource);
		long updatedTimeout = updatedLock.getTimeout();

		Assert.assertTrue(updatedTimeout > originalTimeout);
	}

	@Test(expected = DavException.class)
	public void testRefreshNonExistingLock() throws DavException {
		lockManager.refreshLock(lockInfo, "doesn't exist", parentResource);
	}

	@Test(expected = DavException.class)
	public void testRefreshLockWithInvalidToken() throws DavException {
		lockManager.createLock(lockInfo, parentResource);
		lockManager.refreshLock(lockInfo, "doesn't exist", parentResource);
	}

	@Test
	public void testLockCreationWhenParentAlreadyLocked() throws DavException {
		lockManager.createLock(lockInfo, parentResource);
		try {
			lockManager.createLock(lockInfo, childResource);
			Assert.fail("Should have thrown excpetion");
		} catch (DavException e) {
			Assert.assertEquals(DavServletResponse.SC_LOCKED, e.getErrorCode());
		}
	}

	@Test
	public void testLockCreationWhenChildAlreadyLocked() throws DavException {
		lockManager.createLock(lockInfo, childResource);
		try {
			lockManager.createLock(lockInfo, parentResource);
			Assert.fail("Should have thrown excpetion");
		} catch (DavException e) {
			Assert.assertEquals(DavServletResponse.SC_CONFLICT, e.getErrorCode());
		}
	}

}

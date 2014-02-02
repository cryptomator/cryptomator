/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package de.sebastianstenzel.oce.webdav;

import java.io.InputStream;
import java.security.Principal;

import net.sf.webdav.ITransaction;
import net.sf.webdav.IWebdavStore;
import net.sf.webdav.StoredObject;

public abstract class EnhancedWebdavStore <T extends ITransaction> implements IWebdavStore {
	
	private final Class<T> transactionClass;
	
	protected EnhancedWebdavStore(final Class<T> transactionClass) {
		this.transactionClass = transactionClass;
	}
	
	private T cast(final ITransaction transaction) {
		if (transactionClass.isAssignableFrom(transaction.getClass())) {
			return transactionClass.cast(transaction);
		} else {
			throw new IllegalStateException("transaction "  + transaction + " is not of type " + transactionClass.getName());
		}
	}
	
	abstract void destroy();

	@Override
	public final ITransaction begin(Principal principal) {
		return beginTransactionInternal(principal);
	}
	
	protected abstract T beginTransactionInternal(Principal principal);

	@Override
	public final void checkAuthentication(ITransaction transaction) {
		checkAuthenticationInternal(cast(transaction));
	}
	
	protected abstract void checkAuthenticationInternal(T transaction);

	@Override
	public void commit(ITransaction transaction) {
		commitInternal(cast(transaction));
	}
	
	protected abstract void commitInternal(T transaction);

	@Override
	public void rollback(ITransaction transaction) {
		rollbackInternal(cast(transaction));
	}
	
	protected abstract void rollbackInternal(T transaction);

	@Override
	public void createFolder(ITransaction transaction, String folderUri) {
		createFolderInternal(cast(transaction), folderUri);
	}
	
	protected abstract void createFolderInternal(T transaction, String folderUri);

	@Override
	public void createResource(ITransaction transaction, String resourceUri) {
		createResourceInternal(cast(transaction), resourceUri);
	}
	
	protected abstract void createResourceInternal(T transaction, String resourceUri);

	@Override
	public InputStream getResourceContent(ITransaction transaction, String resourceUri) {
		return getResourceContentInternal(cast(transaction), resourceUri);
	}
	
	protected abstract InputStream getResourceContentInternal(T transaction, String resourceUri);

	@Override
	public long setResourceContent(ITransaction transaction, String resourceUri, InputStream content, String contentType, String characterEncoding) {
		return setResourceContentInternal(cast(transaction), resourceUri, content, contentType, characterEncoding);
	}
	
	protected abstract long setResourceContentInternal(T transaction, String resourceUri, InputStream content, String contentType, String characterEncoding);

	@Override
	public String[] getChildrenNames(ITransaction transaction, String folderUri) {
		return getChildrenNamesInternal(cast(transaction), folderUri);
	}
	
	protected abstract String[] getChildrenNamesInternal(T transaction, String folderUri);

	@Override
	public long getResourceLength(ITransaction transaction, String path) {
		return getResourceLengthInternal(cast(transaction), path);
	}
	
	protected abstract long getResourceLengthInternal(T transaction, String path);

	@Override
	public void removeObject(ITransaction transaction, String uri) {
		removeObjectInternal(cast(transaction), uri);
	}
	
	protected abstract void removeObjectInternal(T transaction, String uri);

	@Override
	public StoredObject getStoredObject(ITransaction transaction, String uri) {
		return getStoredObjectInternal(cast(transaction), uri);
	}
	
	protected abstract StoredObject getStoredObjectInternal(T transaction, String uri);

}

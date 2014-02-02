/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package de.sebastianstenzel.oce.webdav;

import java.io.File;

import net.sf.webdav.IWebdavStore;
import net.sf.webdav.WebdavServlet;

public class EnhancedWebDavServlet extends WebdavServlet {
	
	private static final long serialVersionUID = 7198160595132838601L;
	
	private EnhancedWebdavStore<?> enhancedStore;
	
	@Override
	protected IWebdavStore constructStore(String clazzName, File root) {
		final IWebdavStore store = super.constructStore(clazzName, root);
		if (store instanceof EnhancedWebdavStore) {
			this.enhancedStore = (EnhancedWebdavStore<?>) store;
		}
		return store;
	}
	
	@Override
	public void destroy() {
		if (this.enhancedStore != null) {
			this.enhancedStore.destroy();
		}
		super.destroy();
	}

}

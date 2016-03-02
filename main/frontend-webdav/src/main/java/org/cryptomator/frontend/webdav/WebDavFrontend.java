/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import org.cryptomator.frontend.CommandFailedException;
import org.cryptomator.frontend.Frontend;
import org.cryptomator.frontend.FrontendCreationFailedException;
import org.cryptomator.frontend.webdav.mount.WebDavMount;
import org.cryptomator.frontend.webdav.mount.WebDavMounterProvider;
import org.eclipse.jetty.servlet.ServletContextHandler;

class WebDavFrontend implements Frontend {

	private final WebDavMounterProvider webdavMounterProvider;
	private final ServletContextHandler handler;
	private final URI uri;
	private WebDavMount mount;

	public WebDavFrontend(WebDavMounterProvider webdavMounterProvider, ServletContextHandler handler, URI uri) throws FrontendCreationFailedException {
		this.webdavMounterProvider = webdavMounterProvider;
		this.handler = handler;
		this.uri = uri;
		try {
			handler.start();
		} catch (Exception e) {
			throw new FrontendCreationFailedException(e);
		}
	}

	@Override
	public void close() throws Exception {
		unmount();
		handler.stop();
	}

	@Override
	public void mount(Map<MountParam, Optional<String>> mountParams) throws CommandFailedException {
		mount = webdavMounterProvider.get().mount(uri, mountParams);
	}

	@Override
	public void unmount() throws CommandFailedException {
		if (mount != null) {
			mount.unmount();
		}
	}

	@Override
	public void reveal() throws CommandFailedException {
		if (mount != null) {
			mount.reveal();
		}
	}

	@Override
	public String getWebDavUrl() {
		return uri.toString();
	}

}
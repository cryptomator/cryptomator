package org.cryptomator.frontend.webdav;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import org.cryptomator.frontend.Frontend;
import org.cryptomator.frontend.FrontendCreationFailedException;
import org.cryptomator.frontend.webdav.mount.CommandFailedException;
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
	public boolean mount(Map<MountParam, Optional<String>> mountParams) {
		try {
			mount = webdavMounterProvider.get().mount(uri, mountParams);
			return true;
		} catch (CommandFailedException e) {
			return false;
		}
	}

	@Override
	public void unmount() {
		if (mount != null) {
			try {
				mount.unmount();
			} catch (CommandFailedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void reveal() {
		if (mount != null) {
			try {
				mount.reveal();
			} catch (CommandFailedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
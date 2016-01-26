package org.cryptomator.frontend.webdav.mount;

abstract class AbstractWebDavMount implements WebDavMount {

	@Override
	public void close() throws Exception {
		this.unmount();
	}

}

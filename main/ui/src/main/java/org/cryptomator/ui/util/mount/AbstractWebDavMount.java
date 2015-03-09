package org.cryptomator.ui.util.mount;

abstract class AbstractWebDavMount implements WebDavMount {

	@Override
	public void close() throws Exception {
		this.unmount();
	}

}

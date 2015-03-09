package org.cryptomator.webdav.jackrabbit;

import org.apache.jackrabbit.webdav.property.AbstractDavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;

class HttpHeaderProperty extends AbstractDavProperty<String> {

	private final String value;

	public HttpHeaderProperty(String key, String value) {
		super(DavPropertyName.create(key), true);
		this.value = value;
	}

	@Override
	public String getValue() {
		return value;
	}

}

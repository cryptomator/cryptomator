/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.charsets;

import java.text.Normalizer.Form;

import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.delegating.DelegatingFileSystem;

public class NormalizedNameFileSystem extends NormalizedNameFolder implements DelegatingFileSystem {

	public NormalizedNameFileSystem(Folder delegate, Form displayForm) {
		super(null, delegate, displayForm);
	}

	@Override
	public Folder getDelegate() {
		return delegate;
	}

}

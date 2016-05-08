/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.charsets;

import java.io.UncheckedIOException;
import java.text.Normalizer;
import java.text.Normalizer.Form;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.delegating.DelegatingFile;

class NormalizedNameFile extends DelegatingFile<NormalizedNameFolder> {

	private final Form displayForm;

	public NormalizedNameFile(NormalizedNameFolder parent, File delegate, Form displayForm) {
		super(parent, delegate);
		this.displayForm = displayForm;
	}

	@Override
	public String name() throws UncheckedIOException {
		return Normalizer.normalize(super.name(), displayForm);
	}

}

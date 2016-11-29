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
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.delegating.DelegatingFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class NormalizedNameFolder extends DelegatingFolder<NormalizedNameFolder, NormalizedNameFile> {

	private static final Logger LOG = LoggerFactory.getLogger(NormalizedNameFolder.class);
	private final Form displayForm;

	public NormalizedNameFolder(NormalizedNameFolder parent, Folder delegate, Form displayForm) {
		super(parent, delegate);
		this.displayForm = displayForm;
	}

	@Override
	public String name() throws UncheckedIOException {
		return Normalizer.normalize(super.name(), displayForm);
	}

	@Override
	public NormalizedNameFile file(String name) throws UncheckedIOException {
		String nfcName = Normalizer.normalize(name, Form.NFC);
		String nfdName = Normalizer.normalize(name, Form.NFD);
		NormalizedNameFile nfcFile = super.file(nfcName);
		NormalizedNameFile nfdFile = super.file(nfdName);
		if (!nfcName.equals(nfdName) && nfcFile.exists() && nfdFile.exists()) {
			LOG.debug("Ambiguous file names \"" + nfcName + "\" (NFC) vs. \"" + nfdName + "\" (NFD). Both files exist. Using \"" + nfcName + "\" (NFC).");
		} else if (!nfcName.equals(nfdName) && !nfcFile.exists() && nfdFile.exists()) {
			LOG.debug("Moving file from \"" + nfcName + "\" (NFD) to \"" + nfdName + "\" (NFC).");
			nfdFile.moveTo(nfcFile);
		}
		return nfcFile;
	}

	@Override
	protected NormalizedNameFile newFile(File delegate) {
		return new NormalizedNameFile(this, delegate, displayForm);
	}

	@Override
	public NormalizedNameFolder folder(String name) throws UncheckedIOException {
		String nfcName = Normalizer.normalize(name, Form.NFC);
		String nfdName = Normalizer.normalize(name, Form.NFD);
		NormalizedNameFolder nfcFolder = super.folder(nfcName);
		NormalizedNameFolder nfdFolder = super.folder(nfdName);
		if (!nfcName.equals(nfdName) && nfcFolder.exists() && nfdFolder.exists()) {
			LOG.debug("Ambiguous folder names \"" + nfcName + "\" (NFC) vs. \"" + nfdName + "\" (NFD). Both files exist. Using \"" + nfcName + "\" (NFC).");
		} else if (!nfcName.equals(nfdName) && !nfcFolder.exists() && nfdFolder.exists()) {
			LOG.debug("Moving folder from \"" + nfcName + "\" (NFD) to \"" + nfdName + "\" (NFC).");
			nfdFolder.moveTo(nfcFolder);
		}
		return nfcFolder;
	}

	@Override
	protected NormalizedNameFolder newFolder(Folder delegate) {
		return new NormalizedNameFolder(this, delegate, displayForm);
	}

}

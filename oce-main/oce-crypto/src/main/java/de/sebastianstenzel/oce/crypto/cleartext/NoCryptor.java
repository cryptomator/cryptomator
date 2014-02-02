/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package de.sebastianstenzel.oce.crypto.cleartext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.sebastianstenzel.oce.crypto.Cryptor;
import de.sebastianstenzel.oce.crypto.TransactionAwareFileAccess;
import de.sebastianstenzel.oce.crypto.cache.PseudonymRepository;

/**
 * This Cryptor doesn't encrypting anything. It just pseudonymizes path names.
 * @deprecated Used for testing only. Will be removed soon.
 */
@Deprecated
public class NoCryptor extends Cryptor {

	private static final Logger LOG = LoggerFactory.getLogger(NoCryptor.class);
	private static String METADATA_FILENAME = "metadata.json";

	private static final char URI_PATH_SEP = '/';
	private final ObjectMapper objectMapper = new ObjectMapper();

	/* Crypting */

	@Override
	public boolean isStorage(Path path) {
		// NoCryptor doesn't depend on any special folder structure.
		return true;
	}

	@Override
	public void initializeStorage(Path path, CharSequence password) {
		// Do nothing
	}

	@Override
	public void unlockStorage(Path path, CharSequence password) {
		// Do nothing
	}

	@Override
	public long encryptFile(String pseudonymizedUri, InputStream in, TransactionAwareFileAccess accessor) throws IOException {
		final Path path = accessor.resolveUri(pseudonymizedUri);
		OutputStream out = null;
		try {
			out = accessor.openFileForWrite(path);
			return IOUtils.copyLarge(in, out);
		} finally {
			in.close();
			if (out != null) {
				out.close();
			}
		}
	}

	@Override
	public InputStream decryptFile(String pseudonymizedUri, TransactionAwareFileAccess accessor) throws IOException {
		final Path path = accessor.resolveUri(pseudonymizedUri);
		return accessor.openFileForRead(path);
	}

	@Override
	public long getDecryptedContentLength(String pseudonymizedUri, TransactionAwareFileAccess accessor) throws IOException {
		final Path path = accessor.resolveUri(pseudonymizedUri);
		return Files.size(path);
	}

	@Override
	public void swipeSensitiveData() {
		// Do nothing
	}

	/* Pseudonymizing */

	@Override
	public String createPseudonym(String cleartextUri, TransactionAwareFileAccess access) throws IOException {
		final List<String> cleartextUriComps = this.splitUri(cleartextUri);
		final List<String> pseudonymUriComps = PseudonymRepository.pseudonymizedPathComponents(cleartextUriComps);

		// return immediately if path is already known:
		if (pseudonymUriComps.size() == cleartextUriComps.size()) {
			return concatUri(pseudonymUriComps);
		}

		// append further path components otherwise:
		for (int i = pseudonymUriComps.size(); i < cleartextUriComps.size(); i++) {
			final String currentFolder = concatUri(pseudonymUriComps);
			final String cleartext = cleartextUriComps.get(i);
			String pseudonym = readPseudonymFromMetadata(access, currentFolder, cleartext);
			if (pseudonym == null) {
				pseudonym = UUID.randomUUID().toString();
				this.addToMetadata(access, currentFolder, cleartext, pseudonym);
			}
			pseudonymUriComps.add(pseudonym);
		}
		PseudonymRepository.registerPath(cleartextUriComps, pseudonymUriComps);

		return concatUri(pseudonymUriComps);
	}

	@Override
	public String uncoverPseudonym(String pseudonymizedUri, TransactionAwareFileAccess access) throws IOException {
		final List<String> pseudonymUriComps = this.splitUri(pseudonymizedUri);
		final List<String> cleartextUriComps = PseudonymRepository.cleartextPathComponents(pseudonymUriComps);

		// return immediately if path is already known:
		if (cleartextUriComps.size() == pseudonymUriComps.size()) {
			return concatUri(cleartextUriComps);
		}

		// append further path components otherwise:
		for (int i = cleartextUriComps.size(); i < pseudonymUriComps.size(); i++) {
			final String currentFolder = concatUri(pseudonymUriComps.subList(0, i));
			final String pseudonym = pseudonymUriComps.get(i);
			try {
				final String cleartext = this.readCleartextFromMetadata(access, currentFolder, pseudonym);
				if (cleartext == null) {
					return null;
				}
				cleartextUriComps.add(cleartext);
			} catch (IOException ex) {
				LOG.warn("Unresolvable pseudonym: " + currentFolder + "/" + pseudonym);
				return null;
			}
		}
		PseudonymRepository.registerPath(cleartextUriComps, pseudonymUriComps);

		return concatUri(cleartextUriComps);
	}

	@Override
	public void deletePseudonym(String pseudonymizedUri, TransactionAwareFileAccess access) throws IOException {
		// find parent folder:
		final int lastPathSeparator = pseudonymizedUri.lastIndexOf(URI_PATH_SEP);
		final String parentUri;
		if (lastPathSeparator > 0) {
			parentUri = pseudonymizedUri.substring(0, lastPathSeparator);
		} else {
			parentUri = "/";
		}

		// delete from metadata file:
		final String pseudonym = pseudonymizedUri.substring(lastPathSeparator + 1);
		final Metadata metadata = this.loadOrCreateMetadata(access, parentUri);
		metadata.getFilenames().remove(pseudonym);
		this.saveMetadata(metadata, access, parentUri);

		// delete from cache:
		final List<String> pseudonymUriComps = this.splitUri(pseudonymizedUri);
		PseudonymRepository.unregisterPath(pseudonymUriComps);
	}

	/* Metadata load & save */

	private String readPseudonymFromMetadata(TransactionAwareFileAccess access, String parentFolder, String cleartext) throws IOException {
		final Metadata metadata = loadOrCreateMetadata(access, parentFolder);
		return metadata.getFilenames().getKey(cleartext);
	}

	private String readCleartextFromMetadata(TransactionAwareFileAccess access, String parentFolder, String pseudonym) throws IOException {
		final Metadata metadata = loadOrCreateMetadata(access, parentFolder);
		return metadata.getFilenames().get(pseudonym);
	}

	private void addToMetadata(TransactionAwareFileAccess access, String parentFolder, String cleartext, String pseudonym) throws IOException {
		final Metadata metadata = loadOrCreateMetadata(access, parentFolder);
		if (!pseudonym.equals(metadata.getFilenames().getKey(cleartext))) {
			metadata.getFilenames().put(pseudonym, cleartext);
			saveMetadata(metadata, access, parentFolder);
		}
	}

	private Metadata loadOrCreateMetadata(TransactionAwareFileAccess access, String parentFolder) throws IOException {
		InputStream in = null;
		try {
			final Path path = access.resolveUri(parentFolder).resolve(METADATA_FILENAME);
			in = access.openFileForRead(path);
			return objectMapper.readValue(in, Metadata.class);
		} catch (IOException ex) {
			return new Metadata();
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	private void saveMetadata(Metadata metadata, TransactionAwareFileAccess access, String parentFolder) throws IOException {
		OutputStream out = null;
		try {
			final Path path = access.resolveUri(parentFolder).resolve(METADATA_FILENAME);
			out = access.openFileForWrite(path);
			objectMapper.writeValue(out, metadata);
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	/* utility stuff */

	private String concatUri(final List<String> uriComponents) {
		final StringBuilder sb = new StringBuilder();
		for (final String comp : uriComponents) {
			sb.append(URI_PATH_SEP).append(comp);
		}
		return sb.toString();
	}

	private List<String> splitUri(final String uri) {
		final List<String> result = new ArrayList<>();
		int begin = 0;
		int end = 0;
		do {
			end = uri.indexOf(URI_PATH_SEP, begin);
			end = (end == -1) ? uri.length() : end;
			if (end > begin) {
				result.add(uri.substring(begin, end));
			}
			begin = end + 1;
		} while (end < uri.length());
		return result;
	}

}

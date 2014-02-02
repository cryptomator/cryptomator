/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package de.sebastianstenzel.oce.crypto;

import java.io.IOException;

public interface FilenamePseudonymizing {
	
	/**
	 * Pseudonymizes and caches the given URI. If the doesn't exist yet, the new pseudonyms and its corresponding directory structure is created.
	 * @return Pseudonymized URI for the provided cleartext URI.
	 */
	String createPseudonym(String cleartextUri, TransactionAwareFileAccess accessor) throws IOException;
	
	/**
	 * Looks up the corresponding cleartext names for a given pseudonymized path.
	 * @return Cleartext URI for the provided pseudonym URI. Returns <code>null</code>, if the pseudonym can't be resolved.
	 */
	String uncoverPseudonym(String pseudonymizedUri, TransactionAwareFileAccess accessor) throws IOException;
	
	/**
	 * Deletes a pair of cleartext/pseudonym file name from the cache and metadata file.
	 */
	void deletePseudonym(String pseudonymizedUri, TransactionAwareFileAccess accessor) throws IOException;

}

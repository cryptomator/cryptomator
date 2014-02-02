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
import java.io.InputStream;
import java.nio.file.Path;

public interface StorageCrypting {
	
	/**
	 * Closes the given InputStream, when all content is encrypted.
	 */
	long encryptFile(String pseudonymizedUri, InputStream content, TransactionAwareFileAccess accessor) throws IOException;
	
	InputStream decryptFile(String pseudonymizedUri, TransactionAwareFileAccess accessor) throws IOException;
	
	long getDecryptedContentLength(String pseudonymizedUri, TransactionAwareFileAccess accessor) throws IOException;
	
	boolean isStorage(Path path);
	
	void initializeStorage(Path path, CharSequence password) throws AlreadyInitializedException, IOException;
	
	void unlockStorage(Path path, CharSequence password) throws InvalidStorageLocationException, DecryptFailedException, WrongPasswordException, UnsupportedKeyLengthException, IOException;
	
	void swipeSensitiveData();
	
	/* Exceptions */
	
	class StorageCryptingException extends Exception {
		private static final long serialVersionUID = -6622699014483319376L;
		
		public StorageCryptingException(String string) {
			super(string);
		}
		
		public StorageCryptingException(String string, Throwable t) {
			super(string, t);
		}
	}

	class AlreadyInitializedException extends StorageCryptingException {
		private static final long serialVersionUID = -8928660250898037968L;
		
		public AlreadyInitializedException(Path path) {
			super(path.toString() + " already contains a vault.");
		}
	}
	
	class InvalidStorageLocationException extends StorageCryptingException {
		private static final long serialVersionUID = -967813718181720188L;

		public InvalidStorageLocationException(Path path) {
			super("Can't read vault in path " + path.toString());
		}
	}

	class WrongPasswordException extends StorageCryptingException {
		private static final long serialVersionUID = -602047799678568780L;

		public WrongPasswordException() {
			super("Wrong password.");
		}
	}
	
	class DecryptFailedException extends StorageCryptingException {
		private static final long serialVersionUID = -3855673600374897828L;

		public DecryptFailedException(Throwable t) {
			super("Decryption failed.", t);
		}
	}
	
	class UnsupportedKeyLengthException extends StorageCryptingException {
		private static final long serialVersionUID = 8114147446419390179L;
		
		public UnsupportedKeyLengthException(int length, int maxLength) {
			super(String.format("Key length (%i) exceeds policy maximum (%i).", length, maxLength));
		}
		
	}

}



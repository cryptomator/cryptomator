package org.cryptomator.frontend;

import org.cryptomator.filesystem.Folder;

public interface FrontendFactory {

	/**
	 * Provides a new frontend to access the given folder.
	 * 
	 * @param root Root resource accessible through this frontend.
	 * @param uniqueName Name of the frontend, i.e. used to create subresources for the different frontends inside of a common virtual drive.
	 * @return A new frontend
	 * @throws FrontendCreationFailedException If creation was not possible.
	 */
	Frontend create(Folder root, String uniqueName) throws FrontendCreationFailedException;

}

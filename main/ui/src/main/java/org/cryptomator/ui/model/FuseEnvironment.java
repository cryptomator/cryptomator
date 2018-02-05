package org.cryptomator.ui.model;

import java.nio.file.Path;

public interface FuseEnvironment {

	void prepare() throws CommandFailedException;

	String[] getMountParameters() throws CommandFailedException;

	Path getFsRootPath();

	/**
	 * TODO: implement it in subclasses!
	 * @throws CommandFailedException
	 */
	default void revealFsRootInFilesystemManager() throws CommandFailedException {
		throw new CommandFailedException("Not implemented.");
	}

	void cleanUp();

	default boolean supportsFuse(){
		return false;
	}


}

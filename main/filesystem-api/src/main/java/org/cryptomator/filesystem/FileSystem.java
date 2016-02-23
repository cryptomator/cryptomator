/*******************************************************************************
 * Copyright (c) 2015 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 ******************************************************************************/
package org.cryptomator.filesystem;

import java.util.Optional;

/**
 * The root folder of a file system.
 * 
 * @author Markus Kreusch
 */
public interface FileSystem extends Folder {

	/**
	 * @return an empty {@link Optional} because a {@link FileSystem} represents
	 *         the root {@link Folder} and thus does not have a parent
	 */
	@Override
	default Optional<? extends Folder> parent() {
		return Optional.empty();
	}

	Optional<Long> quotaUsedBytes();

	Optional<Long> quotaAvailableBytes();

}

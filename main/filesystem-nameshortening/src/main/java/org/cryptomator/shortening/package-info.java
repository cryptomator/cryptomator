/**
 * Provides a decoration layer for the {@link org.cryptomator.filesystem Filesystem API}.
 * {@link org.cryptomator.filesystem.File File} and {@link org.cryptomator.filesystem.Folder Folder} names exceeding a certain length limit will be mapped to shorter equivalents.
 * The mapping itself is stored in metadata files inside the <code>m/</code> directory on root level.
 */
package org.cryptomator.shortening;
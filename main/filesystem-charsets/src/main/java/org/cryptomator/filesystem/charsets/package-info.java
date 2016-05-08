/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
/**
 * Makes sure, the filesystems wrapped by this filesystem work only on UTF-8 encoded file paths using Normalization Form C.
 * Filesystems wrapping this file system, on the other hand, will get filenames reported in a specified Normalization Form.
 * It is recommended to use NFD for OS X and NFC for other operating systems.
 * When looking for a file or folder with a name given in either form, both possibilities are considered
 * and files/folders stored in NFD are automatically migrated to NFC.
 */
package org.cryptomator.filesystem.charsets;
/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
/**
 * Provides a decoration layer for the {@link org.cryptomator.filesystem Filesystem API}, consuming an encrypted file system and providing access to a cleartext filesystem.
 * While the implementation in this package dictates the Vault directory layout, no encryption code can be found here.
 * All cryptographic operations are delegated to the {@link org.cryptomator.crypto.engine CryptoEngine}.
 */
package org.cryptomator.filesystem.crypto;
/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.keychain;

interface KeychainAccessStrategy extends KeychainAccess {

	/**
	 * @return <code>true</code> if this KeychainAccessStrategy works on the current machine.
	 */
	boolean isSupported();

}

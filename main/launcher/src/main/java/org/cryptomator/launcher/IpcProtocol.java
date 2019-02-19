/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.launcher;

import java.rmi.Remote;
import java.rmi.RemoteException;

interface IpcProtocol extends Remote {

	void handleLaunchArgs(String[] args) throws RemoteException;

}
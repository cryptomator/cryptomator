/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.common.vaults;

import dagger.Module;
import dagger.Provides;
import org.cryptomator.common.Nullable;
import org.cryptomator.cryptofs.CryptoFileSystem;

import javax.inject.Named;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import java.util.concurrent.atomic.AtomicReference;

@Module
public class VaultModule {

	@Provides
	@PerVault
	public AtomicReference<CryptoFileSystem> provideCryptoFileSystemReference() {
		return new AtomicReference<>();
	}

	@Provides
	@Named("lastKnownException")
	@PerVault
	public ObjectProperty<Exception> provideLastKnownException(@Named("lastKnownException") @Nullable Exception initialErrorCause) {
		return new SimpleObjectProperty<>(initialErrorCause);
	}

}

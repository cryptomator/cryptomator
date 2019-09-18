/*******************************************************************************
 * Copyright (c) 2019 Skymatic GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.common;

import dagger.Module;
import dagger.Provides;
import org.cryptomator.jni.JniFunctions;
import org.cryptomator.jni.MacFunctions;
import org.cryptomator.jni.WinFunctions;

import javax.inject.Singleton;
import java.util.Optional;

@Module
public class JniModule {

	@Provides
	@Singleton
	Optional<MacFunctions> provideOptionalMacFunctions() {
		return JniFunctions.macFunctions();
	}

	@Provides
	@Singleton
	Optional<WinFunctions> provideOptionalWinFunctions() {
		return JniFunctions.winFunctions();
	}

}

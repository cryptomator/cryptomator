/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.fxapp;

import dagger.BindsInstance;
import dagger.Subcomponent;

import javax.inject.Named;

@FxApplicationScoped
@Subcomponent(modules = FxApplicationModule.class)
public interface FxApplicationComponent {

	FxApplication application();

	@Subcomponent.Builder
	interface Builder {

		@BindsInstance
		Builder trayMenuSupported(@Named("trayMenuSupported") boolean trayMenuSupported);

		FxApplicationComponent build();
	}

}

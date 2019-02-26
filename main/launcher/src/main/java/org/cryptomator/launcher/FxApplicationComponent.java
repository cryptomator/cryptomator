/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.launcher;

import dagger.Subcomponent;
import org.cryptomator.common.FxApplicationScoped;

@FxApplicationScoped
@Subcomponent(modules = FxApplicationModule.class)
interface FxApplicationComponent {

	FxApplication application();

}

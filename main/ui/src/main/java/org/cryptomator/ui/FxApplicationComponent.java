/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui;

import dagger.Subcomponent;
import javafx.application.Platform;

@FxApplicationScoped
@Subcomponent(modules = FxApplicationModule.class)
public interface FxApplicationComponent {

	FxApplication application();
	
	default void start() {
		Platform.startup(() -> {
			assert Platform.isFxApplicationThread();
			application().start();
		});
	}

}

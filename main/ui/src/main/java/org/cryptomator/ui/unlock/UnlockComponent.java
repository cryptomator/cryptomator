/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.unlock;

import dagger.BindsInstance;
import dagger.Lazy;
import dagger.Subcomponent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.common.vaults.Vault;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

@UnlockScoped
@Subcomponent(modules = {UnlockModule.class})
public interface UnlockComponent {

	ExecutorService defaultExecutorService();

	UnlockWorkflow unlockWorkflow();
	
	default Future<Boolean> startUnlockWorkflow() {
		UnlockWorkflow workflow = unlockWorkflow();
		defaultExecutorService().submit(workflow);
		return workflow;
	}

	@Subcomponent.Builder
	interface Builder {
		
		@BindsInstance
		Builder vault(@UnlockWindow Vault vault);

		UnlockComponent build();
	}

}

/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.unlock;

import dagger.BindsInstance;
import dagger.Subcomponent;
import org.cryptomator.common.vaults.Vault;

import javax.inject.Named;
import javafx.stage.Stage;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

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

		@BindsInstance
		Builder owner(@Named("unlockWindowOwner") Optional<Stage> owner);

		UnlockComponent build();
	}

}

/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.unlock;

import dagger.BindsInstance;
import dagger.Subcomponent;
import org.cryptomator.common.Nullable;
import org.cryptomator.common.vaults.Vault;

import javax.inject.Named;
import javafx.stage.Stage;

@UnlockScoped
@Subcomponent(modules = {UnlockModule.class})
public interface UnlockComponent {

	UnlockWorkflow unlockWorkflow();

	@Subcomponent.Factory
	interface Factory {
		UnlockComponent create(@BindsInstance @UnlockWindow Vault vault, @BindsInstance @Named("unlockWindowOwner") @Nullable Stage owner);
	}

}

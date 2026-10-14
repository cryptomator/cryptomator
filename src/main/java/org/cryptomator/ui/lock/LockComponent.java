package org.cryptomator.ui.lock;

import dagger.BindsInstance;
import dagger.Subcomponent;
import org.cryptomator.common.Nullable;
import org.cryptomator.common.vaults.Vault;

import javax.inject.Named;
import javafx.stage.Stage;


@LockScoped
@Subcomponent(modules = {LockModule.class})
public interface LockComponent {

	LockWorkflow lockWorkflow();

	@Subcomponent.Factory
	interface Factory {
		LockComponent create(@BindsInstance @LockWindow Vault vault, @BindsInstance @Named("lockWindowOwner") @Nullable Stage owner);
	}

}

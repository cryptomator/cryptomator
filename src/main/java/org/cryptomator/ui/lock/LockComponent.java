package org.cryptomator.ui.lock;

import dagger.BindsInstance;
import dagger.Subcomponent;
import org.cryptomator.common.Nullable;
import org.cryptomator.common.vaults.Vault;

import javax.inject.Named;
import javafx.stage.Stage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;


@LockScoped
@Subcomponent(modules = {LockModule.class})
public interface LockComponent {

	ExecutorService defaultExecutorService();

	LockWorkflow lockWorkflow();

	default Future<Void> startLockWorkflow() {
		LockWorkflow workflow = lockWorkflow();
		defaultExecutorService().submit(workflow);
		return workflow;
	}

	@Subcomponent.Factory
	interface Factory {
		LockComponent create(@BindsInstance @LockWindow Vault vault, @BindsInstance @Named("lockWindowOwner") @Nullable Stage owner);
	}

}

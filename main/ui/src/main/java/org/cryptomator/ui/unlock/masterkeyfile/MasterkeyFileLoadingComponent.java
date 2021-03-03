package org.cryptomator.ui.unlock.masterkeyfile;

import dagger.BindsInstance;
import dagger.Subcomponent;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptolib.api.MasterkeyLoadingFailedException;
import org.cryptomator.cryptolib.common.MasterkeyFileLoader;
import org.cryptomator.ui.unlock.KeyLoadingComponent;

import javafx.stage.Stage;

@MasterkeyFileLoadingScoped
@Subcomponent(modules = {MasterkeyFileLoadingModule.class})
public interface MasterkeyFileLoadingComponent extends KeyLoadingComponent {

	MasterkeyFileLoadingFinisher finisher();

	MasterkeyFileLoadingContext context();

	@Override
	MasterkeyFileLoader masterkeyLoader();

	@Override
	default boolean recoverFromException(MasterkeyLoadingFailedException exception) {
		return context().recoverFromException(exception);
	}

	@Override
	default void cleanup(boolean unlockedSuccessfully) {
		finisher().cleanup(unlockedSuccessfully);
	}

	@Subcomponent.Builder
	interface Builder {

		@BindsInstance
		Builder vault(@MasterkeyFileLoading Vault vault);

		@BindsInstance
		Builder unlockWindow(@MasterkeyFileLoading Stage unlockWindow);

		MasterkeyFileLoadingComponent build();
	}

}

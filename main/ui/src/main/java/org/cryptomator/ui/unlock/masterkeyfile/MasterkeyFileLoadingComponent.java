package org.cryptomator.ui.unlock.masterkeyfile;

import dagger.BindsInstance;
import dagger.Subcomponent;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptolib.common.MasterkeyFileLoader;

import javax.annotation.Nullable;
import javafx.stage.Stage;

@MasterkeyFileLoadingScoped
@Subcomponent(modules = {MasterkeyFileLoadingModule.class})
public interface MasterkeyFileLoadingComponent {

	MasterkeyFileLoadingFinisher finisher();

	MasterkeyFileLoader masterkeyLoader();

	default void cleanup(boolean unlockedSuccessfully) {
		finisher().cleanup(unlockedSuccessfully);
	}

	@Subcomponent.Builder
	interface Builder {

		@BindsInstance
		Builder previousError(@Nullable Exception previousError);

		@BindsInstance
		Builder vault(@MasterkeyFileLoading Vault vault);

		@BindsInstance
		Builder unlockWindow(@MasterkeyFileLoading Stage unlockWindow);

		MasterkeyFileLoadingComponent build();
	}

}

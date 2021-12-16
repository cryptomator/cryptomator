package org.cryptomator.ui.keyloading.masterkeyfile;

import dagger.BindsInstance;
import dagger.Subcomponent;
import org.cryptomator.common.Nullable;
import org.cryptomator.common.Passphrase;

import javax.inject.Named;
import javafx.scene.Scene;
import java.util.concurrent.CompletableFuture;

@PassphraseEntryScoped
@Subcomponent(modules = {PassphraseEntryModule.class})
public interface PassphraseEntryComponent {

	@PassphraseEntryScoped
	Scene passphraseEntryScene();

	@PassphraseEntryScoped
	CompletableFuture<PassphraseEntryResult> result();

	@Subcomponent.Builder
	interface Builder {

		@BindsInstance
		PassphraseEntryComponent.Builder savedPassword(@Nullable @Named("savedPassword") Passphrase savedPassword);

		PassphraseEntryComponent build();
	}

}

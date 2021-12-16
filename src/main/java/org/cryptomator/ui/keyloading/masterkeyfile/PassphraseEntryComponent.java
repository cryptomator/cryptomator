package org.cryptomator.ui.keyloading.masterkeyfile;

import dagger.BindsInstance;
import dagger.Subcomponent;
import org.cryptomator.common.Nullable;
import org.cryptomator.ui.common.Animations;

import javax.inject.Named;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
		PassphraseEntryComponent.Builder savedPassword(@Nullable @Named("savedPassword") char[] savedPassword);

		PassphraseEntryComponent build();
	}

}

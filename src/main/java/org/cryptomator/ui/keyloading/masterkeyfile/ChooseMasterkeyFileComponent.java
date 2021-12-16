package org.cryptomator.ui.keyloading.masterkeyfile;

import dagger.Subcomponent;

import javafx.scene.Scene;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

@ChooseMasterkeyFileScoped
@Subcomponent(modules = {ChooseMasterkeyFileModule.class})
public interface ChooseMasterkeyFileComponent {

	@ChooseMasterkeyFileScoped
	Scene chooseMasterkeyScene();

	@ChooseMasterkeyFileScoped
	CompletableFuture<Path> result();

	@Subcomponent.Builder
	interface Builder {

		ChooseMasterkeyFileComponent build();
	}

}

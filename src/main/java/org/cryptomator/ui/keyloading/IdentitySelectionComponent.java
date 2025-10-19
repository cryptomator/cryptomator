package org.cryptomator.ui.keyloading;

import dagger.Subcomponent;
import org.cryptomator.common.vaults.VaultIdentity;

import javafx.scene.Scene;
import java.util.concurrent.CompletableFuture;

@IdentitySelectionScoped
@Subcomponent(modules = {IdentitySelectionModule.class})
public interface IdentitySelectionComponent {

	@IdentitySelectionScoped
	Scene identitySelectionScene();

	@IdentitySelectionScoped
	CompletableFuture<VaultIdentity> result();

	@Subcomponent.Builder
	interface Builder {

		IdentitySelectionComponent build();
	}

}

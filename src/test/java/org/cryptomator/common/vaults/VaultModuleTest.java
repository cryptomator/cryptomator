package org.cryptomator.common.vaults;

import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.VaultSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import java.nio.file.Path;

public class VaultModuleTest {

	private final Settings settings = Mockito.mock(Settings.class);
	private final VaultSettings vaultSettings = Mockito.mock(VaultSettings.class);

	private final VaultModule module = new VaultModule();

	@BeforeEach
	public void setup(@TempDir Path tmpDir) {
		Mockito.when(vaultSettings.mountName()).thenReturn(Bindings.createStringBinding(() -> "TEST"));
		Mockito.when(vaultSettings.usesReadOnlyMode()).thenReturn(new SimpleBooleanProperty(true));
		Mockito.when(vaultSettings.displayName()).thenReturn(new SimpleStringProperty("Vault"));
		System.setProperty("user.home", tmpDir.toString());
	}

}

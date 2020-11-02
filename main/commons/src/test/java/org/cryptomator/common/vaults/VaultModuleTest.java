package org.cryptomator.common.vaults;

import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.settings.VolumeImpl;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import java.nio.file.Path;

public class VaultModuleTest {

	private final Settings settings = Mockito.mock(Settings.class);
	private final VaultSettings vaultSettings = Mockito.mock(VaultSettings.class);

	private final VaultModule module = new VaultModule();

	@BeforeEach
	public void setup(@TempDir Path tmpDir) {
		Mockito.when(vaultSettings.mountName()).thenReturn(Bindings.createStringBinding(() -> "TEST"));
		Mockito.when(vaultSettings.usesReadOnlyMode()).thenReturn(new SimpleBooleanProperty(true));
		System.setProperty("user.home", tmpDir.toString());
	}

	@Test
	@DisplayName("provideDefaultMountFlags on Mac/FUSE")
	@EnabledOnOs(OS.MAC)
	public void testMacFuseDefaultMountFlags() {
		Mockito.when(settings.preferredVolumeImpl()).thenReturn(new SimpleObjectProperty<>(VolumeImpl.FUSE));

		StringBinding result = module.provideDefaultMountFlags(settings, vaultSettings);

		MatcherAssert.assertThat(result.get(), CoreMatchers.containsString("-ovolname=TEST"));
		MatcherAssert.assertThat(result.get(), CoreMatchers.containsString("-ordonly"));
	}

	@Test
	@DisplayName("provideDefaultMountFlags on Linux/FUSE")
	@EnabledOnOs(OS.LINUX)
	public void testLinuxFuseDefaultMountFlags() {
		Mockito.when(settings.preferredVolumeImpl()).thenReturn(new SimpleObjectProperty<>(VolumeImpl.FUSE));

		StringBinding result = module.provideDefaultMountFlags(settings, vaultSettings);

		MatcherAssert.assertThat(result.get(), CoreMatchers.containsString("-oro"));
	}

	@Test
	@DisplayName("provideDefaultMountFlags on Windows/Dokany")
	@EnabledOnOs(OS.WINDOWS)
	public void testWinDokanyDefaultMountFlags() {
		Mockito.when(settings.preferredVolumeImpl()).thenReturn(new SimpleObjectProperty<>(VolumeImpl.DOKANY));

		StringBinding result = module.provideDefaultMountFlags(settings, vaultSettings);

		MatcherAssert.assertThat(result.get(), CoreMatchers.containsString("--options CURRENT_SESSION,WRITE_PROTECTION"));
	}

}

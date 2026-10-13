package org.cryptomator.common.mount;

import org.cryptomator.common.Environment;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.SettingsProvider;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.integrations.mount.MountBuilder;
import org.cryptomator.integrations.mount.MountCapability;
import org.cryptomator.integrations.mount.MountService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import javafx.beans.property.SimpleObjectProperty;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

class MounterTest {

	@TempDir
	Path tmpDir;

	private Mounter.MountHandle mount(Set<MountCapability> capabilities) throws Exception {
		var mountService = Mockito.mock(MountService.class);
		Mockito.when(mountService.capabilities()).thenReturn(capabilities);
		Mockito.when(mountService.hasCapability(Mockito.any())).thenAnswer(invocation -> capabilities.contains(invocation.getArgument(0, MountCapability.class)));
		Mockito.when(mountService.forFileSystem(Mockito.any())).thenReturn(Mockito.mock(MountBuilder.class));
		var env = Mockito.mock(Environment.class);
		Mockito.when(env.getMountPointsDir()).thenReturn(Optional.of(tmpDir));
		var settings = Settings.create(Mockito.mock(SettingsProvider.class), env);
		var mounter = new Mounter(env, settings, Mockito.mock(WindowsDriveLetters.class), List.of(mountService), new HashSet<>(), new SimpleObjectProperty<>(mountService));
		return mounter.mount(VaultSettings.withRandomId(), tmpDir);
	}

	@Test
	public void testMountHandleReportsWhenTerminated() throws Exception {
		var mountHandle = mount(EnumSet.of(MountCapability.MOUNT_TO_SYSTEM_CHOSEN_PATH, MountCapability.WHEN_TERMINATED));

		Assertions.assertTrue(mountHandle.supportsWhenTerminated());
		Assertions.assertFalse(mountHandle.supportsUnmountForced());
	}

	@Test
	public void testMountHandleWithoutWhenTerminated() throws Exception {
		var mountHandle = mount(EnumSet.of(MountCapability.MOUNT_TO_SYSTEM_CHOSEN_PATH, MountCapability.UNMOUNT_FORCED));

		Assertions.assertFalse(mountHandle.supportsWhenTerminated());
		Assertions.assertTrue(mountHandle.supportsUnmountForced());
	}

}

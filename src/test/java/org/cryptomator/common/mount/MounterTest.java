package org.cryptomator.common.mount;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.cryptomator.common.Environment;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.SettingsProvider;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.integrations.mount.MountBuilder;
import org.cryptomator.integrations.mount.MountCapability;
import org.cryptomator.integrations.mount.MountFailedException;
import org.cryptomator.integrations.mount.MountService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
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
		return mount(capabilities, Mockito.mock(MountBuilder.class));
	}

	private Mounter.MountHandle mountFailingWith(MountFailedException exception) throws Exception {
		var builder = Mockito.mock(MountBuilder.class);
		Mockito.when(builder.mount()).thenThrow(exception);
		return mount(EnumSet.of(MountCapability.MOUNT_TO_SYSTEM_CHOSEN_PATH), builder);
	}

	private Mounter.MountHandle mount(Set<MountCapability> capabilities, MountBuilder builder) throws Exception {
		var mountService = Mockito.mock(MountService.class);
		Mockito.when(mountService.capabilities()).thenReturn(capabilities);
		Mockito.when(mountService.hasCapability(Mockito.any())).thenAnswer(invocation -> capabilities.contains(invocation.getArgument(0, MountCapability.class)));
		Mockito.when(mountService.forFileSystem(Mockito.any())).thenReturn(builder);
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

	@Test
	public void testMountTranslatesExtensionSwitchedOff() throws Exception {
		var exceptionClass = new ByteBuddy().subclass(MountFailedException.class) //
				.name(Mounter.FSKIT_EXTENSION_SWITCHED_OFF_EXCEPTION) //
				.make() //
				.load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST) //
				.getLoaded();
		var extensionSwitchedOff = exceptionClass.getConstructor(String.class).newInstance("switched off");

		var thrown = Assertions.assertThrows(FSKitExtensionSwitchedOffException.class, () -> mountFailingWith(extensionSwitchedOff));

		Assertions.assertSame(extensionSwitchedOff, thrown.getCause());
	}

	@Test
	public void testMountRethrowsOtherMountFailures() {
		var mountFailed = new MountFailedException("failed");

		var thrown = Assertions.assertThrows(MountFailedException.class, () -> mountFailingWith(mountFailed));

		Assertions.assertSame(mountFailed, thrown);
	}

	@Test
	@EnabledOnOs(OS.MAC)
	public void testExtensionSwitchedOffExceptionExistsInAdapter() throws Exception {
		var exceptionClass = Class.forName(Mounter.FSKIT_EXTENSION_SWITCHED_OFF_EXCEPTION);

		Assertions.assertTrue(MountFailedException.class.isAssignableFrom(exceptionClass));
	}

}

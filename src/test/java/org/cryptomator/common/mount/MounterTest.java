package org.cryptomator.common.mount;

import org.cryptomator.common.Environment;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.SettingsProvider;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.integrations.mount.Mount;
import org.cryptomator.integrations.mount.MountBuilder;
import org.cryptomator.integrations.mount.MountCapability;
import org.cryptomator.integrations.mount.MountFailedException;
import org.cryptomator.integrations.mount.MountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import javafx.beans.property.SimpleObjectProperty;
import java.io.IOException;
import java.net.BindException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MounterTest {

	@Test
	void wrapsBindExceptionAsPortAlreadyInUseException(@TempDir Path mountPointsDir) {
		var mountFailedException = new MountFailedException("Failed to start server", new BindException("Address already in use"));
		var service = new FailingLoopbackMountService(mountFailedException);
		var mounter = mounterWith(service, mountPointsDir);

		var exception = assertThrows(PortAlreadyInUseException.class, () -> mounter.mount(VaultSettings.withRandomId(), mountPointsDir));

		assertEquals(42427, exception.getPort());
		assertTrue(exception.isDefaultPort());
		assertSame(mountFailedException, exception.getCause());
	}

	@Test
	void detectsSuppressedBindException(@TempDir Path mountPointsDir) {
		var serverFailure = new IOException("Server failed to start");
		serverFailure.addSuppressed(new BindException("Address already in use"));
		var mountFailedException = new MountFailedException("Failed to start server", serverFailure);
		var service = new FailingLoopbackMountService(mountFailedException);
		var mounter = mounterWith(service, mountPointsDir);

		var exception = assertThrows(PortAlreadyInUseException.class, () -> mounter.mount(VaultSettings.withRandomId(), mountPointsDir));

		assertEquals(42427, exception.getPort());
		assertSame(mountFailedException, exception.getCause());
	}

	@Test
	void keepsOtherMountFailuresUnchanged(@TempDir Path mountPointsDir) {
		var mountFailedException = new MountFailedException("Mounting failed", new IOException("Some other failure"));
		var service = new FailingLoopbackMountService(mountFailedException);
		var mounter = mounterWith(service, mountPointsDir);

		var exception = assertThrows(MountFailedException.class, () -> mounter.mount(VaultSettings.withRandomId(), mountPointsDir));

		assertSame(mountFailedException, exception);
	}

	private static Mounter mounterWith(MountService service, Path mountPointsDir) {
		var env = Mockito.mock(Environment.class);
		Mockito.when(env.getLoopbackAlias()).thenReturn(Optional.empty());
		Mockito.when(env.getMountPointsDir()).thenReturn(Optional.of(mountPointsDir));
		var settings = Settings.create(Mockito.mock(SettingsProvider.class), Mockito.mock(Environment.class));
		return new Mounter(env, settings, Mockito.mock(WindowsDriveLetters.class), List.of(service), ConcurrentHashMap.newKeySet(), new SimpleObjectProperty<>(service));
	}

	private record FailingLoopbackMountService(MountFailedException exception) implements MountService {

		@Override
		public String displayName() {
			return "Failing Loopback";
		}

		@Override
		public boolean isSupported() {
			return true;
		}

		@Override
		public Set<MountCapability> capabilities() {
			return Set.of(MountCapability.LOOPBACK_PORT, MountCapability.MOUNT_TO_SYSTEM_CHOSEN_PATH);
		}

		@Override
		public MountBuilder forFileSystem(Path path) {
			return new MountBuilder() {
				@Override
				public MountBuilder setLoopbackPort(int port) {
					return this;
				}

				@Override
				public Mount mount() throws MountFailedException {
					throw exception;
				}
			};
		}
	}
}

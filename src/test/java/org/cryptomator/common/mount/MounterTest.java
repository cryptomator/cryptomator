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

/**
 * Tests translation of low-level loopback mount failures into user-facing mount exceptions.
 */
class MounterTest {

	/**
	 * Verifies that a direct {@link BindException} becomes a typed port conflict.
	 */
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

	/**
	 * Verifies that suppressed binding failures are detected as port conflicts.
	 */
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

	/**
	 * Verifies that unrelated mount failures keep their original exception type.
	 */
	@Test
	void keepsOtherMountFailuresUnchanged(@TempDir Path mountPointsDir) {
		var mountFailedException = new MountFailedException("Mounting failed", new IOException("Some other failure"));
		var service = new FailingLoopbackMountService(mountFailedException);
		var mounter = mounterWith(service, mountPointsDir);

		var exception = assertThrows(MountFailedException.class, () -> mounter.mount(VaultSettings.withRandomId(), mountPointsDir));

		assertSame(mountFailedException, exception);
	}

	/**
	 * Verifies that bind failures unrelated to occupied ports keep their original exception type.
	 */
	@Test
	void keepsNonPortCollisionBindFailuresUnchanged(@TempDir Path mountPointsDir) {
		var mountFailedException = new MountFailedException("Failed to start server", new BindException("Cannot assign requested address"));
		var service = new FailingLoopbackMountService(mountFailedException);
		var mounter = mounterWith(service, mountPointsDir);

		var exception = assertThrows(MountFailedException.class, () -> mounter.mount(VaultSettings.withRandomId(), mountPointsDir));

		assertSame(mountFailedException, exception);
	}

	/**
	 * Creates a mounter with one fake mount service and a deterministic mount points directory.
	 */
	private static Mounter mounterWith(MountService service, Path mountPointsDir) {
		var env = Mockito.mock(Environment.class);
		Mockito.when(env.getLoopbackAlias()).thenReturn(Optional.empty());
		Mockito.when(env.getMountPointsDir()).thenReturn(Optional.of(mountPointsDir));
		var settings = Settings.create(Mockito.mock(SettingsProvider.class), Mockito.mock(Environment.class));
		return new Mounter(env, settings, Mockito.mock(WindowsDriveLetters.class), List.of(service), ConcurrentHashMap.newKeySet(), new SimpleObjectProperty<>(service));
	}

	/**
	 * Mount service whose builder always fails with the provided exception.
	 */
	private record FailingLoopbackMountService(MountFailedException exception) implements MountService {

		/**
		 * @return A display name for this fake mount service.
		 */
		@Override
		public String displayName() {
			return "Failing Loopback";
		}

		/**
		 * @return {@code true}, because the fake service is always available in tests.
		 */
		@Override
		public boolean isSupported() {
			return true;
		}

		/**
		 * @return The capabilities required to exercise loopback port handling without mount point setup.
		 */
		@Override
		public Set<MountCapability> capabilities() {
			return Set.of(MountCapability.LOOPBACK_PORT, MountCapability.MOUNT_TO_SYSTEM_CHOSEN_PATH);
		}

		/**
		 * @return A builder that records accepted port configuration and fails during mounting.
		 */
		@Override
		public MountBuilder forFileSystem(Path path) {
			return new MountBuilder() {
				/**
				 * Accepts the configured loopback port for the test builder.
				 *
				 * @return This builder.
				 */
				@Override
				public MountBuilder setLoopbackPort(int port) {
					return this;
				}

				/**
				 * Fails with the exception supplied to the fake service.
				 */
				@Override
				public Mount mount() throws MountFailedException {
					throw exception;
				}
			};
		}
	}
}

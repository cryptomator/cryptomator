package org.cryptomator.common.mountpoint;

import com.google.common.collect.ImmutableSet;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import org.cryptomator.common.Environment;
import org.cryptomator.common.mountpoint.MountPointChooser.Phase;
import org.cryptomator.common.vaults.PerVault;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.WindowsDriveLetters;

import javax.inject.Named;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

/**
 * Dagger-Module for {@link MountPointChooser MountPointChoosers.}<br>
 * See there for additional information.
 *
 * @see MountPointChooser
 */
@Module
public abstract class MountPointChooserModule {

	@Provides
	@IntoMap
	@MountPointChooser.PhaseKey(Phase.CUSTOM_MOUNTPOINT)
	@PerVault
	public static MountPointChooser provideCustomMountPointChooser(Vault vault) {
		return new CustomMountPointChooser(vault);
	}

	@Provides
	@IntoMap
	@MountPointChooser.PhaseKey(Phase.CUSTOM_DRIVELETTER)
	@PerVault
	public static MountPointChooser provideCustomDriveLetterChooser(Vault vault) {
		return new CustomDriveLetterChooser(vault);
	}

	@Provides
	@IntoMap
	@MountPointChooser.PhaseKey(Phase.AVAILABLE_DRIVELETTER)
	@PerVault
	public static MountPointChooser provideAvailableDriveLetterChooser(WindowsDriveLetters windowsDriveLetters) {
		return new AvailableDriveLetterChooser(windowsDriveLetters);
	}

	@Provides
	@IntoMap
	@MountPointChooser.PhaseKey(Phase.TEMPORARY_MOUNTPOINT)
	@PerVault
	public static MountPointChooser provideTemporaryMountPointChooser(Vault vault, Environment environment) {
		return new TemporaryMountPointChooser(vault, environment);
	}

	@Provides
	@PerVault
	@Named("orderedValidMountPointChoosers")
	public static Set<MountPointChooser> provideOrderedValidMountPointChoosers(Map<Phase, MountPointChooser> chooserMap) {
		//Sorted Set
		return chooserMap.entrySet().stream()
				.sorted(Comparator.comparingInt(value -> value.getKey().getTiming()))
				.map(Map.Entry::getValue)
				.filter(MountPointChooser::isApplicable)
				.collect(ImmutableSet.toImmutableSet());
	}
}

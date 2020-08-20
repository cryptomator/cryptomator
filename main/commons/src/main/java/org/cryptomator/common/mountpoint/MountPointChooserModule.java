package org.cryptomator.common.mountpoint;

import com.google.common.collect.ImmutableSet;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import org.cryptomator.common.Environment;
import org.cryptomator.common.vaults.PerVault;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.WindowsDriveLetters;

import javax.inject.Named;
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
	@IntoSet
	@PerVault
	public static MountPointChooser provideCustomMountPointChooser(Vault vault) {
		return new CustomMountPointChooser(vault);
	}

	@Provides
	@IntoSet
	@PerVault
	public static MountPointChooser provideCustomDriveLetterChooser(Vault vault) {
		return new CustomDriveLetterChooser(vault);
	}

	@Provides
	@IntoSet
	@PerVault
	public static MountPointChooser provideAvailableDriveLetterChooser(WindowsDriveLetters windowsDriveLetters) {
		return new AvailableDriveLetterChooser(windowsDriveLetters);
	}

	@Provides
	@IntoSet
	@PerVault
	public static MountPointChooser provideTemporaryMountPointChooser(Vault vault, Environment environment) {
		return new TemporaryMountPointChooser(vault, environment);
	}

	@Provides
	@PerVault
	@Named("orderedValidMountPointChoosers")
	public static Set<MountPointChooser> provideOrderedValidMountPointChoosers(Set<MountPointChooser> choosers) {
		//Sorted Set
		return choosers.stream().sorted().filter(MountPointChooser::isApplicable).collect(ImmutableSet.toImmutableSet());
	}
}

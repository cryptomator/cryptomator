package org.cryptomator.common.mountpoint;

import com.google.common.collect.ImmutableSet;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import org.cryptomator.common.vaults.PerVault;

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

	@Binds
	@IntoSet
	@PerVault
	public abstract MountPointChooser bindCustomMountPointChooser(CustomMountPointChooser chooser);

	@Binds
	@IntoSet
	@PerVault
	public abstract MountPointChooser bindCustomDriveLetterChooser(CustomDriveLetterChooser chooser);

	@Binds
	@IntoSet
	@PerVault
	public abstract MountPointChooser bindAvailableDriveLetterChooser(AvailableDriveLetterChooser chooser);

	@Binds
	@IntoSet
	@PerVault
	public abstract MountPointChooser bindTemporaryMountPointChooser(TemporaryMountPointChooser chooser);

	@Provides
	@PerVault
	@Named("orderedValidMountPointChoosers")
	public static Set<MountPointChooser> provideOrderedValidMountPointChoosers(Set<MountPointChooser> choosers) {
		//Sorted Set
		return choosers.stream().sorted().filter(MountPointChooser::isApplicable).collect(ImmutableSet.toImmutableSet());
	}
}

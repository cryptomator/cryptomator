package org.cryptomator.common.mountpoint;

import com.google.common.collect.Iterables;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntKey;
import dagger.multibindings.IntoMap;
import org.cryptomator.common.vaults.PerVault;

import javax.inject.Named;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Dagger-Module for {@link MountPointChooser MountPointChoosers.}<br>
 * See there for additional information.
 *
 * @see MountPointChooser
 */
@Module
public abstract class MountPointChooserModule {

	@Binds
	@IntoMap
	@IntKey(0)
	@PerVault
	public abstract MountPointChooser bindCustomMountPointChooser(CustomMountPointChooser chooser);

	@Binds
	@IntoMap
	@IntKey(100)
	@PerVault
	public abstract MountPointChooser bindCustomDriveLetterChooser(CustomDriveLetterChooser chooser);

	@Binds
	@IntoMap
	@IntKey(101)
	@PerVault
	public abstract MountPointChooser bindMacVolumeMountChooser(MacVolumeMountChooser chooser);

	@Binds
	@IntoMap
	@IntKey(200)
	@PerVault
	public abstract MountPointChooser bindAvailableDriveLetterChooser(AvailableDriveLetterChooser chooser);

	@Binds
	@IntoMap
	@IntKey(999)
	@PerVault
	public abstract MountPointChooser bindTemporaryMountPointChooser(TemporaryMountPointChooser chooser);

	@Provides
	@PerVault
	@Named("orderedMountPointChoosers")
	public static Iterable<MountPointChooser> provideOrderedMountPointChoosers(Map<Integer, MountPointChooser> choosers) {
		SortedMap<Integer, MountPointChooser> sortedChoosers = new TreeMap<>(choosers);
		return Iterables.unmodifiableIterable(sortedChoosers.values());
	}
}

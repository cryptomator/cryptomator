package org.cryptomator.common.mountpoint;

import dagger.MapKey;
import org.cryptomator.common.vaults.Volume;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

/**
 * Base interface for the Mountpoint-Choosing-Operation that results in the choice and
 * preparation of a Mountpoint or an exception otherwise.<br>
 * <p>All <i>MountPointChoosers (MPCs)</i> need to implement this class and must be added to
 * the pool of possible MPCs by {@link MountPointChooserModule MountPointChooserModule.}
 * The MountPointChooserModule requires all {@link dagger.Provides Providermethods} to
 * be annotated with the {@link PhaseKey @PhaseKey-Annotation} and a <b>unique</b> Phase to
 * allow the Module to sort them according to the phases' {@link Phase#getTiming()} timing.}
 * The timing must be defined by the developer to reflect a useful execution order.
 *
 * <p><b>Phase-Uniqueness:</b> Phases must be unique, meaning that they must not be used
 * to annotate more than <i>one</i> Providermethod. Define a new phase for each additional
 * MPC that is added to the Module. Timings can be reused; the order of Phases with equal
 * timing among themselves is undefined.
 *
 * <p>MPCs are executed by a {@link Volume} in the order of their phase's timing to find
 * and prepare a suitable Mountpoint for the Volume. The Volume only has access to a {@link Set}
 * of MPCs in this specific order, that is provided by the Module; the according Phases and exact
 * timings are inaccessible to the Volume. The Set only contains Choosers that were deemed
 * {@link #isApplicable() applicable} by the Module.
 *
 * <p>At execution of a MPC {@link #chooseMountPoint()} and then {@link #prepare(Path)} are called
 * by the Volume. If {@code #chooseMountPoint()} yields no result, the next MPC is executed
 * without first calling the {@code #prepare(Path)}-Method of the current MPC.
 * This is repeated until<br>
 * <ul>
 *     <li><b>either</b> a Mountpoint is returned by {@code #chooseMountPoint()}
 *     and {@code #prepare(Path)} succeeds or fails</li>
 *     <li><b>or</b> no MPC remains and an {@link InvalidMountPointException} is thrown.</li>
 * </ul>
 * If the {@code #prepare(Path)}-Method of a MPC fails the entire Mountpoint-Choosing-Operation
 * is aborted and the method should do all necessary cleanup before throwing the exception.
 * If the preparation succeeds {@link #cleanup(Path)} can be used after unmount to do any
 * remaining cleanup.
 */
public interface MountPointChooser {

	/**
	 * Called by the {@link MountPointChooserModule} to determine whether this MountPointChooser is
	 * applicable for the given Systemconfiguration.
	 *
	 * <p>The result of this method defaults to true. Developers should override this method to
	 * check for Systemconfigurations that are unsuitable for this MPC.
	 *
	 * @return a boolean flag; true if applicable, else false.
	 * @see #chooseMountPoint()
	 */
	default boolean isApplicable() {
		return true; //Usually most of the choosers should be applicable
	}

	/**
	 * Called by a {@link Volume} to do choose a Mountpoint according to the
	 * MountPointChoosers strategy.
	 *
	 * <p>This method is only called for MPCs that were deemed {@link #isApplicable() applicable}
	 * by the {@link MountPointChooserModule MountPointChooserModule.}
	 * Developers should override this method to find or extract a Mountpoint for
	 * the Volume <b>without</b> preparing it. Preparation should be done by
	 * {@link #prepare(Path)} instead.
	 * Exceptions in this method should be handled gracefully and result in returning
	 * {@link Optional#empty()} instead of throwing an exception.
	 *
	 * @return the chosen path or {@link Optional#empty()} if an exception occurred
	 * or no Mountpoint could be found.
	 * @see #isApplicable()
	 * @see #prepare(Path)
	 */
	Optional<Path> chooseMountPoint();

	/**
	 * Called by a {@link Volume} to prepare and/or verify the chosen Mountpoint.<br>
	 * This method is only called if the {@link #chooseMountPoint()}-Method of the same
	 * MountPointChooser returned a path.
	 *
	 * <p>Developers should override this method to prepare the Mountpoint for
	 * the Volume and check for any obstacles that could hinder the Mount-Operation.
	 * The Mountpoint is deemed "prepared" if it can be used to mount a Volume
	 * without any further Filesystemactions or Userinteraction. If this is not possible,
	 * this method should fail. In other words: This method should not return without
	 * either failing or finalizing the preparation of the Mountpoint.
	 * Generally speaking exceptions should be wrapped as
	 * {@link InvalidMountPointException} to allow efficient handling by the caller.
	 *
	 * <p>Often the preparation of a Mountpoint involves creating files or others
	 * actions that require cleaning up after the Volume is unmounted.
	 * In this case developers should override the {@link #cleanup(Path)}-Method
	 * and return {@code true} to the Volume to indicate that the
	 * {@code #cleanup}-Method of this MPC should be called after unmount.
	 *
	 * <p><b>Please note:</b> If this method fails the entire
	 * Mountpoint-Choosing-Operation is aborted without calling {@link #cleanup(Path)}
	 * or any other MPCs. Therefore this method should do all necessary cleanup
	 * before throwing the exception.
	 *
	 * @param mountPoint the Mountpoint chosen by {@link #chooseMountPoint()}
	 * @return a boolean flag; true if cleanup is needed, false otherwise
	 * @throws InvalidMountPointException
	 */
	default boolean prepare(Path mountPoint) throws InvalidMountPointException {
		return false; //NO-OP
	}

	/**
	 * Called by a {@link Volume} to do any cleanup needed after unmount.
	 *
	 * <p>This method is only called if the {@link #prepare(Path)}-Method of the same
	 * MountPointChooser returned {@code true}. Typically developers want to
	 * delete any files created prior to mount or do similar tasks.<br>
	 * Exceptions in this method should be handled gracefully.
	 *
	 * @param mountPoint the Mountpoint that was prepared by {@link #prepare(Path)}
	 */
	default void cleanup(Path mountPoint) {
		//NO-OP
	}

	/**
	 * The phases of the existing {@link MountPointChooser MountPointChoosers.}
	 * <p>The {@code Phases} of the MPCs are attached to them in the
	 * {@link MountPointChooserModule} by annotating them with the
	 * {@link PhaseKey @PhaseKey-Annotation.}
	 * <p>Each MPC must have a <b>unique</b> Phase that allows the Module to sort
	 * the MPCs according to their phases' {@link Phase#getTiming()} timing.}
	 * The timing must be defined by the developer to reflect a useful execution order.
	 *
	 * <p><b>Phase-Uniqueness:</b> Phases must be unique, meaning that they must not be used
	 * to annotate more than <i>one</i> Providermethod. Define a new phase for each additional
	 * MPC that is added to the Module. Timings can be reused; the order of Phases with equal
	 * timings among themselves is undefined.
	 */
	enum Phase {

		CUSTOM_MOUNTPOINT(0),

		CUSTOM_DRIVELETTER(1),

		AVAILABLE_DRIVELETTER(2),

		TEMPORARY_MOUNTPOINT(3);

		private final int timing;

		Phase(int timing) {
			this.timing = timing;
		}

		public int getTiming() {
			return timing;
		}
	}

	@MapKey
	@interface PhaseKey {

		Phase value();
	}
}

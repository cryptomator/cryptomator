package org.cryptomator.common.mountpoint;

import org.cryptomator.common.vaults.Volume;

import java.nio.file.Path;
import java.util.Optional;
import java.util.SortedSet;

/**
 * Base interface for the Mountpoint-Choosing-Operation that results in the choice and
 * preparation of a mountpoint or an exception otherwise.<br>
 * <p>All <i>MountPointChoosers (MPCs)</i> need to implement this class and must be added to
 * the pool of possible MPCs by the {@link MountPointChooserModule MountPointChooserModule.}
 * The MountPointChooserModule will sort them according to their {@link #getPriority() priority.}
 * The priority must be defined by the developer to reflect a useful execution order;
 * the order of execution of MPCs with equal priority is undefined.
 *
 * <p>MPCs are executed by a {@link Volume} in ascedning order of their priority to find
 * and prepare a suitable mountpoint for the volume. The volume has access to a
 * {@link SortedSet} of MPCs in this specific order, that is provided by the Module.
 * The Set only contains Choosers that were deemed {@link #isApplicable() applicable}
 * by the Module.
 *
 * <p>At execution of a MPC {@link #chooseMountPoint()} is called to choose a mountpoint
 * according to the MPC's <i>strategy.</i> The <i>strategy</i> can involve reading configs,
 * searching the filesystem, processing user-input or similar operations.
 * If {@code #chooseMountPoint()} returns a non-null path (everthing but
 * {@linkplain Optional#empty()}) the MPC's {@link #prepare(Path)}-Method is called and the
 * MountPoint is verfied and/or prepared. In this case <i>no other MPC's will be called for
 * this volume, even if {@code #prepare(Path)} fails.</i>
 *
 * <p>If {@code #chooseMountPoint()} yields no result, the next MPC is executed
 * <i>without</i> first calling the {@code #prepare(Path)}-Method of the current MPC.
 * This is repeated until<br>
 * <ul>
 *     <li><b>either</b> a mountpoint is returned by {@code #chooseMountPoint()}
 *     and {@code #prepare(Path)} succeeds or fails, ending the entire operation</li>
 *     <li><b>or</b> no MPC remains and an {@link InvalidMountPointException} is thrown.</li>
 * </ul>
 * If the {@code #prepare(Path)}-Method of a MPC fails, the entire Mountpoint-Choosing-Operation
 * is aborted and the method should do all necessary cleanup before throwing the exception.
 * If the preparation succeeds {@link #cleanup(Path)} can be used after unmount to do any
 * remaining cleanup.
 */
public interface MountPointChooser extends Comparable<MountPointChooser> {

	/**
	 * Called by the {@link MountPointChooserModule} to determine whether this MountPointChooser is
	 * applicable for the given Systemconfiguration.
	 *
	 * <p>Developers should override this method to
	 * check for Systemconfigurations that are unsuitable for this MPC.
	 *
	 * @return a boolean flag; true if applicable, else false.
	 * @see #chooseMountPoint()
	 */
	boolean isApplicable();

	/**
	 * Called by a {@link Volume} to choose a mountpoint according to the
	 * MountPointChoosers strategy.
	 *
	 * <p>This method is only called for MPCs that were deemed {@link #isApplicable() applicable}
	 * by the {@link MountPointChooserModule MountPointChooserModule.}
	 * Developers should override this method to find or extract a mountpoint for
	 * the volume <b>without</b> preparing it. Preparation should be done by
	 * {@link #prepare(Path)} instead.
	 * Exceptions in this method should be handled gracefully and result in returning
	 * {@link Optional#empty()} instead of throwing an exception.
	 *
	 * @return the chosen path or {@link Optional#empty()} if an exception occurred
	 * or no mountpoint could be found.
	 * @see #isApplicable()
	 * @see #prepare(Path)
	 */
	Optional<Path> chooseMountPoint();

	/**
	 * Called by a {@link Volume} to prepare and/or verify the chosen mountpoint.<br>
	 * This method is only called if the {@link #chooseMountPoint()}-Method of the same
	 * MountPointChooser returned a path.
	 *
	 * <p>Developers should override this method to prepare the mountpoint for
	 * the volume and check for any obstacles that could hinder the mount operation.
	 * The mountpoint is deemed "prepared" if it can be used to mount a volume
	 * without any further filesystem actions or user interaction. If this is not possible,
	 * this method should fail. In other words: This method should not return without
	 * either failing or finalizing the preparation of the mountpoint.
	 * Generally speaking exceptions should be wrapped as
	 * {@link InvalidMountPointException} to allow efficient handling by the caller.
	 *
	 * <p>Often the preparation of a mountpoint involves creating files or others
	 * actions that require cleaning up after the volume is unmounted.
	 * In this case developers should override the {@link #cleanup(Path)}-Method
	 * and return {@code true} to the volume to indicate that the
	 * {@code #cleanup}-Method of this MPC should be called after unmount.
	 *
	 * <p><b>Please note:</b> If this method fails the entire
	 * Mountpoint-Choosing-Operation is aborted without calling {@link #cleanup(Path)}
	 * or any other MPCs. Therefore this method should do all necessary cleanup
	 * before throwing the exception.
	 *
	 * @param mountPoint the mountpoint chosen by {@link #chooseMountPoint()}
	 * @return a boolean flag; true if cleanup is needed, false otherwise
	 * @throws InvalidMountPointException if the preparation fails
	 * @see #chooseMountPoint()
	 * @see #cleanup(Path)
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
	 * @param mountPoint the mountpoint that was prepared by {@link #prepare(Path)}
	 * @see #prepare(Path)
	 */
	default void cleanup(Path mountPoint) {
		//NO-OP
	}

	/**
	 * Called by the {@link MountPointChooserModule} to sort the available MPCs
	 * and determine their execution order.
	 * The priority must be defined by the developer to reflect a useful execution order.
	 * MPCs with lower priorities will be placed at lower indices in the resulting
	 * {@link SortedSet} and will be executed with higher probability.
	 * The order of execution of MPCs with equal priority is undefined.
	 *
	 * @return the priority of this MPC.
	 */
	int getPriority();

	/**
	 * Called by the {@link MountPointChooserModule} to determine the execution order
	 * of the registered MPCs. <b>Implementations usually should not override this
	 * method.</b> This default implementation sorts the MPCs in ascending order
	 * of their {@link #getPriority() priority.}<br>
	 * <br>
	 * <b>Original description:</b>
	 * <p>{@inheritDoc}
	 *
	 * @implNote This default implementation sorts the MPCs in ascending order
	 * of their {@link #getPriority() priority.}
	 */
	@Override
	default int compareTo(MountPointChooser other) {
		//Sort by priority (ascending order)
		return Integer.compare(this.getPriority(), other.getPriority());
	}
}

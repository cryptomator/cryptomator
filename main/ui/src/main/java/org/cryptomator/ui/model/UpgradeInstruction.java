package org.cryptomator.ui.model;

import org.cryptomator.ui.settings.Localization;

public interface UpgradeInstruction {

	static UpgradeInstruction[] AVAILABLE_INSTRUCTIONS = {new UpgradeVersion3DropBundleExtension()};

	/**
	 * @return Localized string to display to the user when an upgrade is needed.
	 */
	String getNotification(Vault vault, Localization localization);

	/**
	 * Upgrades a vault. Might take a moment, should be run in a background thread.
	 */
	void upgrade(Vault vault, Localization localization) throws UpgradeFailedException;

	/**
	 * Determines in O(1), if an upgrade can be applied to a vault.
	 * 
	 * @return <code>true</code> if and only if the vault can be migrated to a newer version without the risk of data losses.
	 */
	boolean isApplicable(Vault vault);

	/**
	 * Thrown when data migration failed.
	 */
	public class UpgradeFailedException extends Exception {

		UpgradeFailedException() {
		}

		UpgradeFailedException(String message) {
			super(message);
		}

	}

}

package org.cryptomator.common.vaults;

public enum VaultState {
	/**
	 * No vault found at the provided path
	 */
	MISSING,

	/**
	 * Vault requires migration to a newer vault format
	 */
	NEEDS_MIGRATION,

	/**
	 * Vault ready to be unlocked
	 */
	LOCKED,

	/**
	 * Vault in transition between two other states
	 */
	PROCESSING,

	/**
	 * Vault is unlocked
	 */
	UNLOCKED,

	/**
	 * Unknown state due to preceeding unrecoverable exceptions.
	 */
	ERROR;

}

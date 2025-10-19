package org.cryptomator.common.vaults;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a vault identity for plausibly deniable encryption.
 * Each identity has its own master key and presents a disjoint namespace.
 */
public class VaultIdentity {

	private final String id;
	private final String name;
	private final String description;
	private final boolean isPrimary;

	@JsonCreator
	public VaultIdentity(@JsonProperty("id") String id,
						 @JsonProperty("name") String name,
						 @JsonProperty("description") String description,
						 @JsonProperty("isPrimary") boolean isPrimary) {
		this.id = Objects.requireNonNull(id);
		this.name = Objects.requireNonNull(name);
		this.description = description;
		this.isPrimary = isPrimary;
	}

	/**
	 * Creates a new primary identity.
	 */
	public static VaultIdentity createPrimary(String name, String description) {
		return new VaultIdentity(UUID.randomUUID().toString(), name, description, true);
	}

	/**
	 * Creates a new secondary identity.
	 */
	public static VaultIdentity createSecondary(String name, String description) {
		return new VaultIdentity(UUID.randomUUID().toString(), name, description, false);
	}

	@JsonProperty("id")
	public String getId() {
		return id;
	}

	@JsonProperty("name")
	public String getName() {
		return name;
	}

	@JsonProperty("description")
	public String getDescription() {
		return description;
	}

	@JsonProperty("isPrimary")
	public boolean isPrimary() {
		return isPrimary;
	}

	/**
	 * Returns the masterkey filename for this identity.
	 * TrueCrypt-style: ALL identities use the same filename.
	 * This provides true plausible deniability - cannot detect hidden vaults by file presence.
	 * This is a computed property and should not be serialized.
	 */
	@JsonIgnore
	public String getMasterkeyFilename() {
		// TrueCrypt-style: All identities use same file with multiple keyslots
		return "masterkey.cryptomator";
	}

	/**
	 * Returns the vault config filename for this identity.
	 * TrueCrypt-style: ALL identities use the same filename.
	 * The vault config file is multi-keyslot format, storing multiple configurations
	 * signed with different masterkeys. This provides TRUE plausible deniability
	 * as there's no separate vault.bak file to reveal hidden vaults.
	 * This is a computed property and should not be serialized.
	 */
	@JsonIgnore
	public String getVaultConfigFilename() {
		// TrueCrypt-style: All identities use same file with multiple config slots
		// This eliminates vault.bak and achieves TRUE deniable encryption
		return "vault.cryptomator";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		VaultIdentity that = (VaultIdentity) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "VaultIdentity{" +
				"id='" + id + '\'' +
				", name='" + name + '\'' +
				", isPrimary=" + isPrimary +
				'}';
	}
}

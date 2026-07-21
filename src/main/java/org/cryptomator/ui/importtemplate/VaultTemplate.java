package org.cryptomator.ui.importtemplate;

import org.cryptomator.common.Constants;
import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptofs.VaultConfigLoadException;
import org.cryptomator.ui.keyloading.hub.HubConfig;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A vault template that has been unpacked to a temporary directory and found sound.
 * <p>
 * Holding an instance means the template <em>is</em> valid: it was a readable ZIP within the entry and size
 * limits, held a decodable {@value Constants#VAULTCONFIG_FILENAME} directly in its root and no entry escaped
 * the extraction directory.
 * <p>
 * Because the vault lies in the archive root, the temporary directory <em>is</em> the vault directory: importing moves
 * it wholesale to its destination.
 * <p>
 * The instance owns that temporary directory and <strong>must be {@link #close() closed}</strong>, whether or not the
 * import completes.
 * <p>
 * {@link #hubUrl()} is read from the <em>unverified</em> vault config.
 */
public final class VaultTemplate implements AutoCloseable {

	private static final Logger LOG = LoggerFactory.getLogger(VaultTemplate.class);

	private final Path vaultDir;
	private final @Nullable String hubUrl;

	private VaultTemplate(Path vaultDir, @Nullable String hubUrl) {
		this.vaultDir = vaultDir;
		this.hubUrl = hubUrl;
	}

	/**
	 * Unpacks the given archive to a temporary directory and validates it.
	 * <p>
	 * This touches the file system and should not run on the FX thread.
	 *
	 * @param archive the ZIP archive bytes
	 * @return the unpacked template, which the caller must close
	 * @throws MalformedTemplateException if the archive is unusable
	 * @throws IOException                if unpacking fails
	 */
	public static VaultTemplate extract(byte[] archive) throws IOException {
		return extract(archive, null);
	}

	@VisibleForTesting
	static VaultTemplate extract(byte[] archive, @Nullable Path tempParent) throws IOException {
		Path vaultDir = tempParent == null //
				? Files.createTempDirectory("vault-template-") //
				: Files.createTempDirectory(tempParent, "vault-template-");
		try {
			VaultTemplateExtractor.extractTo(archive, vaultDir);
			var hubUrl = readHubUrl(vaultDir);
			return new VaultTemplate(vaultDir, hubUrl);
		} catch (IOException | RuntimeException e) {
			VaultTemplateExtractor.deleteQuietly(vaultDir);
			throw e;
		}
	}

	private static @Nullable String readHubUrl(Path vaultDir) throws IOException {
		var token = Files.readString(vaultDir.resolve(Constants.VAULTCONFIG_FILENAME), StandardCharsets.US_ASCII).trim();
		HubConfig hubConfig;
		try {
			hubConfig = VaultConfig.decode(token).getHeader("hub", HubConfig.class);
		} catch (VaultConfigLoadException e) {
			throw new MalformedTemplateException("Template does not contain a decodable vault config.", e);
		}
		if (hubConfig == null) {
			return null; // not a hub vault - allowed, the user just cannot be shown a Hub
		}
		try {
			return hubConfig.getApiBaseUrl().toString();
		} catch (RuntimeException e) {
			// hub header present but unusable (e.g. neither apiBaseUrl nor devicesResourceUrl set)
			LOG.warn("Vault template declares an unusable hub config.", e);
			return null;
		}
	}

	/**
	 * The Hub instance which generated the template, or {@code null} if its config declares none.
	 * <p>
	 * Unverified - see the class documentation.
	 */
	public @Nullable String hubUrl() {
		return hubUrl;
	}

	/**
	 * Moves the unpacked vault to its final location.
	 *
	 * @param destination the target vault directory, which must not yet exist
	 * @throws java.nio.file.FileAlreadyExistsException if {@code destination} already exists
	 * @throws IOException                              if the vault cannot be moved
	 */
	public void moveTo(Path destination) throws IOException {
		VaultTemplateExtractor.moveToDestination(vaultDir, destination);
	}

	/**
	 * Discards the unpacked vault. A no-op after a successful {@link #moveTo(Path)}, which relocates the directory
	 * this would otherwise delete.
	 */
	@Override
	public void close() {
		VaultTemplateExtractor.deleteQuietly(vaultDir);
	}
}

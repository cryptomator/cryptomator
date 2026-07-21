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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/**
 * A vault template that has been unpacked to a temporary directory and found sound.
 * <p>
 * Holding an instance means the template <em>can</em> be imported: it was a readable ZIP within the entry and size
 * limits, held exactly one decodable {@value Constants#VAULTCONFIG_FILENAME}, and no entry escaped the extraction
 * directory. Unpacking therefore happens once, when the deeplink arrives, so a template that could never be imported is
 * rejected before the user is asked to choose a storage location - and {@link #moveTo(Path)} is left with nothing to
 * validate but the destination.
 * <p>
 * The instance owns a temporary directory and <strong>must be {@link #close() closed}</strong>, whether or not the
 * import completes.
 * <p>
 * {@link #hubUrl()} is read from the <em>unverified</em> vault config: its signature is keyed on the masterkey, which is
 * only obtained by completing the Hub flow. It is shown so the user can recognize an unexpected Hub; the authoritative
 * trust decision remains with {@code CheckHostTrustController} at unlock time.
 */
public final class VaultTemplate implements AutoCloseable {

	private static final Logger LOG = LoggerFactory.getLogger(VaultTemplate.class);

	private final Path tempDir;
	private final Path vaultRoot;
	private final @Nullable String hubUrl;

	private VaultTemplate(Path tempDir, Path vaultRoot, @Nullable String hubUrl) {
		this.tempDir = tempDir;
		this.vaultRoot = vaultRoot;
		this.hubUrl = hubUrl;
	}

	/**
	 * Unpacks and validates the given archive on {@code executor}, off the FX thread.
	 * <p>
	 * Failures arrive as a {@link CompletionException} wrapping the cause, so callers should unwrap before deciding
	 * what to show: a {@link MalformedTemplateException} means the template itself is a dead end, any other
	 * {@link IOException} means unpacking failed for an environmental reason.
	 *
	 * @param archive  the ZIP archive bytes
	 * @param executor the executor to unpack on
	 * @return the unpacked template, which the caller must close
	 * @see #extract(byte[])
	 */
	public static CompletionStage<VaultTemplate> extractAsync(byte[] archive, Executor executor) {
		return CompletableFuture.supplyAsync(()-> {
			try {
				return VaultTemplate.extract(archive);
			} catch (IOException e) {
				throw new CompletionException(e);
			}
		}, executor);
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
		Path tempDir = tempParent == null //
				? Files.createTempDirectory("vault-template-") //
				: Files.createTempDirectory(tempParent, "vault-template-");
		try {
			var vaultRoot = VaultTemplateExtractor.extractTo(archive, tempDir);
			return new VaultTemplate(tempDir, vaultRoot, readHubUrl(vaultRoot));
		} catch (IOException | RuntimeException e) {
			VaultTemplateExtractor.deleteQuietly(tempDir);
			throw e;
		}
	}

	private static @Nullable String readHubUrl(Path vaultRoot) throws IOException {
		var token = Files.readString(vaultRoot.resolve(Constants.VAULTCONFIG_FILENAME), StandardCharsets.US_ASCII).trim();
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
	 * The Hub this template's vault belongs to, or {@code null} if its config declares none.
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
		VaultTemplateExtractor.moveToDestination(vaultRoot, destination);
	}

	/**
	 * Deletes the temporary directory. Safe to call after a successful {@link #moveTo(Path)}, which only moves the
	 * vault directory out of it.
	 */
	@Override
	public void close() {
		VaultTemplateExtractor.deleteQuietly(tempDir);
	}
}

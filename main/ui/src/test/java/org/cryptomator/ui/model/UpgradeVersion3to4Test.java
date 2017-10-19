package org.cryptomator.ui.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import org.cryptomator.ui.l10n.Localization;
import org.cryptomator.ui.l10n.LocalizationMock;
import org.cryptomator.ui.model.UpgradeStrategy.UpgradeFailedException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

public class UpgradeVersion3to4Test {

	private static final Localization L10N = new LocalizationMock();
	private static final String NULL_KEY_CONTENTS = "{" //
			+ "  \"version\": 3," //
			+ "  \"scryptSalt\": \"AAAAAAAAAAA=\"," //
			+ "  \"scryptCostParam\": 16384," //
			+ "  \"scryptBlockSize\": 8," //
			+ "  \"primaryMasterKey\": \"BJPIq5pvhN24iDtPJLMFPLaVJWdGog9k4n0P03j4ru+ivbWY9OaRGQ==\"," //
			+ "  \"hmacMasterKey\": \"BJPIq5pvhN24iDtPJLMFPLaVJWdGog9k4n0P03j4ru+ivbWY9OaRGQ==\"," //
			+ "  \"versionMac\": \"iUmRRHITuyJsJbVNqGNw+82YQ4A3Rma7j/y1v0DCVLA=\"" //
			+ "}";

	@Rule
	public final ExpectedException thrown = ExpectedException.none();
	private final UpgradeStrategy upgradeStrategy = new UpgradeVersion3to4(L10N);
	private FileSystem fs;
	private Path fsRoot;
	private Vault vault;
	private Path dataDir;
	private Path metadataDir;

	@Before
	public void setup() throws IOException {
		fs = Jimfs.newFileSystem(Configuration.unix());
		fsRoot = fs.getPath("/");
		dataDir = fsRoot.resolve("d");
		metadataDir = fsRoot.resolve("m");
		vault = Mockito.mock(Vault.class);
		Mockito.when(vault.getPath()).thenReturn(fsRoot);

		Files.write(fsRoot.resolve("masterkey.cryptomator"), NULL_KEY_CONTENTS.getBytes(StandardCharsets.US_ASCII));
	}

	@After
	public void teardown() throws IOException {
		fs.close();
	}

	@Test
	public void upgradeFailsWithWrongPassword() throws UpgradeFailedException {
		thrown.expect(UpgradeFailedException.class);
		thrown.expectMessage("unlock.errorMessage.wrongPassword");
		upgradeStrategy.upgrade(vault, "asdd");
	}

	@Test
	public void upgradeCreatesBackup() throws UpgradeFailedException {
		upgradeStrategy.upgrade(vault, "asd");
		Assert.assertTrue(Files.exists(fsRoot.resolve("masterkey.cryptomator.bkup")));
	}

	@Test
	public void upgradeRenamesSimpleDirFile() throws IOException, UpgradeFailedException {
		Path lvl2Dir = dataDir.resolve("AB/CDEFGHIJKLMNOPQRSTUVWXYZ234567");
		Files.createDirectories(lvl2Dir);
		Path oldFile = lvl2Dir.resolve("ABCDEFGH_");
		Files.createFile(oldFile);

		upgradeStrategy.upgrade(vault, "asd");
		Path newFile = lvl2Dir.resolve("0ABCDEFGH");
		Assert.assertTrue(Files.exists(newFile));
		Assert.assertTrue(Files.notExists(oldFile));
	}

	@Test
	public void upgradeRenamesConflictingDirFile() throws IOException, UpgradeFailedException {
		Path lvl2Dir = dataDir.resolve("AB/CDEFGHIJKLMNOPQRSTUVWXYZ234567");
		Files.createDirectories(lvl2Dir);
		Path oldFile = lvl2Dir.resolve("ABCDEFGH_ (1)");
		Files.createFile(oldFile);

		upgradeStrategy.upgrade(vault, "asd");
		Path newFile = lvl2Dir.resolve("0ABCDEFGH (1)");
		Assert.assertTrue(Files.exists(newFile));
		Assert.assertTrue(Files.notExists(oldFile));
	}

	@Test
	public void upgradeDontRenameNonDirFile() throws IOException, UpgradeFailedException {
		Path lvl2Dir = dataDir.resolve("AB/CDEFGHIJKLMNOPQRSTUVWXYZ234567");
		Files.createDirectories(lvl2Dir);
		Path oldFile = lvl2Dir.resolve("ABCDEFGH");
		Files.createFile(oldFile);

		upgradeStrategy.upgrade(vault, "asd");
		Assert.assertTrue(Files.exists(oldFile));
	}

	@Test
	public void upgradeRenameSimpleLongDirFile() throws IOException, UpgradeFailedException {
		Path lvl2Dir = dataDir.resolve("AB/CDEFGHIJKLMNOPQRSTUVWXYZ234567");
		Files.createDirectories(lvl2Dir);
		Path oldFile = lvl2Dir.resolve("ABCDEFGH.lng");
		Files.createFile(oldFile);
		Path oldMetadataFile = metadataDir.resolve("AB/CD/ABCDEFGH.lng");
		Files.createDirectories(oldMetadataFile.getParent());
		Files.write(oldMetadataFile, "OPQRSTUVWXYZ====_".getBytes(StandardCharsets.UTF_8));

		upgradeStrategy.upgrade(vault, "asd");
		// hex2base32(sha1("0OPQRSTUVWXYZ====")) = DDLCFQ3ODTEAHEZJPHIJQRDHROB3K42G
		Path newMetadataFile = metadataDir.resolve("DD/LC/DDLCFQ3ODTEAHEZJPHIJQRDHROB3K42G.lng");
		Path newFile = lvl2Dir.resolve("DDLCFQ3ODTEAHEZJPHIJQRDHROB3K42G.lng");
		Assert.assertTrue(Files.exists(newFile));
		Assert.assertTrue(Files.exists(newMetadataFile));
		Assert.assertTrue(Files.notExists(oldFile));
	}

	@Test
	public void upgradeRenameConflictingLongDirFile() throws IOException, UpgradeFailedException {
		Path lvl2Dir = dataDir.resolve("AB/CDEFGHIJKLMNOPQRSTUVWXYZ234567");
		Files.createDirectories(lvl2Dir);
		Path oldFile = lvl2Dir.resolve("ABCDEFGH (1).lng");
		Files.createFile(oldFile);
		Path oldMetadataFile = metadataDir.resolve("AB/CD/ABCDEFGH.lng");
		Files.createDirectories(oldMetadataFile.getParent());
		Files.write(oldMetadataFile, "OPQRSTUVWXYZ====_".getBytes(StandardCharsets.UTF_8));

		upgradeStrategy.upgrade(vault, "asd");
		// hex2base32(sha1("0OPQRSTUVWXYZ====")) = DDLCFQ3ODTEAHEZJPHIJQRDHROB3K42G
		Path newMetadataFile = metadataDir.resolve("DD/LC/DDLCFQ3ODTEAHEZJPHIJQRDHROB3K42G.lng");
		Path newFile = lvl2Dir.resolve("DDLCFQ3ODTEAHEZJPHIJQRDHROB3K42G (1).lng");
		Assert.assertTrue(Files.exists(newFile));
		Assert.assertTrue(Files.exists(newMetadataFile));
		Assert.assertTrue(Files.notExists(oldFile));
	}

	@Test
	public void upgradeDontRenameLongNonDirFile() throws IOException, UpgradeFailedException {
		Path lvl2Dir = dataDir.resolve("AB/CDEFGHIJKLMNOPQRSTUVWXYZ234567");
		Files.createDirectories(lvl2Dir);
		Path oldFile = lvl2Dir.resolve("ABCDEFGH.lng");
		Files.createFile(oldFile);
		Path oldMetadataFile = metadataDir.resolve("AB/CD/ABCDEFGH.lng");
		Files.createDirectories(oldMetadataFile.getParent());
		Files.write(oldMetadataFile, "OPQRSTUVWXYZ====".getBytes(StandardCharsets.UTF_8));

		upgradeStrategy.upgrade(vault, "asd");
		Assert.assertTrue(Files.exists(oldFile));
	}

}

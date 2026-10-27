package org.cryptomator.launcher;

import org.cryptomator.launcher.WindowsAdminConfigVerifier.Ace;
import org.cryptomator.launcher.WindowsAdminConfigVerifier.SecurityInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

import static org.cryptomator.launcher.WindowsAdminConfigVerifier.ACCESS_ALLOWED_ACE_TYPE;
import static org.cryptomator.launcher.WindowsAdminConfigVerifier.ACCESS_ALLOWED_CALLBACK_ACE_TYPE;
import static org.cryptomator.launcher.WindowsAdminConfigVerifier.ACCESS_DENIED_ACE_TYPE;
import static org.cryptomator.launcher.WindowsAdminConfigVerifier.DIR_MODIFYING_RIGHTS;
import static org.cryptomator.launcher.WindowsAdminConfigVerifier.FILE_MODIFYING_RIGHTS;
import static org.cryptomator.launcher.WindowsAdminConfigVerifier.INHERIT_ONLY_ACE;

public class WindowsAdminConfigVerifierTest {

	private static final Path PATH = Path.of("config.properties");
	private static final String SYSTEM = "S-1-5-18";
	private static final String ADMINS = "S-1-5-32-544";
	private static final String USERS = "S-1-5-32-545";
	private static final int FULL_CONTROL = 0x1F01FF;
	private static final int READ_EXECUTE = 0x1200A9;
	private static final int OBJECT_INHERIT_ACE = 0x1;
	private static final int ACCESS_ALLOWED_OBJECT_ACE_TYPE = 0x5;

	private static boolean isProtected(String owner, int modifyingRights, Ace... aces) {
		return WindowsAdminConfigVerifier.isProtected(PATH, new SecurityInfo(owner, List.of(aces)), modifyingRights);
	}

	private static Ace allowAce(String sid, int mask) {
		return new Ace(ACCESS_ALLOWED_ACE_TYPE, 0, mask, sid);
	}

	@Test
	@DisplayName("Trusted owner and only trusted SIDs with write access")
	void trustedOwnerAndTrustedWriters() {
		Assertions.assertTrue(isProtected(ADMINS, FILE_MODIFYING_RIGHTS, allowAce(SYSTEM, FULL_CONTROL), allowAce(ADMINS, FULL_CONTROL), allowAce(USERS, READ_EXECUTE)));
	}

	@Test
	@DisplayName("Untrusted owner")
	void untrustedOwner() {
		Assertions.assertFalse(isProtected(USERS, FILE_MODIFYING_RIGHTS, allowAce(ADMINS, FULL_CONTROL)));
	}

	@Test
	@DisplayName("Missing owner")
	void missingOwner() {
		Assertions.assertFalse(isProtected(null, FILE_MODIFYING_RIGHTS, allowAce(ADMINS, FULL_CONTROL)));
	}

	@Test
	@DisplayName("NULL DACL")
	void nullDacl() {
		Assertions.assertFalse(WindowsAdminConfigVerifier.isProtected(PATH, new SecurityInfo(ADMINS, null), FILE_MODIFYING_RIGHTS));
	}

	@Test
	@DisplayName("Untrusted SID with append access to file")
	void untrustedAppender() {
		Assertions.assertFalse(isProtected(ADMINS, FILE_MODIFYING_RIGHTS, allowAce(USERS, READ_EXECUTE | 0x4)));
	}

	@Test
	@DisplayName("Untrusted SID with GENERIC_WRITE on file")
	void untrustedGenericWriterOnFile() {
		Assertions.assertFalse(isProtected(ADMINS, FILE_MODIFYING_RIGHTS, allowAce(USERS, 0x40000000)));
	}

	@Test
	@DisplayName("Untrusted SID with GENERIC_ALL on directory")
	void untrustedGenericAllOnDir() {
		Assertions.assertFalse(isProtected(ADMINS, DIR_MODIFYING_RIGHTS, allowAce(USERS, 0x10000000)));
	}

	@Test
	@DisplayName("Untrusted SID with permission to change DACL")
	void untrustedDaclWriter() {
		Assertions.assertFalse(isProtected(ADMINS, DIR_MODIFYING_RIGHTS, allowAce(USERS, 0x40000)));
	}

	@Test
	@DisplayName("Untrusted SID with permission to delete children of directory")
	void untrustedChildDeleter() {
		Assertions.assertFalse(isProtected(ADMINS, DIR_MODIFYING_RIGHTS, allowAce(USERS, 0x40)));
	}

	@Test
	@DisplayName("Untrusted SID with permission to add files and subdirectories to directory")
	void untrustedFileCreator() {
		// default ACE inherited from C:\ProgramData: (CI)(WD,AD,WEA,WA)
		Assertions.assertTrue(isProtected(ADMINS, DIR_MODIFYING_RIGHTS, allowAce(USERS, 0x116)));
	}

	@Test
	@DisplayName("Inherit-only and deny ACEs are ignored")
	void inheritOnlyAndDenyAces() {
		var inheritOnly = new Ace(ACCESS_ALLOWED_ACE_TYPE, OBJECT_INHERIT_ACE | INHERIT_ONLY_ACE, FULL_CONTROL, USERS);
		var deny = new Ace(ACCESS_DENIED_ACE_TYPE, 0, FULL_CONTROL, USERS);

		Assertions.assertTrue(isProtected(ADMINS, FILE_MODIFYING_RIGHTS, inheritOnly, deny));
	}

	@Test
	@DisplayName("Callback ACE granting write access to untrusted SID")
	void untrustedCallbackAce() {
		Assertions.assertFalse(isProtected(ADMINS, FILE_MODIFYING_RIGHTS, new Ace(ACCESS_ALLOWED_CALLBACK_ACE_TYPE, 0, FULL_CONTROL, USERS)));
	}

	@Test
	@DisplayName("Callback ACE granting write access to trusted SID")
	void trustedCallbackAce() {
		Assertions.assertTrue(isProtected(ADMINS, FILE_MODIFYING_RIGHTS, new Ace(ACCESS_ALLOWED_CALLBACK_ACE_TYPE, 0, FULL_CONTROL, ADMINS)));
	}

	@Test
	@DisplayName("ACE of unexpected type granting write access")
	void unexpectedAceType() {
		Assertions.assertFalse(isProtected(ADMINS, FILE_MODIFYING_RIGHTS, new Ace(ACCESS_ALLOWED_OBJECT_ACE_TYPE, 0, FULL_CONTROL, null)));
	}

	@Nested
	@EnabledOnOs(OS.WINDOWS)
	class OnWindows {

		@Test
		@DisplayName("Reads owner of Windows directory")
		void readsOwner() throws IOException {
			var securityInfo = WindowsAdminConfigVerifier.Advapi32.securityInfo(Path.of(System.getenv("SystemRoot")));

			Assertions.assertEquals("S-1-5-80-956008885-3418522649-1831038044-1853292631-2271478464", securityInfo.ownerSid()); // TrustedInstaller
		}

		@Test
		@DisplayName("Reads owner and DACL of file created by current user")
		void readsUserFile(@TempDir Path dir) throws IOException {
			var file = Files.createFile(dir.resolve("config.properties"));

			var securityInfo = WindowsAdminConfigVerifier.Advapi32.securityInfo(file);

			Assertions.assertAll( //
					() -> Assertions.assertTrue(securityInfo.ownerSid().startsWith("S-1-5-21-")), // local or domain user
					() -> Assertions.assertTrue(securityInfo.dacl().contains(new Ace(ACCESS_ALLOWED_ACE_TYPE, 0x10, FULL_CONTROL, SYSTEM))), // inherited from user profile
					() -> Assertions.assertFalse(WindowsAdminConfigVerifier.isProtected(file)));
		}

		@Test
		@DisplayName("Missing file")
		void missingFile(@TempDir Path dir) {
			Assertions.assertThrows(NoSuchFileException.class, () -> WindowsAdminConfigVerifier.isProtected(dir.resolve("config.properties")));
		}

	}

}

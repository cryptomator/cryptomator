package org.cryptomator.launcher;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_INT_UNALIGNED;
import static java.lang.foreign.ValueLayout.JAVA_SHORT_UNALIGNED;
import static java.nio.charset.StandardCharsets.UTF_16LE;

/**
 * Verifies that a file on Windows can only be modified by SYSTEM, Administrators or TrustedInstaller.
 */
class WindowsAdminConfigVerifier {

	private static final Logger LOG = EventualLogger.INSTANCE;
	// SYSTEM, BUILTIN\Administrators, NT SERVICE\TrustedInstaller
	private static final Set<String> TRUSTED_SIDS = Set.of("S-1-5-18", "S-1-5-32-544", "S-1-5-80-956008885-3418522649-1831038044-1853292631-2271478464");

	// access rights, see https://learn.microsoft.com/en-us/windows/win32/fileio/file-access-rights-constants and https://learn.microsoft.com/en-us/windows/win32/secauthz/access-mask
	private static final int FILE_WRITE_DATA = 0x2; // = FILE_ADD_FILE for directories
	private static final int FILE_APPEND_DATA = 0x4; // = FILE_ADD_SUBDIRECTORY for directories
	private static final int FILE_DELETE_CHILD = 0x40;
	private static final int DELETE = 0x10000;
	private static final int WRITE_DAC = 0x40000;
	private static final int WRITE_OWNER = 0x80000;
	private static final int GENERIC_ALL = 0x10000000;
	private static final int GENERIC_WRITE = 0x40000000;
	// rights to change the content of a file or to replace it
	static final int FILE_MODIFYING_RIGHTS = FILE_WRITE_DATA | FILE_APPEND_DATA | DELETE | WRITE_DAC | WRITE_OWNER | GENERIC_ALL | GENERIC_WRITE;
	// rights to replace a child of a directory or the directory itself (adding new children is fine, as the existing file cannot be overwritten that way, hence GENERIC_WRITE is fine as well)
	static final int DIR_MODIFYING_RIGHTS = FILE_DELETE_CHILD | DELETE | WRITE_DAC | WRITE_OWNER | GENERIC_ALL;

	// ACE types and flags, see https://learn.microsoft.com/en-us/windows/win32/api/winnt/ns-winnt-ace_header
	static final int ACCESS_ALLOWED_ACE_TYPE = 0x0;
	static final int ACCESS_DENIED_ACE_TYPE = 0x1;
	static final int ACCESS_DENIED_OBJECT_ACE_TYPE = 0x6;
	static final int ACCESS_ALLOWED_CALLBACK_ACE_TYPE = 0x9;
	static final int ACCESS_DENIED_CALLBACK_ACE_TYPE = 0xA;
	static final int ACCESS_DENIED_CALLBACK_OBJECT_ACE_TYPE = 0xC;
	static final int INHERIT_ONLY_ACE = 0x8;

	private WindowsAdminConfigVerifier() {
	}

	/**
	 * Checks that neither the given file nor its parent directory is owned or modifiable by any principal other than SYSTEM, Administrators or TrustedInstaller.
	 * <p>
	 * Paths containing links (junctions or symlinks) are rejected, because a link owned by an untrusted principal could be retargeted between this check and the actual read of the file.
	 *
	 * @param file path of an existing file
	 * @return {@code true} if only trusted principals are able to modify or replace the file
	 * @throws java.nio.file.NoSuchFileException if the file does not exist
	 * @throws IOException if the owner or DACL cannot be read
	 */
	static boolean isProtected(Path file) throws IOException {
		if (!isLinkFree(file)) {
			return false;
		}
		var dir = file.toAbsolutePath().getParent();
		return isProtected(file, Advapi32.securityInfo(file), FILE_MODIFYING_RIGHTS) //
				&& isProtected(dir, Advapi32.securityInfo(dir), DIR_MODIFYING_RIGHTS);
	}

	//visible for testing
	static boolean isLinkFree(Path file) throws IOException {
		// both canonicalize case and 8.3 short names, but only the former resolves junctions and symlinks
		var realFile = file.toRealPath();
		var unresolvedFile = file.toRealPath(LinkOption.NOFOLLOW_LINKS);
		if (realFile.equals(unresolvedFile)) {
			return true;
		}
		LOG.warn("{} resolves to {}. Links are not allowed in the admin config path.", unresolvedFile, realFile);
		return false;
	}

	//visible for testing
	static boolean isProtected(Path path, SecurityInfo securityInfo, int modifyingRights) {
		if (securityInfo.ownerSid() == null || !TRUSTED_SIDS.contains(securityInfo.ownerSid())) {
			LOG.warn("{} is owned by untrusted SID {}.", path, securityInfo.ownerSid());
			return false;
		}
		if (securityInfo.dacl() == null) {
			LOG.warn("{} has a NULL DACL, granting full access to everyone.", path);
			return false;
		}
		for (var ace : securityInfo.dacl()) {
			// deny ACEs are ignored, which can only make the check stricter
			if ((ace.flags() & INHERIT_ONLY_ACE) != 0 || ace.isDeny() || (ace.isAllow() && TRUSTED_SIDS.contains(ace.sid()))) {
				continue;
			}
			// remaining ACEs are either allow ACEs for untrusted SIDs or of an unexpected type (e.g. object ACEs), whose SID is not evaluated
			if ((ace.mask() & modifyingRights) != 0) {
				LOG.warn("{} grants access mask 0x{} to untrusted SID {} (ACE type 0x{}).", path, Integer.toHexString(ace.mask()), ace.sid(), Integer.toHexString(ace.type()));
				return false;
			}
		}
		return true;
	}

	// dacl is null for a NULL DACL
	record SecurityInfo(@Nullable String ownerSid, @Nullable List<Ace> dacl) {}

	// sid is null for ACE types whose SID is not evaluated
	record Ace(int type, int flags, int mask, @Nullable String sid) {

		boolean isAllow() {
			return type == ACCESS_ALLOWED_ACE_TYPE || type == ACCESS_ALLOWED_CALLBACK_ACE_TYPE;
		}

		boolean isDeny() {
			return type == ACCESS_DENIED_ACE_TYPE || type == ACCESS_DENIED_OBJECT_ACE_TYPE || type == ACCESS_DENIED_CALLBACK_ACE_TYPE || type == ACCESS_DENIED_CALLBACK_OBJECT_ACE_TYPE;
		}
	}

	// reads owner and DACL, see https://learn.microsoft.com/en-us/windows/win32/api/aclapi/nf-aclapi-getnamedsecurityinfow
	//visible for testing
	static class Advapi32 {

		private static final int SE_FILE_OBJECT = 1;
		private static final int OWNER_SECURITY_INFORMATION = 0x1;
		private static final int DACL_SECURITY_INFORMATION = 0x4;
		private static final long ACL_HEADER_SIZE = 8; // AclRevision, Sbz1, AclSize, AceCount, Sbz2
		private static final long ACL_ACE_COUNT_OFFSET = 4;
		private static final long ACE_MASK_OFFSET = 4; // after the ACE_HEADER, same for all ACE types
		private static final long ACE_SID_OFFSET = 8; // after ACE_HEADER and mask, only for non-object ACE types
		private static final long MAX_SID_STRING_BYTES = 512;

		private static final Linker LINKER = Linker.nativeLinker();
		private static final SymbolLookup ADVAPI32 = SymbolLookup.libraryLookup("advapi32.dll", Arena.global());
		private static final SymbolLookup KERNEL32 = SymbolLookup.libraryLookup("kernel32.dll", Arena.global());
		private static final MethodHandle GET_NAMED_SECURITY_INFO = LINKER.downcallHandle(ADVAPI32.findOrThrow("GetNamedSecurityInfoW"), FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));
		private static final MethodHandle GET_ACE = LINKER.downcallHandle(ADVAPI32.findOrThrow("GetAce"), FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS));
		private static final MethodHandle CONVERT_SID_TO_STRING_SID = LINKER.downcallHandle(ADVAPI32.findOrThrow("ConvertSidToStringSidW"), FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
		private static final MethodHandle LOCAL_FREE = LINKER.downcallHandle(KERNEL32.findOrThrow("LocalFree"), FunctionDescriptor.of(ADDRESS, ADDRESS));

		static SecurityInfo securityInfo(Path path) throws IOException {
			try (var arena = Arena.ofConfined()) {
				var ownerPointer = arena.allocate(ADDRESS);
				var daclPointer = arena.allocate(ADDRESS);
				var descriptorPointer = arena.allocate(ADDRESS);
				var error = (int) GET_NAMED_SECURITY_INFO.invokeExact(arena.allocateFrom(path.toString(), UTF_16LE), SE_FILE_OBJECT, OWNER_SECURITY_INFORMATION | DACL_SECURITY_INFORMATION, ownerPointer, MemorySegment.NULL, daclPointer, MemorySegment.NULL, descriptorPointer);
				if (error != 0) {
					throw new IOException("GetNamedSecurityInfoW failed for %s with error %d".formatted(path, error));
				}
				// owner and DACL point into the security descriptor
				var descriptor = descriptorPointer.get(ADDRESS, 0);
				try {
					var owner = ownerPointer.get(ADDRESS, 0);
					var dacl = daclPointer.get(ADDRESS, 0);
					var ownerSid = MemorySegment.NULL.equals(owner) ? null : sidString(owner, arena);
					var aces = MemorySegment.NULL.equals(dacl) ? null : aces(dacl, arena);
					return new SecurityInfo(ownerSid, aces);
				} finally {
					localFree(descriptor);
				}
			} catch (IOException e) {
				throw e;
			} catch (Throwable e) {
				throw new IOException("Failed to read security info of " + path, e);
			}
		}

		private static List<Ace> aces(MemorySegment dacl, Arena arena) throws Throwable {
			var aceCount = Short.toUnsignedInt(dacl.reinterpret(ACL_HEADER_SIZE).get(JAVA_SHORT_UNALIGNED, ACL_ACE_COUNT_OFFSET));
			var acePointer = arena.allocate(ADDRESS);
			var aces = new ArrayList<Ace>(aceCount);
			for (int i = 0; i < aceCount; i++) {
				if ((int) GET_ACE.invokeExact(dacl, i, acePointer) == 0) {
					throw new IOException("GetAce failed for index " + i);
				}
				var ace = acePointer.get(ADDRESS, 0).reinterpret(ACE_SID_OFFSET);
				var type = Byte.toUnsignedInt(ace.get(JAVA_BYTE, 0));
				var flags = Byte.toUnsignedInt(ace.get(JAVA_BYTE, 1));
				var mask = ace.get(JAVA_INT_UNALIGNED, ACE_MASK_OFFSET);
				var hasSidAtOffset = type == ACCESS_ALLOWED_ACE_TYPE || type == ACCESS_DENIED_ACE_TYPE || type == ACCESS_ALLOWED_CALLBACK_ACE_TYPE || type == ACCESS_DENIED_CALLBACK_ACE_TYPE;
				var sid = hasSidAtOffset ? sidString(MemorySegment.ofAddress(ace.address() + ACE_SID_OFFSET), arena) : null;
				aces.add(new Ace(type, flags, mask, sid));
			}
			return aces;
		}

		private static String sidString(MemorySegment sid, Arena arena) throws Throwable {
			var stringPointer = arena.allocate(ADDRESS);
			if ((int) CONVERT_SID_TO_STRING_SID.invokeExact(sid, stringPointer) == 0) {
				throw new IOException("ConvertSidToStringSidW failed");
			}
			var string = stringPointer.get(ADDRESS, 0);
			try {
				return string.reinterpret(MAX_SID_STRING_BYTES).getString(0, UTF_16LE);
			} finally {
				localFree(string);
			}
		}

		private static void localFree(MemorySegment memory) throws Throwable {
			var _ = (MemorySegment) LOCAL_FREE.invokeExact(memory);
		}
	}

}

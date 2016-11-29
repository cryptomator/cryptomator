package org.cryptomator.filesystem.crypto;

import static org.cryptomator.filesystem.crypto.Constants.DIR_PREFIX;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.ReadableFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ConflictResolver {

	private static final Logger LOG = LoggerFactory.getLogger(ConflictResolver.class);
	private static final int UUID_FIRST_GROUP_STRLEN = 8;
	private static final int MAX_DIR_FILE_SIZE = 87; // "normal" file header has 88 bytes

	private final Pattern encryptedNamePattern;
	private final Function<String, Optional<String>> nameDecryptor;
	private final Function<String, Optional<String>> nameEncryptor;

	public ConflictResolver(Pattern encryptedNamePattern, Function<String, Optional<String>> nameDecryptor, Function<String, Optional<String>> nameEncryptor) {
		this.encryptedNamePattern = encryptedNamePattern;
		this.nameDecryptor = nameDecryptor;
		this.nameEncryptor = nameEncryptor;
	}

	public File resolveIfNecessary(File file) {
		Matcher m = encryptedNamePattern.matcher(StringUtils.removeStart(file.name(), DIR_PREFIX));
		if (m.matches()) {
			// full match, use file as is
			return file;
		} else if (m.find(0)) {
			// partial match, might be conflicting
			return resolveConflict(file, m.toMatchResult());
		} else {
			// no match, file not relevant
			return file;
		}
	}

	private File resolveConflict(File conflictingFile, MatchResult matchResult) {
		String ciphertext = matchResult.group();
		boolean isDirectory = conflictingFile.name().startsWith(DIR_PREFIX);
		Optional<String> cleartext = nameDecryptor.apply(ciphertext);
		if (cleartext.isPresent()) {
			Folder folder = conflictingFile.parent().get();
			File canonicalFile = folder.file(isDirectory ? DIR_PREFIX + ciphertext : ciphertext);
			if (isDirectory && canonicalFile.exists() && isSameFileBasedOnSample(canonicalFile, conflictingFile, MAX_DIR_FILE_SIZE)) {
				// there must not be two directories pointing to the same directory id. In this case no human interaction is needed to resolve this conflict:
				conflictingFile.delete();
				return canonicalFile;
			} else {
				// conventional conflict detected! look for an alternative name:
				File alternativeFile;
				String conflictId;
				do {
					conflictId = createConflictId();
					String alternativeCleartext = cleartext.get() + " (Conflict " + conflictId + ")";
					String alternativeCiphertext = nameEncryptor.apply(alternativeCleartext).get();
					alternativeFile = folder.file(isDirectory ? DIR_PREFIX + alternativeCiphertext : alternativeCiphertext);
				} while (alternativeFile.exists());
				LOG.debug("Detected conflict {}:\n{}\n{}", conflictId, canonicalFile, conflictingFile);
				conflictingFile.moveTo(alternativeFile);
				return alternativeFile;
			}
		} else {
			// not decryptable; false positive
			return conflictingFile;
		}
	}

	private boolean isSameFileBasedOnSample(File file1, File file2, int sampleSize) {
		if (file1.size() != file2.size()) {
			return false;
		} else {
			try (ReadableFile r1 = file1.openReadable(); ReadableFile r2 = file2.openReadable()) {
				ByteBuffer beginOfFile1 = ByteBuffer.allocate(sampleSize);
				ByteBuffer beginOfFile2 = ByteBuffer.allocate(sampleSize);
				int bytesRead1 = r1.read(beginOfFile1);
				int bytesRead2 = r2.read(beginOfFile2);
				if (bytesRead1 == bytesRead2) {
					beginOfFile1.flip();
					beginOfFile2.flip();
					return beginOfFile1.equals(beginOfFile2);
				} else {
					return false;
				}
			}
		}
	}

	private String createConflictId() {
		return UUID.randomUUID().toString().substring(0, UUID_FIRST_GROUP_STRLEN);
	}

}

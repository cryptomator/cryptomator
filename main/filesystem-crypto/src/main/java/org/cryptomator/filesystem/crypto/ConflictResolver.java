package org.cryptomator.filesystem.crypto;

import static org.cryptomator.filesystem.crypto.Constants.DIR_SUFFIX;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.io.FileContents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ConflictResolver {

	private static final Logger LOG = LoggerFactory.getLogger(ConflictResolver.class);
	private static final int UUID_FIRST_GROUP_STRLEN = 8;

	private final Pattern encryptedNamePattern;
	private final Function<String, Optional<String>> nameDecryptor;
	private final Function<String, Optional<String>> nameEncryptor;

	public ConflictResolver(Pattern encryptedNamePattern, Function<String, Optional<String>> nameDecryptor, Function<String, Optional<String>> nameEncryptor) {
		this.encryptedNamePattern = encryptedNamePattern;
		this.nameDecryptor = nameDecryptor;
		this.nameEncryptor = nameEncryptor;
	}

	public File resolveIfNecessary(File file) {
		Matcher m = encryptedNamePattern.matcher(StringUtils.removeEnd(file.name(), DIR_SUFFIX));
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
		boolean isDirectory = conflictingFile.name().substring(matchResult.end()).startsWith(DIR_SUFFIX);
		Optional<String> cleartext = nameDecryptor.apply(ciphertext);
		if (cleartext.isPresent()) {
			Folder folder = conflictingFile.parent().get();
			File canonicalFile = folder.file(isDirectory ? ciphertext + DIR_SUFFIX : ciphertext);
			if (canonicalFile.exists()) {
				// there must not be two directories pointing to the same directory id. In this case no human interaction is needed to resolve this conflict:
				if (isDirectory && FileContents.UTF_8.readContents(canonicalFile).equals(FileContents.UTF_8.readContents(conflictingFile))) {
					conflictingFile.delete();
					return canonicalFile;
				}

				// conventional conflict detected! look for an alternative name:
				File alternativeFile;
				String conflictId;
				do {
					conflictId = createConflictId();
					String alternativeCleartext = cleartext.get() + " (Conflict " + conflictId + ")";
					String alternativeCiphertext = nameEncryptor.apply(alternativeCleartext).get();
					alternativeFile = folder.file(isDirectory ? alternativeCiphertext + DIR_SUFFIX : alternativeCiphertext);
				} while (alternativeFile.exists());
				LOG.info("Detected conflict {}:\n{}\n{}", conflictId, canonicalFile, conflictingFile);
				conflictingFile.moveTo(alternativeFile);
				return alternativeFile;
			} else {
				conflictingFile.moveTo(canonicalFile);
				return canonicalFile;
			}
		} else {
			// not decryptable; false positive
			return conflictingFile;
		}
	}

	private String createConflictId() {
		return UUID.randomUUID().toString().substring(0, UUID_FIRST_GROUP_STRLEN);
	}

}

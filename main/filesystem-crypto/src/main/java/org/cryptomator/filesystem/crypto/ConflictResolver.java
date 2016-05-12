package org.cryptomator.filesystem.crypto;

import static org.cryptomator.filesystem.crypto.Constants.DIR_SUFFIX;

import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.cryptomator.crypto.engine.CryptoException;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ConflictResolver {

	private static final Logger LOG = LoggerFactory.getLogger(ConflictResolver.class);
	private static final int UUID_FIRST_GROUP_STRLEN = 8;

	private final Pattern encryptedNamePattern;
	private final UnaryOperator<String> nameDecryptor;
	private final UnaryOperator<String> nameEncryptor;

	public ConflictResolver(Pattern encryptedNamePattern, UnaryOperator<String> nameDecryptor, UnaryOperator<String> nameEncryptor) {
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
		try {
			String cleartext = nameDecryptor.apply(ciphertext);
			Folder folder = conflictingFile.parent().get();
			File canonicalFile = folder.file(isDirectory ? ciphertext + DIR_SUFFIX : ciphertext);
			if (canonicalFile.exists()) {
				// conflict detected! look for an alternative name:
				File alternativeFile;
				String conflictId;
				do {
					conflictId = createConflictId();
					String alternativeCleartext = cleartext + " (Conflict " + conflictId + ")";
					String alternativeCiphertext = nameEncryptor.apply(alternativeCleartext);
					alternativeFile = folder.file(isDirectory ? alternativeCiphertext + DIR_SUFFIX : alternativeCiphertext);
				} while (alternativeFile.exists());
				LOG.info("Detected conflict {}:\n{}\n{}", conflictId, canonicalFile, conflictingFile);
				conflictingFile.moveTo(alternativeFile);
				return alternativeFile;
			} else {
				conflictingFile.moveTo(canonicalFile);
				return canonicalFile;
			}
		} catch (CryptoException e) {
			// not decryptable; false positive
			return conflictingFile;
		}
	}

	private String createConflictId() {
		return UUID.randomUUID().toString().substring(0, UUID_FIRST_GROUP_STRLEN);
	}

}

package org.cryptomator.filesystem.shortening;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ConflictResolver {

	private static final Logger LOG = LoggerFactory.getLogger(ConflictResolver.class);
	private static final String LONG_NAME_FILE_EXT = ".lng";
	private static final Pattern BASE32_PATTERN = Pattern.compile("^0?([A-Z2-7]{8})*[A-Z2-7=]{8}");
	private static final int UUID_FIRST_GROUP_STRLEN = 8;

	private ConflictResolver() {
	}

	public static File resolveConflictIfNecessary(File potentiallyConflictingFile, FilenameShortener shortener) {
		String shortName = potentiallyConflictingFile.name();
		String basename = StringUtils.removeEnd(shortName, LONG_NAME_FILE_EXT);
		Matcher matcher = BASE32_PATTERN.matcher(basename);
		if (shortName.endsWith(LONG_NAME_FILE_EXT) && matcher.matches()) {
			// no conflict.
			return potentiallyConflictingFile;
		} else if (shortName.endsWith(LONG_NAME_FILE_EXT) && matcher.find(0)) {
			String canonicalShortName = matcher.group() + LONG_NAME_FILE_EXT;
			return resolveConflict(potentiallyConflictingFile, canonicalShortName, shortener);
		} else {
			// not even shortened at all.
			return potentiallyConflictingFile;
		}
	}

	private static File resolveConflict(File conflictingFile, String canonicalShortName, FilenameShortener shortener) {
		Folder parent = conflictingFile.parent().get();
		File canonicalFile = parent.file(canonicalShortName);
		if (canonicalFile.exists()) {
			// foo (1).lng -> bar.lng
			String canonicalLongName = shortener.inflate(canonicalShortName);
			String alternativeLongName;
			String alternativeShortName;
			File alternativeFile;
			String conflictId;
			do {
				conflictId = createConflictId();
				alternativeLongName = canonicalLongName + " (Conflict " + conflictId + ")";
				alternativeShortName = shortener.deflate(alternativeLongName);
				alternativeFile = parent.file(alternativeShortName);
			} while (alternativeFile.exists());
			LOG.info("Detected conflict {}:\n{}\n{}", conflictId, canonicalFile, conflictingFile);
			conflictingFile.moveTo(alternativeFile);
			shortener.saveMapping(alternativeLongName, alternativeShortName);
			return alternativeFile;
		} else {
			// foo (1).lng -> foo.lng
			conflictingFile.moveTo(canonicalFile);
			return canonicalFile;
		}
	}

	private static String createConflictId() {
		return UUID.randomUUID().toString().substring(0, UUID_FIRST_GROUP_STRLEN);
	}

}

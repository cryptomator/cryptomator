package org.cryptomator.event;

import org.cryptomator.cryptofs.common.Constants;
import org.cryptomator.cryptofs.event.FileWriteConflictEvent;

import java.nio.file.Path;
import java.util.Set;

public class EventFileName {

	private static final Set<String> NODE_FILE_NAMES = Set.of(Constants.CONTENTS_FILE_NAME, Constants.SYMLINK_FILE_NAME);

	private EventFileName() {}

	public static String of(FileWriteConflictEvent event) {
		var cleartextPath = event.cleartextPath();
		if (cleartextPath != null) {
			return cleartextPath.substring(cleartextPath.lastIndexOf('/') + 1);
		}
		return ofCiphertext(event.ciphertextPath());
	}

	public static String ofCiphertext(Path ciphertextPath) {
		if (NODE_FILE_NAMES.contains(ciphertextPath.getFileName().toString())) {
			return ciphertextPath.getParent().getFileName().toString();
		}
		return ciphertextPath.getFileName().toString();
	}

}

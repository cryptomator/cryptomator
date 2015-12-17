package org.cryptomator.filesystem.nio;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.cryptomator.filesystem.FolderCreateMode;

enum NioFolderCreateMode {

	FAIL_IF_PARENT_IS_MISSING {
		@Override
		void create(Path folderPath) {
			try {
				Files.createDirectory(folderPath);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	},
	INCLUDING_PARENTS {
		@Override
		void create(Path folderPath) {
			try {
				Files.createDirectories(folderPath);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	;

	public static NioFolderCreateMode valueOf(FolderCreateMode mode) {
		return valueOf(mode.name());
	}

	abstract void create(Path folderPath);

}

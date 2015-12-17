package org.cryptomator.filesystem.nio;

import java.nio.file.Path;
import java.util.Optional;

interface NioNodeFactory {

	NioFile file(Optional<NioFolder> parent, Path path);

	NioFolder folder(Optional<NioFolder> parent, Path path);

}

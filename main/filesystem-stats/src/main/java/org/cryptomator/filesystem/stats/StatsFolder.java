package org.cryptomator.filesystem.stats;

import java.util.function.Consumer;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.delegating.DelegatingFolder;

public class StatsFolder extends DelegatingFolder<StatsFolder, StatsFile> {

	private final Consumer<Long> readCounter;
	private final Consumer<Long> writeCounter;

	public StatsFolder(StatsFolder parent, Folder delegate, Consumer<Long> readCounter, Consumer<Long> writeCounter) {
		super(parent, delegate);
		this.readCounter = readCounter;
		this.writeCounter = writeCounter;
	}

	@Override
	protected StatsFile newFile(File delegate) {
		return new StatsFile(this, delegate, readCounter, writeCounter);
	}

	@Override
	protected StatsFolder newFolder(Folder delegate) {
		return new StatsFolder(this, delegate, readCounter, writeCounter);
	}

}

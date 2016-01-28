package org.cryptomator.filesystem.stats;

import java.util.concurrent.atomic.LongAdder;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;

public class StatsFileSystem extends StatsFolder implements FileSystem {

	private final LongAdder read;
	private final LongAdder written;

	public StatsFileSystem(Folder root) {
		this(root, new LongAdder(), new LongAdder());
	}

	private StatsFileSystem(Folder root, LongAdder read, LongAdder written) {
		super(null, root, read::add, written::add);
		this.read = read;
		this.written = written;
	}

	public long getThenResetBytesRead() {
		return read.sumThenReset();
	}

	public long getThenResetBytesWritten() {
		return written.sumThenReset();
	}

}

package org.cryptomator.filesystem;

class Mover {

	public static void move(File source, File destination) {
		try (OpenFiles openFiles = DeadlockSafeFileOpener.withWritable(source).andWritable(destination).open()) {
			openFiles.writable(source).moveTo(openFiles.writable(destination));
		}
	}

}

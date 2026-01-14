package org.cryptomator.launcher;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

class BufferedLog {

	private final static List<Entry> logMessages = new ArrayList<>();

	record Entry(String className, String message, List<Object> messageInput) {}

	static void log(String className, String message, List<Object> messageInput) {
		add(new BufferedLog.Entry(className, message, messageInput));
	}

	static void add(Entry e) {
		logMessages.add(e);
	}

	public static void flush(Logger log) {
		logMessages.forEach(e -> {
			var message = "PRE LOG INIT Event in %s: %s".formatted(e.className, e.message);
			log.info(message, e.messageInput.toArray());
		});
	}
}

package org.cryptomator.ui.recoverykey;

import javafx.concurrent.Task;

final class RecoveryKeyTasks {

	private RecoveryKeyTasks() {
	}

	@FunctionalInterface
	interface TaskAction {
		void run() throws Exception;
	}

	static Task<Void> createTask(TaskAction action) {
		return new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				action.run();
				return null;
			}
		};
	}

}

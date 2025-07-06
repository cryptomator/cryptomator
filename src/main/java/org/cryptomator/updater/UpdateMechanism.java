package org.cryptomator.updater;

import org.cryptomator.integrations.common.IntegrationsLoader;
import org.cryptomator.integrations.common.NamedServiceProvider;
import org.jetbrains.annotations.Blocking;

import javafx.concurrent.Task;
import java.io.IOException;
import java.util.stream.Stream;

public interface UpdateMechanism extends NamedServiceProvider {

	static Stream<UpdateMechanism> get() {
		return IntegrationsLoader.loadAll(UpdateMechanism.class);
	}

	/**
	 * Checks whether an update is available.
	 * @return <code>true</code> if an update is available, <code>false</code> otherwise.
	 */
	@Blocking
	boolean isUpdateAvailable();

	/**
	 * Performs as much as possible to prepare the update. This may include downloading the update, checking signatures, etc.
	 * @return a {@link Task} that can be used to monitor the progress of the update preparation. The task will complete when the preparation is done.
	 * @throws IOException I/O error during preparation, such as network issues or file access problems.
	 */
	UpdateProcess prepareUpdate() throws IOException; // TODO: exception types?

}

package org.cryptomator.ui.addvaultwizard;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import javax.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@AddVaultWizardScoped
public class LocationPresets {

	private static final String USER_HOME = System.getProperty("user.home");
	private static final String[] DROPBOX_LOCATIONS = {"~/Dropbox"};
	private static final String[] GDRIVE_LOCATIONS = {"~/Google Drive"};

	private final ReadOnlyObjectProperty<Path> dropboxLocation;
	private final ReadOnlyObjectProperty<Path> gdriveLocation;
	private final BooleanBinding foundDropbox;
	private final BooleanBinding foundGdrive;

	@Inject
	public LocationPresets() {
		this.dropboxLocation = new SimpleObjectProperty<>(existingWritablePath(DROPBOX_LOCATIONS));
		this.gdriveLocation = new SimpleObjectProperty<>(existingWritablePath(GDRIVE_LOCATIONS));
		this.foundDropbox = dropboxLocation.isNotNull();
		this.foundGdrive = gdriveLocation.isNotNull();
	}

	private static Path existingWritablePath(String... candidates) {
		for (String candidate : candidates) {
			Path path = Paths.get(resolveHomePath(candidate));
			if (Files.isDirectory(path)) {
				return path;
			}
		}
		return null;
	}

	private static String resolveHomePath(String path) {
		if (path.startsWith("~/")) {
			return USER_HOME + path.substring(1);
		} else {
			return path;
		}
	}

	/* Observables */

	public ReadOnlyObjectProperty<Path> dropboxLocationProperty() {
		return dropboxLocation;
	}

	public Path getDropboxLocation() {
		return dropboxLocation.get();
	}

	public BooleanBinding foundDropboxProperty() {
		return foundDropbox;
	}

	public boolean isFoundDropbox() {
		return foundDropbox.get();
	}

	public ReadOnlyObjectProperty<Path> gdriveLocationProperty() {
		return gdriveLocation;
	}

	public Path getGdriveLocation() {
		return gdriveLocation.get();
	}

	public BooleanBinding froundGdriveProperty() {
		return foundGdrive;
	}

	public boolean isFoundGdrive() {
		return foundGdrive.get();
	}

}

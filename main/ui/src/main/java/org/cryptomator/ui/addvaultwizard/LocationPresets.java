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
	private static final String[] ICLOUDDRIVE_LOCATIONS = {"~/Library/Mobile Documents/iCloud~com~setolabs~Cryptomator/Documents", "~/iCloudDrive/iCloud~com~setolabs~Cryptomator"};
	private static final String[] DROPBOX_LOCATIONS = {"~/Dropbox"};
	private static final String[] GDRIVE_LOCATIONS = {"~/Google Drive"};
	private static final String[] ONEDRIVE_LOCATIONS = {"~/OneDrive"};

	private final ReadOnlyObjectProperty<Path> iclouddriveLocation;
	private final ReadOnlyObjectProperty<Path> dropboxLocation;
	private final ReadOnlyObjectProperty<Path> gdriveLocation;
	private final ReadOnlyObjectProperty<Path> onedriveLocation;
	private final BooleanBinding foundIclouddrive;
	private final BooleanBinding foundDropbox;
	private final BooleanBinding foundGdrive;
	private final BooleanBinding foundOnedrive;

	@Inject
	public LocationPresets() {
		this.iclouddriveLocation = new SimpleObjectProperty<>(existingWritablePath(ICLOUDDRIVE_LOCATIONS));
		this.dropboxLocation = new SimpleObjectProperty<>(existingWritablePath(DROPBOX_LOCATIONS));
		this.gdriveLocation = new SimpleObjectProperty<>(existingWritablePath(GDRIVE_LOCATIONS));
		this.onedriveLocation = new SimpleObjectProperty<>(existingWritablePath(ONEDRIVE_LOCATIONS));
		this.foundIclouddrive = iclouddriveLocation.isNotNull();
		this.foundDropbox = dropboxLocation.isNotNull();
		this.foundGdrive = gdriveLocation.isNotNull();
		this.foundOnedrive = onedriveLocation.isNotNull();
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

	public ReadOnlyObjectProperty<Path> iclouddriveLocationProperty() {
		return iclouddriveLocation;
	}

	public Path getIclouddriveLocation() {
		return iclouddriveLocation.get();
	}

	public BooleanBinding foundIclouddriveProperty() {
		return foundIclouddrive;
	}

	public boolean isFoundIclouddrive() {
		return foundIclouddrive.get();
	}

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

	public BooleanBinding foundGdriveProperty() {
		return foundGdrive;
	}

	public boolean isFoundGdrive() {
		return foundGdrive.get();
	}

	public ReadOnlyObjectProperty<Path> onedriveLocationProperty() {
		return onedriveLocation;
	}

	public Path getOnedriveLocation() {
		return onedriveLocation.get();
	}

	public BooleanBinding foundOnedriveProperty() {
		return foundOnedrive;
	}

	public boolean isFoundOnedrive() {
		return foundOnedrive.get();
	}

}

package org.cryptomator.ui.addvaultwizard;

import org.cryptomator.common.LocationPreset;

import javax.inject.Inject;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import java.nio.file.Path;

@AddVaultWizardScoped
public class ObservedLocationPresets {

	private final ReadOnlyObjectProperty<Path> iclouddriveLocation;
	private final ReadOnlyObjectProperty<Path> dropboxLocation;
	private final ReadOnlyObjectProperty<Path> gdriveLocation;
	private final ReadOnlyObjectProperty<Path> onedriveLocation;
	private final ReadOnlyObjectProperty<Path> megaLocation;
	private final ReadOnlyObjectProperty<Path> pcloudLocation;
	private final BooleanBinding foundIclouddrive;
	private final BooleanBinding foundDropbox;
	private final BooleanBinding foundGdrive;
	private final BooleanBinding foundOnedrive;
	private final BooleanBinding foundMega;
	private final BooleanBinding foundPcloud;

	@Inject
	public ObservedLocationPresets() {
		this.iclouddriveLocation = new SimpleObjectProperty<>(LocationPreset.ICLOUDDRIVE.existingPath());
		this.dropboxLocation = new SimpleObjectProperty<>(LocationPreset.DROPBOX.existingPath());
		this.gdriveLocation = new SimpleObjectProperty<>(LocationPreset.GDRIVE.existingPath());
		this.onedriveLocation = new SimpleObjectProperty<>(LocationPreset.ONEDRIVE.existingPath());
		this.megaLocation = new SimpleObjectProperty<>(LocationPreset.MEGA.existingPath());
		this.pcloudLocation = new SimpleObjectProperty<>(LocationPreset.PCLOUD.existingPath());
		this.foundIclouddrive = iclouddriveLocation.isNotNull();
		this.foundDropbox = dropboxLocation.isNotNull();
		this.foundGdrive = gdriveLocation.isNotNull();
		this.foundOnedrive = onedriveLocation.isNotNull();
		this.foundMega = megaLocation.isNotNull();
		this.foundPcloud = pcloudLocation.isNotNull();
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

	public ReadOnlyObjectProperty<Path> megaLocationProperty() {
		return megaLocation;
	}

	public Path getMegaLocation() {
		return megaLocation.get();
	}

	public BooleanBinding foundMegaProperty() {
		return foundMega;
	}

	public boolean isFoundMega() {
		return foundMega.get();
	}

	public ReadOnlyObjectProperty<Path> pcloudLocationProperty() {
		return pcloudLocation;
	}

	public Path getPcloudLocation() {
		return pcloudLocation.get();
	}

	public BooleanBinding foundPcloudProperty() {
		return foundPcloud;
	}

	public boolean isFoundPcloud() {
		return foundPcloud.get();
	}

}

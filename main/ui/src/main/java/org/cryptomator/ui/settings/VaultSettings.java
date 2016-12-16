package org.cryptomator.ui.settings;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

import com.google.gson.annotations.JsonAdapter;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

@JsonAdapter(VaultSettingsJsonAdapter.class)
public class VaultSettings {

	private final String id;
	private final Property<Path> path = new SimpleObjectProperty<>();
	private final Property<String> mountName = new SimpleStringProperty();
	private final Property<String> winDriveLetter = new SimpleStringProperty();

	public VaultSettings(String id) {
		this.id = Objects.requireNonNull(id);
	}

	public static VaultSettings withRandomId() {
		return new VaultSettings(generateId());
	}

	private static String generateId() {
		return asBase64String(nineBytesFrom(UUID.randomUUID()));
	}

	private static String asBase64String(byte[] bytes) {
		byte[] base64Bytes = Base64.getUrlEncoder().encode(bytes);
		return new String(base64Bytes, StandardCharsets.US_ASCII);
	}

	private static byte[] nineBytesFrom(UUID uuid) {
		ByteBuffer uuidBuffer = ByteBuffer.allocate(9);
		uuidBuffer.putLong(uuid.getMostSignificantBits());
		uuidBuffer.put((byte) (uuid.getLeastSignificantBits() & 0xFF));
		uuidBuffer.flip();
		return uuidBuffer.array();
	}

	/* Getter/Setter */

	public String getId() {
		return id;
	}

	public Property<Path> pathProperty() {
		return path;
	}

	public Path getPath() {
		return path.getValue();
	}

	public void setPath(Path path) {
		this.path.setValue(path);
	}

	public Property<String> mountNameProperty() {
		return mountName;
	}

	public String getMountName() {
		return mountName.getValue();
	}

	public void setMountName(String mountName) {
		this.mountName.setValue(mountName);
	}

	public Property<String> winDriveLetterProperty() {
		return mountName;
	}

	public String getWinDriveLetter() {
		return winDriveLetter.getValue();
	}

	public void setWinDriveLetter(String winDriveLetter) {
		this.winDriveLetter.setValue(winDriveLetter);
	}

	/* Hashcode/Equals */

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof VaultSettings && obj.getClass().equals(this.getClass())) {
			VaultSettings other = (VaultSettings) obj;
			return Objects.equals(this.id, other.id);
		} else {
			return false;
		}
	}

}

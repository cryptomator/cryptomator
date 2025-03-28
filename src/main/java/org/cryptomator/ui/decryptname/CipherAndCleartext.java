package org.cryptomator.ui.decryptname;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import java.nio.file.Path;

public record CipherAndCleartext(Path ciphertext, String cleartextName) {

	public String getCiphertextFilename() {
		return ciphertext.getFileName().toString();
	}

	public ObservableValue<String> ciphertextFilenameProperty() {
		return new ReadOnlyStringWrapper(getCiphertextFilename());
	}

	public String getCleartextName() {
		return cleartextName;
	}

	public ObservableValue<String> cleartextNameProperty() {
		return new ReadOnlyStringWrapper(getCleartextName());
	}

}

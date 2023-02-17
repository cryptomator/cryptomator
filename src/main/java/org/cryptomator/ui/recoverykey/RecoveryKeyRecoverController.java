package org.cryptomator.ui.recoverykey;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import dagger.Lazy;
import org.cryptomator.common.Nullable;
import org.cryptomator.common.ObservableUtil;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptofs.VaultConfigLoadException;
import org.cryptomator.cryptofs.VaultKeyInvalidException;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import java.util.Optional;
import java.util.ResourceBundle;

@RecoveryKeyScoped
public class RecoveryKeyRecoverController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(RecoveryKeyCreationController.class);
	private static final CharMatcher ALLOWED_CHARS = CharMatcher.inRange('a', 'z').or(CharMatcher.is(' '));

	private final Stage window;
	private final Vault vault;
	private final VaultConfig.UnverifiedVaultConfig unverifiedVaultConfig;
	private final StringProperty recoveryKey;
	private final ObservableValue<Boolean> recoveryKeyCorrect;
	private final ObservableValue<Boolean> recoveryKeyWrong;
	private final ObservableValue<Boolean> recoveryKeyInvalid;
	private final RecoveryKeyFactory recoveryKeyFactory;

	private final ObjectProperty<RecoveryKeyState> recoveryKeyState;
	private final Lazy<Scene> resetPasswordScene;
	private final AutoCompleter autoCompleter;

	public TextArea textarea;

	@Inject
	public RecoveryKeyRecoverController(@RecoveryKeyWindow Stage window, @RecoveryKeyWindow Vault vault, @RecoveryKeyWindow @Nullable VaultConfig.UnverifiedVaultConfig unverifiedVaultConfig, @RecoveryKeyWindow StringProperty recoveryKey, RecoveryKeyFactory recoveryKeyFactory, @FxmlScene(FxmlFile.RECOVERYKEY_RESET_PASSWORD) Lazy<Scene> resetPasswordScene, ResourceBundle resourceBundle) {
		this.window = window;
		window.setTitle(resourceBundle.getString("recoveryKey.recover.title"));
		this.vault = vault;
		this.unverifiedVaultConfig = unverifiedVaultConfig;
		this.recoveryKey = recoveryKey;
		this.recoveryKeyFactory = recoveryKeyFactory;
		this.resetPasswordScene = resetPasswordScene;
		this.autoCompleter = new AutoCompleter(recoveryKeyFactory.getDictionary());
		this.recoveryKeyState = new SimpleObjectProperty<>();
		this.recoveryKeyCorrect = ObservableUtil.mapWithDefault(recoveryKeyState, RecoveryKeyState.CORRECT::equals, false);
		this.recoveryKeyWrong = ObservableUtil.mapWithDefault(recoveryKeyState, RecoveryKeyState.WRONG::equals, false);
		this.recoveryKeyInvalid = ObservableUtil.mapWithDefault(recoveryKeyState, RecoveryKeyState.INVALID::equals, false);
	}

	@FXML
	public void initialize() {
		recoveryKey.bind(textarea.textProperty());
		textarea.textProperty().addListener(((observable, oldValue, newValue) -> validateRecoveryKey()));
	}

	private TextFormatter.Change filterTextChange(TextFormatter.Change change) {
		if (Strings.isNullOrEmpty(change.getText())) {
			// pass-through caret/selection changes that don't affect the text
			return change;
		}
		if (!ALLOWED_CHARS.matchesAllOf(change.getText())) {
			return null; // reject change
		}

		String text = change.getControlNewText();
		int caretPos = change.getCaretPosition();
		if (caretPos == text.length() || text.charAt(caretPos) == ' ') { // are we at the end of a word?
			int beginOfWord = Math.max(text.substring(0, caretPos).lastIndexOf(' ') + 1, 0);
			String currentWord = text.substring(beginOfWord, caretPos);
			Optional<String> suggestion = autoCompleter.autocomplete(currentWord);
			if (suggestion.isPresent()) {
				String completion = suggestion.get().substring(currentWord.length());
				change.setText(change.getText() + completion);
				change.setAnchor(caretPos + completion.length());
			}
		}
		return change;
	}

	@FXML
	public void onKeyPressed(KeyEvent keyEvent) {
		if (keyEvent.getCode() == KeyCode.TAB && textarea.getAnchor() > textarea.getCaretPosition()) {
			// apply autocompletion:
			int pos = textarea.getAnchor();
			textarea.insertText(pos, " ");
			textarea.positionCaret(pos + 1);
		}
	}

	@FXML
	public void close() {
		window.close();
	}

	@FXML
	public void recover() {
		window.setScene(resetPasswordScene.get());
	}

	/**
	 * Checks, if vault config is signed with the given key.
	 * <p>
	 * If not, but the deriving recovery key is valid, sets the recoveryKeyState to WRONG.
	 *
	 * @param key byte array of possible signing key
	 * @return true, if vault config is signed with this key
	 */
	private boolean checkKeyAgainstVaultConfig(byte[] key) {
		try {
			var config = unverifiedVaultConfig.verify(key, unverifiedVaultConfig.allegedVaultVersion());
			LOG.info("Provided recovery key matches vault config signature for vault {}", config.getId());
			return true;
		} catch (VaultKeyInvalidException e) {
			LOG.debug("Provided recovery key does not match vault config signature.");
			recoveryKeyState.setValue(RecoveryKeyState.WRONG);
			return false;
		} catch (VaultConfigLoadException e) {
			LOG.error("Failed to parse vault config", e);
			return false;
		}
	}

	public void validateRecoveryKey() {
		var valid = recoveryKeyFactory.validateRecoveryKey(recoveryKey.get(), unverifiedVaultConfig != null ? this::checkKeyAgainstVaultConfig : null);
		if (valid) {
			recoveryKeyState.set(RecoveryKeyState.CORRECT);
		} else if (recoveryKeyState.getValue() != RecoveryKeyState.WRONG) { // set via side effect in checkKeyAgainstVaultConfig()
			recoveryKeyState.set(RecoveryKeyState.INVALID);
		}
	}

	/* Getter/Setter */

	public Vault getVault() {
		return vault;
	}

	public TextFormatter getRecoveryKeyTextFormatter() {
		return new TextFormatter<>(this::filterTextChange);
	}

	public ObservableValue<Boolean> recoveryKeyInvalidProperty() {
		return recoveryKeyInvalid;
	}

	public boolean isRecoveryKeyInvalid() {
		return recoveryKeyInvalid.getValue();
	}

	public ObservableValue<Boolean> recoveryKeyCorrectProperty() {
		return recoveryKeyCorrect;
	}

	public boolean isRecoveryKeyCorrect() {
		return recoveryKeyCorrect.getValue();
	}

	public ObservableValue<Boolean> recoveryKeyWrongProperty() {
		return recoveryKeyWrong;
	}

	public boolean isRecoveryKeyWrong() {
		return recoveryKeyWrong.getValue();
	}

	private enum RecoveryKeyState {
		/**
		 * Recovery key is a valid key and belongs to this vault
		 */
		CORRECT,
		/**
		 * Recovery key is a valid key, but does not belong to this vault
		 */
		WRONG,
		/**
		 * Recovery key is not a valid key.
		 */
		INVALID;
	}
}

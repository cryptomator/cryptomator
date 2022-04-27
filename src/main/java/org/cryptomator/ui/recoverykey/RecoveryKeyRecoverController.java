package org.cryptomator.ui.recoverykey;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import dagger.Lazy;
import org.cryptomator.common.Nullable;
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
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import java.util.Optional;

@RecoveryKeyScoped
public class RecoveryKeyRecoverController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(RecoveryKeyCreationController.class);
	private static final CharMatcher ALLOWED_CHARS = CharMatcher.inRange('a', 'z').or(CharMatcher.is(' '));

	private final Stage window;
	private final Vault vault;
	private final VaultConfig.UnverifiedVaultConfig unverifiedVaultConfig;
	private final StringProperty recoveryKey;
	private final RecoveryKeyFactory recoveryKeyFactory;
	private final BooleanBinding validRecoveryKey;
	private final Lazy<Scene> resetPasswordScene;
	private final AutoCompleter autoCompleter;

	public TextArea textarea;

	@Inject
	public RecoveryKeyRecoverController(@RecoveryKeyWindow Stage window, @RecoveryKeyWindow Vault vault, @RecoveryKeyWindow @Nullable VaultConfig.UnverifiedVaultConfig unverifiedVaultConfig, @RecoveryKeyWindow StringProperty recoveryKey, RecoveryKeyFactory recoveryKeyFactory, @FxmlScene(FxmlFile.RECOVERYKEY_RESET_PASSWORD) Lazy<Scene> resetPasswordScene) {
		this.window = window;
		this.vault = vault;
		this.unverifiedVaultConfig = unverifiedVaultConfig;
		this.recoveryKey = recoveryKey;
		this.recoveryKeyFactory = recoveryKeyFactory;
		this.validRecoveryKey = Bindings.createBooleanBinding(this::isValidRecoveryKey, recoveryKey);
		this.resetPasswordScene = resetPasswordScene;
		this.autoCompleter = new AutoCompleter(recoveryKeyFactory.getDictionary());
	}

	@FXML
	public void initialize() {
		recoveryKey.bind(textarea.textProperty());
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

	private boolean checkKeyAgainstVaultConfig(byte[] key) {
		try {
			var config = unverifiedVaultConfig.verify(key, unverifiedVaultConfig.allegedVaultVersion());
			LOG.info("Provided recovery key matches vault config signature for vault {}", config.getId());
			return true;
		} catch (VaultKeyInvalidException e) {
			LOG.debug("Provided recovery key does not match vault config signature.");
			return false;
		} catch (VaultConfigLoadException e) {
			LOG.error("Failed to parse vault config", e);
			return false;
		}
	}

	/* Getter/Setter */

	public Vault getVault() {
		return vault;
	}

	public BooleanBinding validRecoveryKeyProperty() {
		return validRecoveryKey;
	}

	public boolean isValidRecoveryKey() {
		if (unverifiedVaultConfig != null) {
			return recoveryKeyFactory.validateRecoveryKey(recoveryKey.get(), this::checkKeyAgainstVaultConfig);
		} else {
			return recoveryKeyFactory.validateRecoveryKey(recoveryKey.get());
		}
	}

	public TextFormatter getRecoveryKeyTextFormatter() {
		return new TextFormatter<>(this::filterTextChange);
	}
}

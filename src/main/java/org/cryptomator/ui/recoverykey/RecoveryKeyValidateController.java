package org.cryptomator.ui.recoverykey;


import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import org.cryptomator.common.Nullable;
import org.cryptomator.common.ObservableUtil;
import org.cryptomator.common.RecoverUtil;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptofs.VaultConfigLoadException;
import org.cryptomator.cryptofs.VaultKeyInvalidException;
import org.cryptomator.cryptolib.api.CryptorProvider;
import org.cryptomator.cryptolib.common.MasterkeyFileAccess;
import org.cryptomator.ui.common.FxController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import java.util.concurrent.atomic.AtomicBoolean;

public class RecoveryKeyValidateController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(RecoveryKeyCreationController.class);
	private static final CharMatcher ALLOWED_CHARS = CharMatcher.inRange('a', 'z').or(CharMatcher.is(' '));

	private final Vault vault;
	private final VaultConfig.UnverifiedVaultConfig unverifiedVaultConfig;
	private final StringProperty recoveryKey;
	private final ObservableValue<Boolean> recoveryKeyCorrect;
	private final ObservableValue<Boolean> recoveryKeyWrong;
	private final ObservableValue<Boolean> recoveryKeyInvalid;
	private final RecoveryKeyFactory recoveryKeyFactory;
	private final ObjectProperty<RecoveryKeyState> recoveryKeyState;
	private final ObjectProperty<CryptorProvider.Scheme> cipherCombo;
	private final AutoCompleter autoCompleter;
	private final ObjectProperty<RecoverUtil.Type> recoverType;
	private final MasterkeyFileAccess masterkeyFileAccess;

	private volatile boolean isWrongKey;

	public TextArea textarea;

	public RecoveryKeyValidateController(Vault vault, //
										 @Nullable VaultConfig.UnverifiedVaultConfig vaultConfig, //
										 StringProperty recoveryKey, //
										 RecoveryKeyFactory recoveryKeyFactory, //
										 @Named("recoverType") ObjectProperty<RecoverUtil.Type> recoverType, //
										 @Named("cipherCombo") ObjectProperty<CryptorProvider.Scheme> cipherCombo,//
										 MasterkeyFileAccess masterkeyFileAccess) {
		this.vault = vault;
		this.unverifiedVaultConfig = vaultConfig;
		this.recoveryKey = recoveryKey;
		this.recoveryKeyFactory = recoveryKeyFactory;
		this.autoCompleter = new AutoCompleter(recoveryKeyFactory.getDictionary());
		this.recoveryKeyState = new SimpleObjectProperty<>();
		this.recoveryKeyCorrect = ObservableUtil.mapWithDefault(recoveryKeyState, RecoveryKeyState.CORRECT::equals, false);
		this.recoveryKeyWrong = ObservableUtil.mapWithDefault(recoveryKeyState, RecoveryKeyState.WRONG::equals, false);
		this.recoveryKeyInvalid = ObservableUtil.mapWithDefault(recoveryKeyState, RecoveryKeyState.INVALID::equals, false);
		this.recoverType = recoverType;
		this.cipherCombo = cipherCombo;
		this.masterkeyFileAccess = masterkeyFileAccess;
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
			var suggestion = autoCompleter.autocomplete(currentWord);
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

	/**
	 * Checks, if vault config is signed with the given key.
	 *
	 * @param key byte array of possible signing key
	 * @return true, if vault config is signed with this key
	 */
	private boolean checkKeyAgainstVaultConfig(byte[] key) {
		assert unverifiedVaultConfig != null;
		try {
			var config = unverifiedVaultConfig.verify(key, unverifiedVaultConfig.allegedVaultVersion());
			LOG.info("Provided recovery key matches vault config signature for vault {}", config.getId());
			return true;
		} catch (VaultKeyInvalidException e) {
			LOG.debug("Provided recovery key does not match vault config signature.");
			isWrongKey = true;
			return false;
		} catch (VaultConfigLoadException e) {
			LOG.error("Failed to parse vault config", e);
			return false;
		}
	}

	private void validateRecoveryKey() {
		switch (recoverType.get()) {
			case RESTORE_VAULT_CONFIG -> {
				AtomicBoolean illegalArgumentExceptionOccurred = new AtomicBoolean(false);
				var combo = RecoverUtil.validateRecoveryKeyAndGetCombo(
						recoveryKeyFactory, vault, recoveryKey, masterkeyFileAccess, illegalArgumentExceptionOccurred);
				combo.ifPresent(cipherCombo::set);
				if (illegalArgumentExceptionOccurred.get()) {
					recoveryKeyState.set(RecoveryKeyState.INVALID);
				} else if (combo.isPresent()) {
					recoveryKeyState.set(RecoveryKeyState.CORRECT);
				} else {
					recoveryKeyState.set(RecoveryKeyState.WRONG);
				}
			}
			case RESTORE_MASTERKEY, RESET_PASSWORD, SHOW_KEY, CONVERT_VAULT -> {
				isWrongKey = false;
				boolean valid = recoveryKeyFactory.validateRecoveryKey(recoveryKey.get(),
						unverifiedVaultConfig != null ? this::checkKeyAgainstVaultConfig : null);
				if (valid) {
					recoveryKeyState.set(RecoveryKeyState.CORRECT);
				} else if (isWrongKey) { //set via side effect in checkKeyAgainstVaultConfig()
					recoveryKeyState.set(RecoveryKeyState.WRONG);
				} else {
					recoveryKeyState.set(RecoveryKeyState.INVALID);
				}
			}
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

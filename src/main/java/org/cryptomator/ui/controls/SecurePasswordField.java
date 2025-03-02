/*******************************************************************************
 * Copyright (c) 2014, 2017 Sebastian Stenzel
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui.controls;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Arrays;

import org.cryptomator.common.Passphrase;

import com.google.common.base.Strings;

import javafx.application.Platform;
import javafx.beans.NamedArg;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.control.IndexRange;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;

/**
 * Patched PasswordField that doesn't create String copies of the password in memory (unless explicitly revealed). Instead the password is stored in a char[] that can be swiped.
 *
 * @implNote Since {@link #setText(String)} is final, we can not override its behaviour. For that reason you should not use the {@link #textProperty()} for anything else than display purposes.
 */
public class SecurePasswordField extends TextField {

	private static final char WIPE_CHAR = ' ';
	private static final int INITIAL_BUFFER_SIZE = 50;
	private static final int GROW_BUFFER_SIZE = 50;
	private static final String DEFAULT_PLACEHOLDER = "•";
	private static final String STYLE_CLASS = "secure-password-field";
	private static final KeyCodeCombination SHORTCUT_BACKSPACE = new KeyCodeCombination(KeyCode.BACK_SPACE, KeyCombination.SHORTCUT_DOWN);

	private final String placeholderChar;
	private final BooleanProperty capsLocked = new SimpleBooleanProperty();
	private final BooleanProperty containingNonPrintableChars = new SimpleBooleanProperty();
	private final BooleanProperty revealPassword = new SimpleBooleanProperty();

	private char[] content = new char[INITIAL_BUFFER_SIZE];
	private int length = 0;

	public SecurePasswordField() {
		this(DEFAULT_PLACEHOLDER);
	}

	public SecurePasswordField(@NamedArg("placeholderChar") String placeholderChar) {
		this.getStyleClass().add(STYLE_CLASS);
		this.placeholderChar = placeholderChar;
		this.setAccessibleRole(AccessibleRole.PASSWORD_FIELD);
		this.addEventHandler(DragEvent.DRAG_OVER, this::handleDragOver);
		this.addEventHandler(DragEvent.DRAG_DROPPED, this::handleDragDropped);
		this.addEventHandler(KeyEvent.ANY, this::handleKeyEvent);
		this.revealPasswordProperty().addListener(this::revealPasswordChanged);
		this.focusedProperty().addListener(this::focusedChanged);
	}

	public void cut() {
		//not implemented by design
	}

	public void copy() {
		//not implemented by design
	}

	public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
		return switch (attribute) {
			case TEXT -> null;
			default -> super.queryAccessibleAttribute(attribute, parameters);
		};
	}

	private void handleDragOver(DragEvent event) {
		Dragboard dragboard = event.getDragboard();
		if (dragboard.hasString() && dragboard.getString() != null) {
			event.acceptTransferModes(TransferMode.COPY);
		}
		event.consume();
	}

	private void handleDragDropped(DragEvent event) {
		Dragboard dragboard = event.getDragboard();
		if (dragboard.hasString() && dragboard.getString() != null) {
			insertText(getCaretPosition(), dragboard.getString());
		}
		event.consume();
	}

	private void handleKeyEvent(KeyEvent e) {
		if (e.getCode() == KeyCode.CAPS) {
			updateCapsLocked();
		} else if (SHORTCUT_BACKSPACE.match(e)) {
			wipe();
		}
	}

	private void revealPasswordChanged(@SuppressWarnings("unused") Observable observable) {
		IndexRange selection = getSelection();
		if (isRevealPassword()) {
			super.setText(this.getCharacters().toString());
		} else {
			String placeholderText = Strings.repeat(placeholderChar, length);
			super.setText(placeholderText);
		}
		selectRange(selection.getStart(), selection.getEnd());
	}

	private void focusedChanged(@SuppressWarnings("unused") Observable observable) {
		updateCapsLocked();
	}

	private void updateCapsLocked() {
		capsLocked.set(Platform.isKeyLocked(KeyCode.CAPS).orElse(false));
	}

	private void updateContainingNonPrintableChars() {
		containingNonPrintableChars.set(containsNonPrintableCharacters());
	}

	/**
	 * @return <code>true</code> if any {@link Character#isISOControl(char) control character} is present in the current value of this password field.
	 * @implNote runs in O(n)
	 */
	boolean containsNonPrintableCharacters() {
		for (int i = 0; i < length; i++) {
			if (Character.isISOControl(content[i])) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Replaces a range of characters with the given text.
	 * The text will be normalized to <a href="https://www.unicode.org/glossary/#normalization_form_c">NFC</a>.
	 *
	 * @param start The starting index in the range, inclusive. This must be &gt;= 0 and &lt; the end.
	 * @param end The ending index in the range, exclusive. This is one-past the last character to
	 * delete (consistent with the String manipulation methods). This must be &gt; the start,
	 * and &lt;= the length of the text.
	 * @param text The text that is to replace the range. This must not be null.
	 * @implNote Internally calls {@link PasswordField#replaceText(int, int, String)} with a dummy String for visual purposes.
	 */
	@Override
	public void replaceText(int start, int end, String text) {
		String normalizedText = Normalizer.normalize(text, Form.NFC);
		int removed = end - start;
		int added = normalizedText.length();
		int delta = added - removed;

		// ensure sufficient content buffer size
		int oldLength = length;
		this.length += delta;
		growContentIfNeeded();

		// shift existing content
		if (delta != 0 && start < oldLength) {
			System.arraycopy(content, end, content, end + delta, oldLength - end);
		}

		// copy new text to content buffer
		normalizedText.getChars(0, normalizedText.length(), content, start);

		// trigger visual hints
		updateContainingNonPrintableChars();
		if (isRevealPassword()) {
			super.replaceText(start, end, text);
		} else {
			String placeholderString = Strings.repeat(placeholderChar, normalizedText.length());
			super.replaceText(start, end, placeholderString);
		}
	}

	private void growContentIfNeeded() {
		if (length > content.length) {
			char[] newContent = new char[length + GROW_BUFFER_SIZE];
			System.arraycopy(content, 0, newContent, 0, content.length);
			wipe(content);
			this.content = newContent;
		}
	}

	/**
	 * Creates a CharSequence by wrapping the password characters.
	 *
	 * @return A character sequence backed by the SecurePasswordField's buffer (not a copy).
	 * @implNote The CharSequence will not copy the backing char[].
	 * Therefore any mutation to the SecurePasswordField's content will mutate or eventually swipe the returned CharSequence.
	 * @implSpec The CharSequence is usually in <a href="https://www.unicode.org/glossary/#normalization_form_c">NFC</a> representation (unless NFD-encoded char[] is set via {@link #setPassword(char[])}).
	 * @see #wipe()
	 */
	@Override
	public Passphrase getCharacters() {
		return new Passphrase(content, 0, length);
	}

	/**
	 * Convenience method wrapper for {@link #setPassword(char[])}.
	 *
	 * @param password
	 * @see #setPassword(char[])
	 */
	public void setPassword(CharSequence password) {
		char[] buf = new char[password.length()];
		for (int i = 0; i < password.length(); i++) {
			buf[i] = password.charAt(i);
		}
		setPassword(buf);
		Arrays.fill(buf, WIPE_CHAR);
	}

	/**
	 * Directly sets the content of this password field to a copy of the given password.
	 * No conversion whatsoever happens. If you want to normalize the unicode representation of the password,
	 * do it before calling this method.
	 *
	 * @param password
	 */
	public void setPassword(char[] password) {
		wipe();
		content = Arrays.copyOf(password, password.length);
		length = password.length;

		String placeholderString = Strings.repeat(placeholderChar, password.length);
		setText(placeholderString);
	}

	/**
	 * Destroys the stored password by overriding each character with a different character.
	 */
	public void wipe() {
		wipe(content);
		length = 0;
		setText(null);
	}

	private void wipe(char[] buffer) {
		Arrays.fill(buffer, WIPE_CHAR);
	}

	/* Observable Properties */

	public ReadOnlyBooleanProperty capsLockedProperty() {
		return capsLocked;
	}

	public boolean isCapsLocked() {
		return capsLocked.get();
	}

	public ReadOnlyBooleanProperty containingNonPrintableCharsProperty() {
		return containingNonPrintableChars;
	}

	public boolean isContainingNonPrintableChars() {
		return containingNonPrintableChars.get();
	}

	public BooleanProperty revealPasswordProperty() {
		return revealPassword;
	}

	public boolean isRevealPassword() {
		return revealPassword.get();
	}

	public void setRevealPassword(boolean revealPassword) {
		this.revealPassword.set(revealPassword);
	}
}

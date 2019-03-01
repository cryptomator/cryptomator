/*******************************************************************************
 * Copyright (c) 2014, 2017 Sebastian Stenzel
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui.controls;

import com.google.common.base.Strings;
import javafx.beans.NamedArg;
import javafx.beans.Observable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.awt.Toolkit;
import java.nio.CharBuffer;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Arrays;

/**
 * Patched PasswordField that doesn't create String copies of the password in memory. Instead the password is stored in a char[] that can be swiped.
 *
 * @implNote Since {@link #setText(String)} is final, we can not override its behaviour. For that reason you should not use the {@link #textProperty()} for anything else than display purposes.
 */
public class SecPasswordField extends PasswordField {

	private static final char SWIPE_CHAR = ' ';
	private static final int INITIAL_BUFFER_SIZE = 50;
	private static final int GROW_BUFFER_SIZE = 50;
	private static final String PLACEHOLDER = "*";
	private static final double PADDING = 2.0;
	private static final double INDICATOR_PADDING = 4.0;
	private static final Color INDICATOR_COLOR = new Color(0.901, 0.494, 0.133, 1.0);

	private final Tooltip tooltip = new Tooltip();
	private final Label indicator = new Label();
	private final String nonPrintableCharsWarning;
	private final String capslockWarning;

	private char[] content = new char[INITIAL_BUFFER_SIZE];
	private int length = 0;

	public SecPasswordField() {
		this("", "");
	}

	public SecPasswordField(@NamedArg("nonPrintableCharsWarning") String nonPrintableCharsWarning, @NamedArg("capslockWarning") String capslockWarning) {
		this.nonPrintableCharsWarning = nonPrintableCharsWarning;
		this.capslockWarning = capslockWarning;
		indicator.setPadding(new Insets(PADDING, INDICATOR_PADDING, PADDING, INDICATOR_PADDING));
		indicator.setAlignment(Pos.CENTER_RIGHT);
		indicator.setMouseTransparent(true);
		indicator.setTextOverrun(OverrunStyle.CLIP);
		indicator.setTextFill(INDICATOR_COLOR);
		indicator.setFont(Font.font(indicator.getFont().getFamily(), 15.0));
		this.getChildren().add(indicator);
		this.setTooltip(tooltip);
		this.addEventHandler(DragEvent.DRAG_OVER, this::handleDragOver);
		this.addEventHandler(DragEvent.DRAG_DROPPED, this::handleDragDropped);
		this.addEventHandler(KeyEvent.ANY, this::handleKeyEvent);
		this.focusedProperty().addListener(this::focusedChanged);
	}

	@Override
	protected void layoutChildren() {
		super.layoutChildren();
		indicator.relocate(0.0, 0.0);
		indicator.resize(getWidth(), getHeight());
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
			updateVisualHints(true);
		}
	}

	private void focusedChanged(@SuppressWarnings("unused") Observable observable) {
		updateVisualHints(isFocused());
	}

	private void updateVisualHints(boolean focused) {
		StringBuilder tooltipSb = new StringBuilder();
		StringBuilder indicatorSb = new StringBuilder();
		if (containsNonPrintableCharacters()) {
			indicatorSb.append('⚠');
			tooltipSb.append("- ").append(nonPrintableCharsWarning).append('\n');
		}
		// AWT code needed until https://bugs.openjdk.java.net/browse/JDK-8090882 is closed:
		if (focused && Toolkit.getDefaultToolkit().getLockingKeyState(java.awt.event.KeyEvent.VK_CAPS_LOCK)) {
			indicatorSb.append('⇪');
			tooltipSb.append("- ").append(capslockWarning).append('\n');
		}
		indicator.setText(indicatorSb.toString());
		if (!indicator.getText().isEmpty()) {
			setPadding(new Insets(PADDING, getIndicatorWidth(), PADDING, PADDING));
		} else {
			setPadding(new Insets(PADDING));
		}
		tooltip.setText(tooltipSb.toString());
		if (tooltip.getText().isEmpty()) {
			setTooltip(null);
		} else {
			setTooltip(tooltip);
		}
	}

	private double getIndicatorWidth() {
		return new Text(indicator.getText()).getLayoutBounds().getWidth() + INDICATOR_PADDING * 2.0;
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
		updateVisualHints(true);
		String placeholderString = Strings.repeat(PLACEHOLDER, normalizedText.length());
		super.replaceText(start, end, placeholderString);
	}

	private void growContentIfNeeded() {
		if (length > content.length) {
			char[] newContent = new char[length + GROW_BUFFER_SIZE];
			System.arraycopy(content, 0, newContent, 0, content.length);
			swipe(content);
			this.content = newContent;
		}
	}

	/**
	 * Creates a CharSequence by wrapping the password characters.
	 *
	 * @return A character sequence backed by the SecPasswordField's buffer (not a copy).
	 * @implNote The CharSequence will not copy the backing char[].
	 * Therefore any mutation to the SecPasswordField's content will mutate or eventually swipe the returned CharSequence.
	 * @implSpec The CharSequence is usually in <a href="https://www.unicode.org/glossary/#normalization_form_c">NFC</a> representation (unless NFD-encoded char[] is set via {@link #setPassword(char[])}).
	 * @see #swipe()
	 */
	@Override
	public CharSequence getCharacters() {
		return CharBuffer.wrap(content, 0, length);
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
		Arrays.fill(buf, SWIPE_CHAR);
	}

	/**
	 * Directly sets the content of this password field to a copy of the given password.
	 * No conversion whatsoever happens. If you want to normalize the unicode representation of the password,
	 * do it before calling this method.
	 *
	 * @param password
	 */
	public void setPassword(char[] password) {
		swipe();
		content = Arrays.copyOf(password, password.length);
		length = password.length;

		String placeholderString = Strings.repeat(PLACEHOLDER, password.length);
		setText(placeholderString);
	}

	/**
	 * Destroys the stored password by overriding each character with a different character.
	 */
	public void swipe() {
		swipe(content);
		length = 0;
	}

	private void swipe(char[] buffer) {
		Arrays.fill(buffer, SWIPE_CHAR);
	}

}

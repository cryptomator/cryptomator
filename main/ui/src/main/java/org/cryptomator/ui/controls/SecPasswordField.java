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
import javafx.scene.control.PasswordField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import java.nio.CharBuffer;
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

	private char[] content = new char[INITIAL_BUFFER_SIZE];
	private int length = 0;

	public SecPasswordField() {
		this.onDragOverProperty().set(this::handleDragOver);
		this.onDragDroppedProperty().set(this::handleDragDropped);
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

	@Override
	public void replaceText(int start, int end, String text) {
		int removed = end - start;
		int added = text.length();
		this.length += added - removed;
		growContentIfNeeded();
		text.getChars(0, text.length(), content, start);

		String placeholderString = Strings.repeat(PLACEHOLDER, text.length());
		super.replaceText(start, end, placeholderString);
	}

	private void growContentIfNeeded() {
		if (this.length > content.length) {
			char[] newContent = new char[content.length + GROW_BUFFER_SIZE];
			System.arraycopy(content, 0, newContent, 0, content.length);
			swipe();
			this.content = newContent;
		}
	}

	/**
	 * Creates a CharSequence by wrapping the password characters.
	 *
	 * @return A character sequence backed by the SecPasswordField's buffer (not a copy).
	 * @implNote The CharSequence will not copy the backing char[].
	 * Therefore any mutation to the SecPasswordField's content will mutate or eventually swipe the returned CharSequence.
	 * @see #swipe()
	 */
	@Override
	public CharSequence getCharacters() {
		return CharBuffer.wrap(content, 0, length);
	}

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
		Arrays.fill(content, SWIPE_CHAR);
	}

}

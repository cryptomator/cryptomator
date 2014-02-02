/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package de.sebastianstenzel.oce.ui.controls;

import java.nio.CharBuffer;
import java.util.Arrays;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.TextInputControl;

import com.sun.javafx.binding.ExpressionHelper;

/**
 * Don't use, won't work.
 * Just an experiment. Will be moved to a separate branch, when I have some time for cleanup stuff.
 */
@Deprecated
public class SecurePasswordField extends TextInputControl {

	public SecurePasswordField() {
		this("");
	}

	public SecurePasswordField(String text) {
		super(new SecureContent());
		getStyleClass().add("password-field");
		this.setText(text);
	}

	public void swipe() {
		final Content content = this.getContent();
		if (content instanceof SecureContent) {
			final SecureContent secureContent = (SecureContent) content;
			secureContent.swipe();
		}
	}
	
	@Override
	public void cut() {
		// No-op
	}
	
	@Override
	public void copy() {
		// No-op
	}

	/**
	 * Content based on a CharBuffer, whose backing char[] can be swiped on demand.
	 */
	private static final class SecureContent implements Content {
		private static final int INITIAL_BUFFER_LENGTH = 64;
		private static final int BUFFER_GROWTH_FACTOR = 2;

		private ExpressionHelper<String> helper = null;
		private CharBuffer buffer = CharBuffer.allocate(INITIAL_BUFFER_LENGTH);

		public void swipe() {
			assert (buffer.hasArray());
			Arrays.fill(buffer.array(), (char) 0);
			buffer.position(0);
		}

		@Override
		public String get() {
			return buffer.toString();
		}

		@Override
		public void addListener(ChangeListener<? super String> changeListener) {
			helper = ExpressionHelper.addListener(helper, this, changeListener);

		}

		@Override
		public String getValue() {
			return get();
		}

		@Override
		public void removeListener(ChangeListener<? super String> changeListener) {
			helper = ExpressionHelper.removeListener(helper, changeListener);

		}

		@Override
		public void addListener(InvalidationListener listener) {
			helper = ExpressionHelper.addListener(helper, this, listener);

		}

		@Override
		public void removeListener(InvalidationListener listener) {
			helper = ExpressionHelper.removeListener(helper, listener);

		}

		@Override
		public void delete(int start, int end, boolean notifyListeners) {
			final int delLen = end - start;
			final int pos = buffer.position();
			if (delLen <= 0 || end > pos) {
				return;
			}
			final char[] followingChars = new char[pos - end];
			try {
				// save follow-up chars:
				buffer.get(followingChars, end, buffer.position() - end);
				// close gap:
				buffer.put(followingChars, start, followingChars.length);
				// zeroing out freed space at end of buffer
				final char[] zeros = new char[delLen];
				buffer.put(zeros, pos - delLen, delLen);
				// adjust length:
				buffer.position(pos - delLen);
				if (notifyListeners) {
					ExpressionHelper.fireValueChangedEvent(helper);
				}
			} finally {
				// swipe tmp variable
				Arrays.fill(followingChars, (char) 0);
			}
		}

		@Override
		public String get(int start, int end) {
			final char[] tmp = new char[end - start];
			try {
				buffer.get(tmp, start, end - start);
				return new String(tmp);
			} finally {
				Arrays.fill(tmp, (char) 0);
			}
		}

		@Override
		public void insert(int index, String text, boolean notifyListeners) {
			if (text.isEmpty()) {
				return;
			}
			final String filteredInput;
			if (SecurePasswordField.containsIllegalChars(text)) {
				filteredInput = SecurePasswordField.removeIllegalChars(text);
			} else {
				filteredInput = text;
			}
			while (filteredInput.length() > buffer.remaining()) {
				extendBuffer();
			}
			final int pos = buffer.position();
			final char[] followingChars = new char[pos - index];
			try {
				// create empty gap for new text:
				buffer.get(followingChars, index, followingChars.length);
				// insert text at index:
				buffer.put(filteredInput, index, filteredInput.length() - index);
				// insert chars previously at this position afterwards
				final int posAfterNewText = index + filteredInput.length();
				buffer.put(followingChars, posAfterNewText, followingChars.length - posAfterNewText);
				// adjust length:
				buffer.position(pos + filteredInput.length());
				if (notifyListeners) {
					ExpressionHelper.fireValueChangedEvent(helper);
				}
			} finally {
				// swipe tmp variable
				Arrays.fill(followingChars, (char) 0);
			}
		}

		private void extendBuffer() {
			int currentCapacity = buffer.capacity();
			buffer = CharBuffer.allocate(currentCapacity * BUFFER_GROWTH_FACTOR);
		}

		@Override
		public int length() {
			return buffer.length();
		}

	}

	static boolean containsIllegalChars(String string) {
		for (char c : string.toCharArray()) {
			if (SecurePasswordField.isIllegalChar(c)) {
				return true;
			}
		}
		return false;
	}

	static String removeIllegalChars(String string) {
		final StringBuilder sb = new StringBuilder(string.length());
		for (char c : string.toCharArray()) {
			if (!SecurePasswordField.isIllegalChar(c)) {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	static boolean isIllegalChar(char c) {
		return (c == 0x7F || c == 0x0A || c == 0x09 || c < 0x20);
	}

}

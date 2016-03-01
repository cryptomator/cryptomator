/*******************************************************************************
 * Copyright (c) 2014, 2016 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui.controls;

import java.util.Arrays;

import javafx.scene.control.PasswordField;

/**
 * Compromise in security. While the text can be swiped, any access to the {@link #getText()} method will create a copy of the String in the heap.
 */
public class SecPasswordField extends PasswordField {

	private static final char SWIPE_CHAR = ' ';

	/**
	 * {@link #getContent()} uses a StringBuilder, which in turn is backed by a char[].
	 * The delete operation of AbstractStringBuilder closes the gap, that forms by deleting chars, by moving up the following chars.
	 * <br/>
	 * Imagine the following example with <code>pass</code> being the password, <code>x</code> being the swipe char and <code>'</code> being the offset of the char array:
	 * <ol>
	 * <li>Append filling chars to the end of the password: <code>passxxxx'</code></li>
	 * <li>Delete first 4 chars. Internal implementation will then copy subsequent chars to the position, where the deletion occured: <code>xxxx'xxxx</code></li>
	 * <li>Delete first 4 chars again, as we appended 4 chars in step 1: <code>'xxxxxx</code></li>
	 * </ol>
	 */
	public void swipe() {
		final int pwLength = this.getContent().length();
		final char[] fillingChars = new char[pwLength];
		Arrays.fill(fillingChars, SWIPE_CHAR);
		this.getContent().insert(pwLength, new String(fillingChars), false);
		this.getContent().delete(0, pwLength, true);
		this.getContent().delete(0, pwLength, true);
		// previous text has now been overwritten. but we still need to update the text to trigger some property bindings:
		this.clear();
	}

}

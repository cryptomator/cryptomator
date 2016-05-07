/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.charsets;

import java.text.Normalizer.Form;

import org.cryptomator.filesystem.File;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class NormalizedNameFileTest {

	private File delegateNfc;
	private File delegateNfd;

	@Before
	public void setup() {
		delegateNfc = Mockito.mock(File.class);
		delegateNfd = Mockito.mock(File.class);
		Mockito.when(delegateNfc.name()).thenReturn("\u00C5");
		Mockito.when(delegateNfd.name()).thenReturn("\u0041\u030A");
	}

	@Test
	public void testDisplayNameInNfc() {
		File file1 = new NormalizedNameFile(null, delegateNfc, Form.NFC);
		File file2 = new NormalizedNameFile(null, delegateNfd, Form.NFC);
		Assert.assertEquals("\u00C5", file1.name());
		Assert.assertEquals("\u00C5", file2.name());
	}

	@Test
	public void testDisplayNameInNfd() {
		File file1 = new NormalizedNameFile(null, delegateNfc, Form.NFD);
		File file2 = new NormalizedNameFile(null, delegateNfd, Form.NFD);
		Assert.assertEquals("\u0041\u030A", file1.name());
		Assert.assertEquals("\u0041\u030A", file2.name());
	}

}

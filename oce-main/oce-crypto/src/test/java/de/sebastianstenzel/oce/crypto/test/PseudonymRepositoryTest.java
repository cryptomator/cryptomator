/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package de.sebastianstenzel.oce.crypto.test;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import de.sebastianstenzel.oce.crypto.cache.PseudonymRepository;

public class PseudonymRepositoryTest {
	
	@Test
	public void testPseudonymRepos() {
		// register first pair:
		final List<String> clear1 = Arrays.asList("foo", "bar", "baz", "info.txt");
		final List<String> pseudo1 = Arrays.asList("frog", "bear", "bear", "iguana");
		PseudonymRepository.registerPath(clear1, pseudo1);
		
		// get pseudonymized path:
		final List<String> result1 = PseudonymRepository.pseudonymizedPathComponents(clear1);
		Assert.assertEquals(pseudo1, result1);
		
		// get cleartext path:
		final List<String> result2 = PseudonymRepository.cleartextPathComponents(pseudo1);
		Assert.assertEquals(clear1, result2);
		
		// register additional path:
		final List<String> clear2 = Arrays.asList("foo", "bar", "zab", "info.txt");
		final List<String> pseudo2 = Arrays.asList("frog", "bear", "zebra", "iguana");
		PseudonymRepository.registerPath(clear2, pseudo2);

		// get pseudonymized path:
		final List<String> result3 = PseudonymRepository.pseudonymizedPathComponents(clear2);
		Assert.assertEquals(pseudo2, result3);
		
		// get cleartext path:
		final List<String> result4 = PseudonymRepository.cleartextPathComponents(pseudo2);
		Assert.assertEquals(clear2, result4);
	}

}

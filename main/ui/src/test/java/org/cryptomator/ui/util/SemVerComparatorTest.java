/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui.util;

import java.util.Comparator;

import org.junit.Assert;
import org.junit.Test;

public class SemVerComparatorTest {

	private final Comparator<String> semVerComparator = new SemVerComparator();

	// equal versions

	@Test
	public void compareEqualVersions() {
		final int comparisonResult = semVerComparator.compare("1.23.4", "1.23.4");
		Assert.assertEquals(0, Integer.signum(comparisonResult));
	}

	// newer versions in first argument

	@Test
	public void compareHigherToLowerVersions() {
		Assert.assertEquals(1, Integer.signum(semVerComparator.compare("1.23.5", "1.23.4")));
		Assert.assertEquals(1, Integer.signum(semVerComparator.compare("1.24.4", "1.23.4")));
		Assert.assertEquals(1, Integer.signum(semVerComparator.compare("1.23.4", "1.23")));
		Assert.assertEquals(1, Integer.signum(semVerComparator.compare("1.23.4a", "1.23.4")));
	}

	// newer versions in second argument

	@Test
	public void compareLowerToHigherVersions() {
		Assert.assertEquals(-1, Integer.signum(semVerComparator.compare("1.23.4", "1.23.5")));
		Assert.assertEquals(-1, Integer.signum(semVerComparator.compare("1.23.4", "1.24.4")));
		Assert.assertEquals(-1, Integer.signum(semVerComparator.compare("1.23", "1.23.4")));
		Assert.assertEquals(-1, Integer.signum(semVerComparator.compare("1.23.4", "1.23.4a")));
	}

}

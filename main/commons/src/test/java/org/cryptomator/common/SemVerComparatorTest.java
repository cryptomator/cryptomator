/*******************************************************************************
 * Copyright (c) 2016, 2017 Sebastian Stenzel and others.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.common;

import java.util.Comparator;

import org.junit.Assert;
import org.junit.Test;

public class SemVerComparatorTest {

	private final Comparator<String> semVerComparator = new SemVerComparator();

	// equal versions

	@Test
	public void compareEqualVersions() {
		Assert.assertEquals(0, Integer.signum(semVerComparator.compare("1.23.4", "1.23.4")));
		Assert.assertEquals(0, Integer.signum(semVerComparator.compare("1.23.4-alpha", "1.23.4-alpha")));
		Assert.assertEquals(0, Integer.signum(semVerComparator.compare("1.23.4+20170101", "1.23.4+20171231")));
		Assert.assertEquals(0, Integer.signum(semVerComparator.compare("1.23.4-alpha+20170101", "1.23.4-alpha+20171231")));
	}

	// newer versions in first argument

	@Test
	public void compareHigherToLowerVersions() {
		Assert.assertEquals(1, Integer.signum(semVerComparator.compare("1.23.5", "1.23.4")));
		Assert.assertEquals(1, Integer.signum(semVerComparator.compare("1.24.4", "1.23.4")));
		Assert.assertEquals(1, Integer.signum(semVerComparator.compare("1.23.4", "1.23")));
		Assert.assertEquals(1, Integer.signum(semVerComparator.compare("1.23.4", "1.23.4-SNAPSHOT")));
		Assert.assertEquals(1, Integer.signum(semVerComparator.compare("1.23.4", "1.23.4-56.78")));
		Assert.assertEquals(1, Integer.signum(semVerComparator.compare("1.23.4-beta", "1.23.4-alpha")));
		Assert.assertEquals(1, Integer.signum(semVerComparator.compare("1.23.4-alpha.1", "1.23.4-alpha")));
		Assert.assertEquals(1, Integer.signum(semVerComparator.compare("1.23.4-56.79", "1.23.4-56.78")));
	}

	// newer versions in second argument

	@Test
	public void compareLowerToHigherVersions() {
		Assert.assertEquals(-1, Integer.signum(semVerComparator.compare("1.23.4", "1.23.5")));
		Assert.assertEquals(-1, Integer.signum(semVerComparator.compare("1.23.4", "1.24.4")));
		Assert.assertEquals(-1, Integer.signum(semVerComparator.compare("1.23", "1.23.4")));
		Assert.assertEquals(-1, Integer.signum(semVerComparator.compare("1.23.4-SNAPSHOT", "1.23.4")));
		Assert.assertEquals(-1, Integer.signum(semVerComparator.compare("1.23.4-56.78", "1.23.4")));
		Assert.assertEquals(-1, Integer.signum(semVerComparator.compare("1.23.4-alpha", "1.23.4-beta")));
		Assert.assertEquals(-1, Integer.signum(semVerComparator.compare("1.23.4-alpha", "1.23.4-alpha.1")));
		Assert.assertEquals(-1, Integer.signum(semVerComparator.compare("1.23.4-56.78", "1.23.4-56.79")));
	}

}

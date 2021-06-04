/*******************************************************************************
 * Copyright (c) 2016, 2017 Sebastian Stenzel and others.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Comparator;

public class SemVerComparatorTest {

	private final Comparator<String> semVerComparator = new SemVerComparator();

	// equal versions

	@Test
	public void compareEqualVersions() {
		Assertions.assertEquals(0, Integer.signum(semVerComparator.compare("1.23.4", "1.23.4")));
		Assertions.assertEquals(0, Integer.signum(semVerComparator.compare("1.23.4-alpha", "1.23.4-alpha")));
		Assertions.assertEquals(0, Integer.signum(semVerComparator.compare("1.23.4+20170101", "1.23.4+20171231")));
		Assertions.assertEquals(0, Integer.signum(semVerComparator.compare("1.23.4-alpha+20170101", "1.23.4-alpha+20171231")));
	}

	// newer versions in first argument

	@Test
	public void compareHigherToLowerVersions() {
		Assertions.assertEquals(1, Integer.signum(semVerComparator.compare("1.23.5", "1.23.4")));
		Assertions.assertEquals(1, Integer.signum(semVerComparator.compare("1.24.4", "1.23.4")));
		Assertions.assertEquals(1, Integer.signum(semVerComparator.compare("1.23.4", "1.23")));
		Assertions.assertEquals(1, Integer.signum(semVerComparator.compare("1.23.4", "1.23.4-SNAPSHOT")));
		Assertions.assertEquals(1, Integer.signum(semVerComparator.compare("1.23.4", "1.23.4-56.78")));
		Assertions.assertEquals(1, Integer.signum(semVerComparator.compare("1.23.4-beta", "1.23.4-alpha")));
		Assertions.assertEquals(1, Integer.signum(semVerComparator.compare("1.23.4-alpha.1", "1.23.4-alpha")));
		Assertions.assertEquals(1, Integer.signum(semVerComparator.compare("1.23.4-56.79", "1.23.4-56.78")));
	}

	// newer versions in second argument

	@Test
	public void compareLowerToHigherVersions() {
		Assertions.assertEquals(-1, Integer.signum(semVerComparator.compare("1.23.4", "1.23.5")));
		Assertions.assertEquals(-1, Integer.signum(semVerComparator.compare("1.23.4", "1.24.4")));
		Assertions.assertEquals(-1, Integer.signum(semVerComparator.compare("1.23", "1.23.4")));
		Assertions.assertEquals(-1, Integer.signum(semVerComparator.compare("1.23.4-SNAPSHOT", "1.23.4")));
		Assertions.assertEquals(-1, Integer.signum(semVerComparator.compare("1.23.4-56.78", "1.23.4")));
		Assertions.assertEquals(-1, Integer.signum(semVerComparator.compare("1.23.4-alpha", "1.23.4-beta")));
		Assertions.assertEquals(-1, Integer.signum(semVerComparator.compare("1.23.4-alpha", "1.23.4-alpha.1")));
		Assertions.assertEquals(-1, Integer.signum(semVerComparator.compare("1.23.4-56.78", "1.23.4-56.79")));
	}

}

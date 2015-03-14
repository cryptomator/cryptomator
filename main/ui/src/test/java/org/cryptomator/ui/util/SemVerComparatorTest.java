package org.cryptomator.ui.util;

import java.util.Comparator;

import javax.inject.Inject;
import javax.inject.Named;

import org.cryptomator.ui.MainModule;
import org.cryptomator.ui.test.GuiceJUnitRunner;
import org.cryptomator.ui.test.GuiceJUnitRunner.GuiceModules;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(GuiceJUnitRunner.class)
@GuiceModules(MainModule.class)
public class SemVerComparatorTest {

	@Inject
	@Named("SemVer")
	private Comparator<String> semVerComparator;

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

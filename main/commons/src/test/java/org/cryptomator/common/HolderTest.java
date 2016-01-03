package org.cryptomator.common;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class HolderTest {

	private static final Object INITIAL = new Object();
	private static final Object VALUE = new Object();

	private Holder<Object> inTest = new Holder<>(INITIAL);

	@Test
	public void testInitialValueIsInitial() {
		assertThat(inTest.get(), is(INITIAL));
	}

	@Test
	public void testSetChangesValue() {
		inTest.set(VALUE);

		assertThat(inTest.get(), is(VALUE));
	}

	@Test
	public void testAcceptChangesValue() {
		inTest.accept(VALUE);

		assertThat(inTest.get(), is(VALUE));
	}

	@Test
	public void testResetChangesValueToInitial() {
		inTest.set(VALUE);
		inTest.reset();

		assertThat(inTest.get(), is(INITIAL));
	}

}

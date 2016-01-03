package org.cryptomator.filesystem.nio;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class InstanceFactoryTest {

	@Test
	public void testInitialDefaultIsDefaultInstanceFactory() {
		InstanceFactory.DEFAULT.reset();

		assertThat(InstanceFactory.DEFAULT.get(), is(instanceOf(DefaultInstanceFactory.class)));
	}

}

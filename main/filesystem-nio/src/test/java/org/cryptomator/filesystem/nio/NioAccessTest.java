package org.cryptomator.filesystem.nio;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class NioAccessTest {

	@Test
	public void testInitialDefaultIsDefaultNioAccess() {
		NioAccess.DEFAULT.reset();

		assertThat(NioAccess.DEFAULT.get(), is(instanceOf(DefaultNioAccess.class)));
	}

}

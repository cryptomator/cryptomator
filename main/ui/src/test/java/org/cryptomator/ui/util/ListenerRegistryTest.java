/*******************************************************************************
 * Copyright (c) 2014, 2016 cryptomator.org
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Tillmann Gaida - initial implementation
 ******************************************************************************/
package org.cryptomator.ui.util;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.concurrent.ConcurrentSkipListMap;

import org.junit.Test;

public class ListenerRegistryTest {
	/**
	 * This test looks at how concurrent modifications affect the iterator of a
	 * {@link ConcurrentSkipListMap}. It shows that concurrent modifications
	 * work just fine, however the state of the iterator including the next
	 * value are advanced during retrieval of a value, so it's not possible to
	 * remove the next value.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testConcurrentSkipListMap() throws Exception {
		ConcurrentSkipListMap<Integer, Integer> map = new ConcurrentSkipListMap<>();

		map.put(1, 1);
		map.put(2, 2);
		map.put(3, 3);
		map.put(4, 4);
		map.put(5, 5);

		final Iterator<Integer> iterator = map.values().iterator();

		assertTrue(iterator.hasNext());
		assertEquals((Integer) 1, iterator.next());
		map.remove(2);
		assertTrue(iterator.hasNext());
		// iterator returns 2 anyway.
		assertEquals((Integer) 2, iterator.next());
		assertTrue(iterator.hasNext());
		map.remove(4);
		assertEquals((Integer) 3, iterator.next());
		assertTrue(iterator.hasNext());
		// this time we removed 4 before retrieving 3, so it is skipped.
		assertEquals((Integer) 5, iterator.next());
	}
}

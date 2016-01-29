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

import org.apache.commons.lang3.StringUtils;

public class SemVerComparator implements Comparator<String> {

	@Override
	public int compare(String version1, String version2) {
		final String[] vComps1 = StringUtils.split(version1, '.');
		final String[] vComps2 = StringUtils.split(version2, '.');
		final int commonCompCount = Math.min(vComps1.length, vComps2.length);

		for (int i = 0; i < commonCompCount; i++) {
			int subversionComparisionResult = 0;
			try {
				final int v1 = Integer.parseInt(vComps1[i]);
				final int v2 = Integer.parseInt(vComps2[i]);
				subversionComparisionResult = v1 - v2;
			} catch (NumberFormatException ex) {
				// ok, lets compare this fragment lexicographically
				subversionComparisionResult = vComps1[i].compareTo(vComps2[i]);
			}
			if (subversionComparisionResult != 0) {
				return subversionComparisionResult;
			}
		}

		// all in common so far? longest version string wins:
		return vComps1.length - vComps2.length;
	}

}
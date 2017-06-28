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

import org.apache.commons.lang3.StringUtils;

/**
 * Compares version strings according to <a href="http://semver.org/spec/v2.0.0.html">SemVer 2.0.0</a>.
 */
public class SemVerComparator implements Comparator<String> {

	private static final char VERSION_SEP = '.'; // http://semver.org/spec/v2.0.0.html#spec-item-2
	private static final String PRE_RELEASE_SEP = "-"; // http://semver.org/spec/v2.0.0.html#spec-item-9
	private static final String BUILD_SEP = "+"; // http://semver.org/spec/v2.0.0.html#spec-item-10

	@Override
	public int compare(String version1, String version2) {
		// "Build metadata SHOULD be ignored when determining version precedence.
		// Thus two versions that differ only in the build metadata, have the same precedence."
		String v1WithoutBuildMetadata = StringUtils.substringBefore(version1, BUILD_SEP);
		String v2WithoutBuildMetadata = StringUtils.substringBefore(version2, BUILD_SEP);

		if (v1WithoutBuildMetadata.equals(v2WithoutBuildMetadata)) {
			return 0;
		}

		String v1MajorMinorPatch = StringUtils.substringBefore(v1WithoutBuildMetadata, PRE_RELEASE_SEP);
		String v2MajorMinorPatch = StringUtils.substringBefore(v2WithoutBuildMetadata, PRE_RELEASE_SEP);
		String v1PreReleaseVersion = StringUtils.substringAfter(v1WithoutBuildMetadata, PRE_RELEASE_SEP);
		String v2PreReleaseVersion = StringUtils.substringAfter(v2WithoutBuildMetadata, PRE_RELEASE_SEP);
		return compare(v1MajorMinorPatch, v1PreReleaseVersion, v2MajorMinorPatch, v2PreReleaseVersion);
	}

	private int compare(String v1MajorMinorPatch, String v1PreReleaseVersion, String v2MajorMinorPatch, String v2PreReleaseVersion) {
		int comparisonResult = compareNumericallyThenLexicographically(v1MajorMinorPatch, v2MajorMinorPatch);
		if (comparisonResult == 0) {
			if (v1PreReleaseVersion.isEmpty()) {
				return 1; // 1.0.0 > 1.0.0-BETA
			} else if (v2PreReleaseVersion.isEmpty()) {
				return -1; // 1.0.0-BETA < 1.0.0
			} else {
				return compareNumericallyThenLexicographically(v1PreReleaseVersion, v2PreReleaseVersion);
			}
		} else {
			return comparisonResult;
		}
	}

	private int compareNumericallyThenLexicographically(String version1, String version2) {
		final String[] vComps1 = StringUtils.split(version1, VERSION_SEP);
		final String[] vComps2 = StringUtils.split(version2, VERSION_SEP);
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

		// all in common so far? longest version string is considered the higher version:
		return vComps1.length - vComps2.length;
	}

}
/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.common;

import java.util.Comparator;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class CommonsModule {

	@Provides
	@Singleton
	@Named("SemVer")
	Comparator<String> providesSemVerComparator() {
		return new SemVerComparator();
	}

}

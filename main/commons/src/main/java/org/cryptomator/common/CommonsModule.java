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

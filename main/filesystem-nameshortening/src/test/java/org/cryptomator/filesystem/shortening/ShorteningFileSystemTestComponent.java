package org.cryptomator.filesystem.shortening;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component
public interface ShorteningFileSystemTestComponent {

	ShorteningFileSystemFactory shorteningFileSystemFactory();

}

package org.cryptomator.filesystem.crypto;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = CryptoFileSystemModule.class)
interface CryptoFileSystemComponent {

	CryptoFileSystemFactory cryptoFileSystemFactory();

}

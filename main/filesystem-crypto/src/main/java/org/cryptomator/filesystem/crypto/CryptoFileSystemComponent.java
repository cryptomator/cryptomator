package org.cryptomator.filesystem.crypto;

import javax.inject.Singleton;

import org.cryptomator.crypto.engine.impl.CryptoEngineModule;

import dagger.Component;

@Singleton
@Component(modules = CryptoEngineModule.class)
interface CryptoFileSystemComponent {

	CryptoFileSystemFactory cryptoFileSystemFactory();

}

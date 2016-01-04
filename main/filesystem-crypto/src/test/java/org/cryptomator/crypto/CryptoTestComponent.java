package org.cryptomator.crypto;

import javax.inject.Singleton;

import org.cryptomator.crypto.engine.impl.CryptoTestModule;

import dagger.Component;

@Singleton
@Component(modules = CryptoTestModule.class)
interface CryptoTestComponent {

	CryptoFileSystemFactory cryptoFileSystemFactory();

}

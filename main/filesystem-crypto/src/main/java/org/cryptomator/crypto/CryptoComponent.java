package org.cryptomator.crypto;

import javax.inject.Singleton;

import org.cryptomator.crypto.engine.impl.CryptoModule;

import dagger.Component;

@Singleton
@Component(modules = CryptoModule.class)
interface CryptoComponent {

	CryptoFileSystemFactory cryptoFileSystemFactory();

}

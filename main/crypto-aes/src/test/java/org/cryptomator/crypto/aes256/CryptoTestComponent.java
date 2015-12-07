package org.cryptomator.crypto.aes256;

import javax.inject.Singleton;

import org.cryptomator.crypto.Cryptor;

import dagger.Component;

@Singleton
@Component(modules = CryptoTestModule.class)
public interface CryptoTestComponent {

	Cryptor cryptor();

}

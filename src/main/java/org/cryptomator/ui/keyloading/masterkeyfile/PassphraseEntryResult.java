package org.cryptomator.ui.keyloading.masterkeyfile;

// TODO needs to be public due to Dagger -.-
public record PassphraseEntryResult(char[] passphrase, boolean savePassphrase) {

}

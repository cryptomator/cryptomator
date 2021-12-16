package org.cryptomator.ui.keyloading.masterkeyfile;

import org.cryptomator.common.Passphrase;

// TODO needs to be public due to Dagger -.-
public record PassphraseEntryResult(Passphrase passphrase, boolean savePassphrase) {

}

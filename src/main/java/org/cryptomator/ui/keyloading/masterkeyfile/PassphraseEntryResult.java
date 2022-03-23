package org.cryptomator.ui.keyloading.masterkeyfile;

import org.cryptomator.common.Passphrase;

// TODO: change to package-private, as soon as this works for Dagger -.-
public record PassphraseEntryResult(Passphrase passphrase, boolean savePassphrase) {

}

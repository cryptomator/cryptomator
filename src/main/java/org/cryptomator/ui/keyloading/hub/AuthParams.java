package org.cryptomator.ui.keyloading.hub;

/**
 * Parameters required to decrypt the masterkey:
 * <ul>
 *     <li><code>m</code> Encrypted Masterkey (Base64-encoded)</li>
 *     <li><code>epk</code> Ephemeral Public Key (TODO: PEM-encoded?)</li>
 * </ul>
 */
record AuthParams(String m, String epk) {

}

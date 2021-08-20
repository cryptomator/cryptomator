package org.cryptomator.ui.keyloading.hub;

/**
 * ECIES parameters required to decrypt the masterkey:
 * <ul>
 *     <li><code>m</code> Encrypted Masterkey (base64url-encoded ciphertext)</li>
 *     <li><code>epk</code> Ephemeral Public Key (base64url-encoded SPKI format)</li>
 * </ul>
 * <p>
 * No separate tag required, since we use GCM for encryption.
 */
record EciesParams(String m, String epk) {

}

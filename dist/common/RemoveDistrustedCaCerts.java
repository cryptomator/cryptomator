import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.Security;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Map;

/**
 * Removes distrusted root CA certificates from a JDK {@code cacerts} trust store, matching by SHA-256 fingerprint so that vendor-specific alias names don't matter.
 * <p>
 * Run after {@code jlink} and before {@code jpackage} on the runtime image, e.g. {@code $JAVA_HOME/bin/java dist/common/RemoveDistrustedCaCerts.java runtime/lib/security/cacerts}.
 * The trust store is written back password-less, as the JDK expects for {@code cacerts}.
 */
public class RemoveDistrustedCaCerts {

	// Roots affected by the eMudhra intermediate CA key compromise (2026-10-17), see https://emudhra.we-have-a-problem.com/roots.json and https://bugzilla.mozilla.org/show_bug.cgi?id=2083449
	private static final Map<String, String> DISTRUSTED_SHA256 = Map.of( //
			"emSign ECC Root CA - G3", "86A1ECBA089C4A8D3BBE2734C612BA341D813E043CF9E8A862CD5C57A36BBE6B", //
			"emSign Root TLS CA - G3", "7DD78D5F4F13459A83DFF9ABBBA62EDBAF6F2D102BF257FD712F4D9F2746ED8D", //
			"emSign Root CS CA - G3", "F2EDAAFF3D9B701FB4D14C9F285F8C0BAD90FC3BCC1FA07D47F4718EDABA34A5", //
			"AAA Certificate Services", "D7A7A0FB5D7E2731D771E9484EBCDEF71D5F0C3E0A2948782BC83EE0EA699EF4" // Sectigo root that cross-signed the compromised intermediates
	);

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.err.println("Usage: java RemoveDistrustedCaCerts.java <path/to/cacerts>");
			System.exit(2);
		}
		var cacerts = Path.of(args[0]);
		var keyStore = KeyStore.getInstance(cacerts.toFile(), (char[]) null);
		var sha256 = MessageDigest.getInstance("SHA-256");
		var hex = HexFormat.of().withUpperCase();
		int removed = 0;
		for (var alias : Collections.list(keyStore.aliases())) {
			var cert = keyStore.getCertificate(alias);
			if (cert == null) {
				continue;
			}
			var fingerprint = hex.formatHex(sha256.digest(cert.getEncoded()));
			if (DISTRUSTED_SHA256.containsValue(fingerprint)) {
				keyStore.deleteEntry(alias);
				removed++;
				System.out.println("Removed distrusted CA certificate \"" + alias + "\" (SHA-256: " + fingerprint + ")");
			}
		}
		if (removed > 0) {
			// store without certificate encryption and MAC, i.e. password-less, otherwise the JDK can't read cacerts
			Security.setProperty("keystore.pkcs12.certProtectionAlgorithm", "NONE");
			Security.setProperty("keystore.pkcs12.macAlgorithm", "NONE");
			try (var out = Files.newOutputStream(cacerts)) {
				keyStore.store(out, null);
			}
		}
		System.out.println("Removed " + removed + " distrusted CA certificate(s) from " + cacerts + ", " + keyStore.size() + " remaining");
	}
}

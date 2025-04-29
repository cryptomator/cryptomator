package org.cryptomator.networking;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

public class CombinedKeyStoreSpi extends KeyStoreSpi {

	private final KeyStore primary;
	private final KeyStore fallback;

	public static CombinedKeyStoreSpi create(KeyStore primary, KeyStore fallback) {
		checkIfLoaded(primary);
		checkIfLoaded(fallback);
		return new CombinedKeyStoreSpi(primary, fallback);
	}

	private static void checkIfLoaded(KeyStore s) {
		try {
			s.aliases();
		} catch (KeyStoreException e) {
			throw new IllegalArgumentException("Keystore %s is not loaded.".formatted(s.getType()));
		}
	}

	private CombinedKeyStoreSpi(KeyStore primary, KeyStore fallback) {
		this.primary = primary;
		this.fallback = fallback;
	}

	@Override
	public Key engineGetKey(String alias, char[] password) throws NoSuchAlgorithmException, UnrecoverableKeyException {
		try {
			Key key = primary.getKey(alias, password);
			if (key == null) {
				key = fallback.getKey(alias, password);
			}
			return key;
		} catch (KeyStoreException e) {
			throw new IllegalStateException("At least one keystore of [%s, %s] is not initialized.".formatted(primary.getType(), fallback.getType()), e);
		}
	}

	@Override
	public Certificate[] engineGetCertificateChain(String alias) {
		try {
			Certificate[] chain = primary.getCertificateChain(alias);
			if (chain == null) {
				chain = fallback.getCertificateChain(alias);
			}
			return chain;
		} catch (KeyStoreException e) {
			throw new IllegalStateException("At least one keystore of [%s, %s] is not initialized.".formatted(primary.getType(), fallback.getType()), e);
		}
	}

	@Override
	public Certificate engineGetCertificate(String alias) {
		try {
			Certificate cert = primary.getCertificate(alias);
			if (cert == null) {
				cert = fallback.getCertificate(alias);
			}
			return cert;
		} catch (KeyStoreException e) {
			throw new IllegalStateException("At least one keystore of [%s, %s] is not initialized.".formatted(primary.getType(), fallback.getType()), e);
		}
	}

	@Override
	public Date engineGetCreationDate(String alias) {
		try {
			Date date = primary.getCreationDate(alias);
			if (date == null) {
				date = fallback.getCreationDate(alias);
			}
			return date;
		} catch (KeyStoreException e) {
			throw new IllegalStateException("At least one keystore of [%s, %s] is not initialized.".formatted(primary.getType(), fallback.getType()), e);
		}
	}

	@Override
	public void engineSetKeyEntry(String alias, Key key, char[] password, Certificate[] chain) throws KeyStoreException {
		throw new UnsupportedOperationException("Read-only KeyStore");
	}

	@Override
	public void engineSetKeyEntry(String alias, byte[] key, Certificate[] chain) throws KeyStoreException {
		throw new UnsupportedOperationException("Read-only KeyStore");
	}

	@Override
	public void engineSetCertificateEntry(String alias, Certificate cert) throws KeyStoreException {
		throw new UnsupportedOperationException("Read-only KeyStore");
	}

	@Override
	public void engineDeleteEntry(String alias) throws KeyStoreException {
		throw new UnsupportedOperationException("Read-only KeyStore");
	}

	@Override
	public Enumeration<String> engineAliases() {
		var aliases = new Vector<String>();
		try {
			var it1 = primary.aliases().asIterator();
			while (it1.hasNext()) {
				aliases.add(it1.next());
			}
			var it2 = fallback.aliases().asIterator();
			while (it2.hasNext()) {
				var alias = it2.next();
				if (!aliases.contains(alias)) {
					aliases.add(alias);
				}
			}
			return aliases.elements();
		} catch (KeyStoreException e) {
			throw new IllegalStateException("At least one keystore of [%s, %s] is not initialized.".formatted(primary.getType(), fallback.getType()), e);
		}
	}

	@Override
	public boolean engineContainsAlias(String alias) {
		try {
			return primary.containsAlias(alias) || fallback.containsAlias(alias);
		} catch (KeyStoreException e) {
			throw new IllegalStateException("At least one keystore of [%s, %s] is not initialized.".formatted(primary.getType(), fallback.getType()), e);
		}
	}

	@Override
	public int engineSize() {
		var aliases = engineAliases();
		if (!aliases.hasMoreElements()) {
			return 0;
		}

		var i = new AtomicInteger(0);
		aliases.asIterator().forEachRemaining(_ -> i.incrementAndGet());
		return i.get();
	}

	@Override
	public boolean engineIsKeyEntry(String alias) {
		return false;
	}

	@Override
	public boolean engineIsCertificateEntry(String alias) {
		return false;
	}

	@Override
	public String engineGetCertificateAlias(Certificate cert) {
		try {
			String alias = primary.getCertificateAlias(cert);
			if (alias == null) {
				alias = fallback.getCertificateAlias(cert);
			}
			return alias;
		} catch (KeyStoreException e) {
			throw new IllegalStateException("At least one keystore of [%s, %s] is not initialized.".formatted(primary.getType(), fallback.getType()), e);
		}
	}

	@Override
	public void engineStore(OutputStream stream, char[] password) throws IOException, NoSuchAlgorithmException, CertificateException {
		throw new UnsupportedOperationException("Read-only KeyStore");
	}

	@Override
	public void engineLoad(InputStream stream, char[] password) throws IOException, NoSuchAlgorithmException, CertificateException {
		// Nothing to do; the real keystores are already loaded.
	}
}

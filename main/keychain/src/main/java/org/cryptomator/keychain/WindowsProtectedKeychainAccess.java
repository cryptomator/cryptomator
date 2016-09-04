package org.cryptomator.keychain;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.jni.WinDataProtection;
import org.cryptomator.jni.WinFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

class WindowsProtectedKeychainAccess implements KeychainAccessStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(WindowsProtectedKeychainAccess.class);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting() //
			.registerTypeHierarchyAdapter(byte[].class, new ByteArrayJsonAdapter()) //
			.disableHtmlEscaping().create();

	private final WinDataProtection dataProtection;
	private final Path keychainPath;
	private Map<String, KeychainEntry> keychainEntries;

	@Inject
	public WindowsProtectedKeychainAccess(Optional<WinFunctions> winFunctions) {
		if (winFunctions.isPresent()) {
			this.dataProtection = winFunctions.get().dataProtection();
		} else {
			this.dataProtection = null;
		}
		String keychainPathProperty = System.getProperty("cryptomator.keychainPath");
		if (dataProtection != null && keychainPathProperty == null) {
			LOG.warn("Windows DataProtection module loaded, but no cryptomator.keychainPath property found.");
		}
		if (keychainPathProperty != null) {
			if (keychainPathProperty.startsWith("~/")) {
				keychainPathProperty = SystemUtils.USER_HOME + keychainPathProperty.substring(1);
			}
			this.keychainPath = FileSystems.getDefault().getPath(keychainPathProperty);
		} else {
			this.keychainPath = null;
		}
	}

	@Override
	public void storePassphrase(String key, CharSequence passphrase) {
		loadKeychainEntriesIfNeeded();
		ByteBuffer buf = UTF_8.encode(CharBuffer.wrap(passphrase));
		byte[] cleartext = new byte[buf.remaining()];
		buf.get(cleartext);
		KeychainEntry entry = new KeychainEntry();
		entry.salt = generateSalt();
		entry.ciphertext = dataProtection.protect(cleartext, entry.salt);
		Arrays.fill(buf.array(), (byte) 0x00);
		Arrays.fill(cleartext, (byte) 0x00);
		keychainEntries.put(key, entry);
		saveKeychainEntries();
	}

	@Override
	public char[] loadPassphrase(String key) {
		loadKeychainEntriesIfNeeded();
		KeychainEntry entry = keychainEntries.get(key);
		if (entry == null) {
			return null;
		}
		byte[] cleartext = dataProtection.unprotect(entry.ciphertext, entry.salt);
		if (cleartext == null) {
			return null;
		}
		CharBuffer buf = UTF_8.decode(ByteBuffer.wrap(cleartext));
		char[] passphrase = new char[buf.remaining()];
		buf.get(passphrase);
		Arrays.fill(cleartext, (byte) 0x00);
		Arrays.fill(buf.array(), (char) 0x00);
		return passphrase;
	}

	@Override
	public void deletePassphrase(String key) {
		loadKeychainEntriesIfNeeded();
		keychainEntries.remove(key);
		saveKeychainEntries();
	}

	@Override
	public boolean isSupported() {
		return SystemUtils.IS_OS_WINDOWS && dataProtection != null && keychainPath != null;
	}

	private byte[] generateSalt() {
		byte[] result = new byte[2 * Long.BYTES];
		UUID uuid = UUID.randomUUID();
		ByteBuffer buf = ByteBuffer.wrap(result);
		buf.putLong(uuid.getMostSignificantBits());
		buf.putLong(uuid.getLeastSignificantBits());
		return result;
	}

	private void loadKeychainEntriesIfNeeded() {
		if (keychainEntries == null) {
			loadKeychainEntries();
		}
		assert keychainEntries != null;
	}

	private void loadKeychainEntries() {
		Type type = new TypeToken<Map<String, KeychainEntry>>() {
		}.getType();
		try (InputStream in = Files.newInputStream(keychainPath, StandardOpenOption.READ); //
				Reader reader = new InputStreamReader(in, UTF_8)) {
			keychainEntries = GSON.fromJson(reader, type);
		} catch (JsonParseException | NoSuchFileException e) {
			LOG.info("Creating new keychain at path {}", keychainPath);
		} catch (IOException e) {
			throw new UncheckedIOException("Could not read keychain from path " + keychainPath, e);
		}
		if (keychainEntries == null) {
			keychainEntries = new HashMap<>();
		}
	}

	private void saveKeychainEntries() {
		try (OutputStream out = Files.newOutputStream(keychainPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING); //
				Writer writer = new OutputStreamWriter(out, UTF_8)) {
			GSON.toJson(keychainEntries, writer);
		} catch (IOException e) {
			throw new UncheckedIOException("Could not read keychain from path " + keychainPath, e);
		}
	}

	private static class KeychainEntry {
		@SerializedName("ciphertext")
		byte[] ciphertext;
		@SerializedName("salt")
		byte[] salt;
	}

	private static class ByteArrayJsonAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {

		private static final Base64 BASE64 = new Base64();

		@Override
		public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			return BASE64.decode(json.getAsString().getBytes(StandardCharsets.UTF_8));
		}

		@Override
		public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
			return new JsonPrimitive(new String(BASE64.encode(src), StandardCharsets.UTF_8));
		}

	}

}

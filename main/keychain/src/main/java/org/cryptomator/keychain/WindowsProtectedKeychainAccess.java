/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.keychain;

import com.google.common.io.BaseEncoding;
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
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.Environment;
import org.cryptomator.jni.WinDataProtection;
import org.cryptomator.jni.WinFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
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
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

class WindowsProtectedKeychainAccess implements KeychainAccessStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(WindowsProtectedKeychainAccess.class);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting() //
			.registerTypeHierarchyAdapter(byte[].class, new ByteArrayJsonAdapter()) //
			.disableHtmlEscaping().create();

	private final Optional<WinFunctions> winFunctions;
	private final List<Path> keychainPaths;
	private Map<String, KeychainEntry> keychainEntries;

	@Inject
	public WindowsProtectedKeychainAccess(Optional<WinFunctions> winFunctions, Environment environment) {
		this.winFunctions = winFunctions;
		this.keychainPaths = environment.getKeychainPath().collect(Collectors.toList());
	}

	private WinDataProtection dataProtection() {
		return winFunctions.orElseThrow(IllegalStateException::new).dataProtection();
	}

	@Override
	public void storePassphrase(String key, CharSequence passphrase) {
		loadKeychainEntriesIfNeeded();
		ByteBuffer buf = UTF_8.encode(CharBuffer.wrap(passphrase));
		byte[] cleartext = new byte[buf.remaining()];
		buf.get(cleartext);
		KeychainEntry entry = new KeychainEntry();
		entry.salt = generateSalt();
		entry.ciphertext = dataProtection().protect(cleartext, entry.salt);
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
		byte[] cleartext = dataProtection().unprotect(entry.ciphertext, entry.salt);
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
		return SystemUtils.IS_OS_WINDOWS && winFunctions.isPresent() && !keychainPaths.isEmpty();
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
			for (Path keychainPath : keychainPaths) {
				Optional<Map<String, KeychainEntry>> keychain = loadKeychainEntries(keychainPath);
				if (keychain.isPresent()) {
					keychainEntries = keychain.get();
					break;
				}
			}
		}
		if (keychainEntries == null) {
			LOG.info("Unable to load existing keychain file, creating new keychain.");
			keychainEntries = new HashMap<>();
		}
	}

	private Optional<Map<String, KeychainEntry>> loadKeychainEntries(Path keychainPath) {
		LOG.debug("Attempting to load keychain from {}", keychainPath);
		Type type = new TypeToken<Map<String, KeychainEntry>>() {
		}.getType();
		try (InputStream in = Files.newInputStream(keychainPath, StandardOpenOption.READ); //
			 Reader reader = new InputStreamReader(in, UTF_8)) {
			return Optional.of(GSON.fromJson(reader, type));
		} catch (NoSuchFileException | JsonParseException e) {
			return Optional.empty();
		} catch (IOException e) {
			throw new UncheckedIOException("Could not read keychain from path " + keychainPath, e);
		}
	}

	private void saveKeychainEntries() {
		if (keychainPaths.isEmpty()) {
			throw new IllegalStateException("Can't save keychain if no keychain path is specified.");
		}
		saveKeychainEntries(keychainPaths.get(0));
	}

	private void saveKeychainEntries(Path keychainPath) {
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

		private static final BaseEncoding BASE64 = BaseEncoding.base64();

		@Override
		public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			return BASE64.decode(json.getAsString());
		}

		@Override
		public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
			return new JsonPrimitive(BASE64.encode(src));
		}

	}

}

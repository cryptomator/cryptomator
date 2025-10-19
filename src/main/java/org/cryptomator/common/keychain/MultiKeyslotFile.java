package org.cryptomator.common.keychain;

import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.cryptolib.common.MasterkeyFileAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Multi-keyslot masterkey file format for TrueCrypt-style plausible deniability.
 * 
 * File Format:
 * - Magic: "CRYP" (4 bytes) - identifies multi-keyslot file
 * - Version: 1 (4 bytes)
 * - Keyslot Count: N (4 bytes)
 * - Keyslot 1 Size: X bytes (4 bytes)
 * - Keyslot 1 Data: (X bytes) - encrypted masterkey in standard format
 * - Keyslot 2 Size: Y bytes (4 bytes)
 * - Keyslot 2 Data: (Y bytes) - encrypted masterkey in standard format
 * - ... (additional keyslots)
 * 
 * Each keyslot contains a complete masterkey.cryptomator file format.
 * Different passwords decrypt different keyslots, revealing different masterkeys.
 * 
 * Security Properties:
 * - Cannot determine number of keyslots without trying passwords
 * - Cannot distinguish which keyslot was used
 * - All unused keyslots look like random data
 * - Plausible deniability: "I only know one password"
 */
public class MultiKeyslotFile {
	
	private static final Logger LOG = LoggerFactory.getLogger(MultiKeyslotFile.class);
	
	private static final byte[] MAGIC = "CRYP".getBytes();
	private static final int VERSION = 1;
	private static final int HEADER_SIZE = 12; // magic(4) + version(4) + count(4)
	
	private final MasterkeyFileAccess masterkeyFileAccess;
	
	public MultiKeyslotFile(MasterkeyFileAccess masterkeyFileAccess) {
		this.masterkeyFileAccess = masterkeyFileAccess;
	}
	
	/**
	 * Check if a file is a multi-keyslot file.
	 */
	public boolean isMultiKeyslotFile(Path path) throws IOException {
		if (!Files.exists(path) || Files.size(path) < HEADER_SIZE) {
			return false;
		}
		
		byte[] header = new byte[4];
		try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
			ByteBuffer buffer = ByteBuffer.wrap(header);
			channel.read(buffer);
		}
		
		return java.util.Arrays.equals(header, MAGIC);
	}
	
	/**
	 * Try to load masterkey from any keyslot that matches the password.
	 * 
	 * @param path Path to multi-keyslot file
	 * @param password Password to try
	 * @return Masterkey if password matches any keyslot
	 * @throws InvalidPassphraseException if password doesn't match any keyslot
	 * @throws IOException on I/O errors
	 */
	public Masterkey load(Path path, CharSequence password) throws InvalidPassphraseException, IOException {
		if (!isMultiKeyslotFile(path)) {
			// Fall back to standard single-keyslot file
			return masterkeyFileAccess.load(path, password);
		}
		
		List<byte[]> keyslots = readKeyslots(path);
		LOG.debug("Found {} keyslot(s) in {}", keyslots.size(), path.getFileName());
		
		// Try each keyslot with the provided password
		for (int i = 0; i < keyslots.size(); i++) {
			try {
				// Write keyslot to temp file for decryption
				Path tempFile = Files.createTempFile("keyslot-", ".tmp");
				try {
					Files.write(tempFile, keyslots.get(i));
					Masterkey masterkey = masterkeyFileAccess.load(tempFile, password);
					LOG.info("Password matched keyslot {} of {}", i + 1, keyslots.size());
					return masterkey;
				} finally {
					Files.deleteIfExists(tempFile);
				}
			} catch (InvalidPassphraseException e) {
				// Password doesn't match this keyslot, try next
				LOG.trace("Password didn't match keyslot {}", i + 1);
				continue;
			}
		}
		
		// Password didn't match any keyslot
		throw new InvalidPassphraseException();
	}
	
	/**
	 * Create a new multi-keyslot file with a single keyslot.
	 * 
	 * @param path Path where file will be created
	 * @param masterkey Masterkey to encrypt
	 * @param password Password to encrypt with
	 * @param scryptCostParam scrypt cost parameter
	 * @throws IOException on I/O errors
	 */
	public void persist(Path path, Masterkey masterkey, CharSequence password, int scryptCostParam) throws IOException {
		// Create temp file with encrypted masterkey
		Path tempKeyslot = Files.createTempFile("keyslot-", ".tmp");
		try {
			masterkeyFileAccess.persist(masterkey, tempKeyslot, password, scryptCostParam);
			byte[] keyslotData = Files.readAllBytes(tempKeyslot);
			
			// Create multi-keyslot file with single keyslot
			List<byte[]> keyslots = List.of(keyslotData);
			writeKeyslots(path, keyslots);
			
			LOG.info("Created multi-keyslot file with 1 keyslot at {}", path.getFileName());
		} finally {
			Files.deleteIfExists(tempKeyslot);
		}
	}
	
	/**
	 * Add a hidden keyslot to an existing file.
	 * 
	 * @param path Path to existing multi-keyslot file
	 * @param masterkey Hidden masterkey to add
	 * @param password Password for hidden keyslot
	 * @param scryptCostParam scrypt cost parameter
	 * @throws IOException on I/O errors
	 */
	public void addKeyslot(Path path, Masterkey masterkey, CharSequence password, int scryptCostParam) throws IOException {
		List<byte[]> existingKeyslots;
		
		if (isMultiKeyslotFile(path)) {
			existingKeyslots = readKeyslots(path);
		} else {
			// Convert legacy single-keyslot file to multi-keyslot format
			existingKeyslots = new ArrayList<>();
			existingKeyslots.add(Files.readAllBytes(path));
			LOG.info("Converting legacy masterkey file to multi-keyslot format");
		}
		
		// Create new keyslot
		Path tempKeyslot = Files.createTempFile("keyslot-", ".tmp");
		try {
			masterkeyFileAccess.persist(masterkey, tempKeyslot, password, scryptCostParam);
			byte[] newKeyslotData = Files.readAllBytes(tempKeyslot);
			
			// Add to existing keyslots
			List<byte[]> allKeyslots = new ArrayList<>(existingKeyslots);
			allKeyslots.add(newKeyslotData);
			
			// Write back atomically
			Path tempFile = Files.createTempFile(path.getParent(), ".keyslot-", ".tmp");
			try {
				writeKeyslots(tempFile, allKeyslots);
				Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
				LOG.info("Added hidden keyslot to {} (now {} keyslots)", path.getFileName(), allKeyslots.size());
			} finally {
				Files.deleteIfExists(tempFile);
			}
		} finally {
			Files.deleteIfExists(tempKeyslot);
		}
	}
	
	/**
	 * Remove a keyslot from the file.
	 * WARNING: This requires knowing the password of the keyslot to remove.
	 * 
	 * @param path Path to multi-keyslot file
	 * @param password Password of keyslot to remove
	 * @return true if a keyslot was removed
	 * @throws IOException on I/O errors
	 */
	public boolean removeKeyslot(Path path, CharSequence password) throws IOException {
		if (!isMultiKeyslotFile(path)) {
			LOG.warn("Cannot remove keyslot from non-multi-keyslot file");
			return false;
		}
		
		List<byte[]> keyslots = readKeyslots(path);
		if (keyslots.size() <= 1) {
			LOG.warn("Cannot remove last keyslot");
			return false;
		}
		
		// Find which keyslot matches the password
		int keyslotToRemove = -1;
		for (int i = 0; i < keyslots.size(); i++) {
			Path tempFile = Files.createTempFile("keyslot-", ".tmp");
			try {
				Files.write(tempFile, keyslots.get(i));
				masterkeyFileAccess.load(tempFile, password).destroy();
				keyslotToRemove = i;
				break;
			} catch (InvalidPassphraseException e) {
				// Not this keyslot
			} finally {
				Files.deleteIfExists(tempFile);
			}
		}
		
		if (keyslotToRemove == -1) {
			LOG.warn("Password doesn't match any keyslot");
			return false;
		}
		
		// Remove the keyslot
		List<byte[]> newKeyslots = new ArrayList<>(keyslots);
		newKeyslots.remove(keyslotToRemove);
		
		if (newKeyslots.size() == 1) {
			// Convert back to single-keyslot format
			Files.write(path, newKeyslots.get(0), StandardOpenOption.TRUNCATE_EXISTING);
			LOG.info("Removed keyslot, converted back to single-keyslot format");
		} else {
			writeKeyslots(path, newKeyslots);
			LOG.info("Removed keyslot {} (now {} keyslots remaining)", keyslotToRemove + 1, newKeyslots.size());
		}
		
		return true;
	}
	
	/**
	 * Get number of keyslots in file.
	 */
	public int getKeyslotCount(Path path) throws IOException {
		if (!isMultiKeyslotFile(path)) {
			return 1; // Legacy single-keyslot file
		}
		return readKeyslots(path).size();
	}
	
	/**
	 * Read all keyslots from file.
	 */
	private List<byte[]> readKeyslots(Path path) throws IOException {
		List<byte[]> keyslots = new ArrayList<>();
		
		try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
			// Read header
			ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
			channel.read(header);
			header.flip();
			
			byte[] magic = new byte[4];
			header.get(magic);
			int version = header.getInt();
			int count = header.getInt();
			
			if (!java.util.Arrays.equals(magic, MAGIC)) {
				throw new IOException("Invalid multi-keyslot file: bad magic");
			}
			if (version != VERSION) {
				throw new IOException("Unsupported multi-keyslot file version: " + version);
			}
			
			// Read each keyslot
			for (int i = 0; i < count; i++) {
				ByteBuffer sizeBuffer = ByteBuffer.allocate(4);
				channel.read(sizeBuffer);
				sizeBuffer.flip();
				int keyslotSize = sizeBuffer.getInt();
				
				ByteBuffer keyslotBuffer = ByteBuffer.allocate(keyslotSize);
				channel.read(keyslotBuffer);
				keyslotBuffer.flip();
				
				byte[] keyslotData = new byte[keyslotSize];
				keyslotBuffer.get(keyslotData);
				keyslots.add(keyslotData);
			}
		}
		
		return keyslots;
	}
	
	/**
	 * Write keyslots to file.
	 */
	private void writeKeyslots(Path path, List<byte[]> keyslots) throws IOException {
		try (FileChannel channel = FileChannel.open(path, 
				StandardOpenOption.CREATE, 
				StandardOpenOption.WRITE, 
				StandardOpenOption.TRUNCATE_EXISTING)) {
			
			// Write header
			ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
			header.put(MAGIC);
			header.putInt(VERSION);
			header.putInt(keyslots.size());
			header.flip();
			channel.write(header);
			
			// Write each keyslot
			for (byte[] keyslot : keyslots) {
				ByteBuffer sizeBuffer = ByteBuffer.allocate(4);
				sizeBuffer.putInt(keyslot.length);
				sizeBuffer.flip();
				channel.write(sizeBuffer);
				
				ByteBuffer keyslotBuffer = ByteBuffer.wrap(keyslot);
				channel.write(keyslotBuffer);
			}
		}
	}
}

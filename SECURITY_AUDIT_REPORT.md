# Security Audit Report - Plausible Deniability Feature

## Executive Summary

This report documents a comprehensive security review of the plausible-deniability feature implementation in Cryptomator. The review focuses on cryptographic correctness, information leakage, robustness, and implementation hygiene.

**Status**: ⚠️ **MULTIPLE CRITICAL ISSUES FOUND**

---

## 1. Security / Plausible-Deniability Correctness (CRITICAL)

### ❌ 1.1 Vault/Config Format Leaks Existence/Count of Identities

**Files**: `MultiKeyslotVaultConfig.java`, `VaultConfigCache.java`, `Vault.java`

**Issue**: The multi-keyslot vault config format exposes:
- Magic bytes `"VCFG"` (line 74 in MultiKeyslotVaultConfig.java)
- Explicit config count field (line 34, 296)
- These allow an adversary to determine the number of hidden vaults without passwords

**Evidence**:
```java:src/main/java/org/cryptomator/common/vaults/MultiKeyslotVaultConfig.java
private static final byte[] MAGIC = "VCFG".getBytes(StandardCharsets.US_ASCII);  // Line 74
// ...
int count = header.getInt();  // Line 296 - exposed count
LOG.debug("Found {} config slot(s) in {}", configTokens.size(), path.getFileName());  // Line 123
```

**Impact**: Defeats plausible deniability - file format reveals hidden vault existence

**Recommendation**: 
1. Remove magic bytes and count field
2. Use fixed-size file format with padding (like MultiKeyslotFile already does)
3. Make config slots indistinguishable from random data

---

### ❌ 1.2 Hidden-Identity Creation Doesn't Verify Primary Password

**Files**: `IdentityInitializer.java`, `HiddenVaultCreationDialog.java`, `IdentityManagementController.java`

**Issue**: The `addSecondaryIdentity` method accepts `primaryPassword` parameter but never verifies it before adding the hidden keyslot. An attacker could add a keyslot without knowing the primary password.

**Evidence**:
```java:src/main/java/org/cryptomator/common/recovery/IdentityInitializer.java
public static VaultIdentity addSecondaryIdentity(Path vaultPath, String identityName, 
                                                 String identityDescription, CharSequence primaryPassword,
                                                 CharSequence hiddenPassword, 
                                                 MultiKeyslotFile multiKeyslotFile,
                                                 MultiKeyslotVaultConfig multiKeyslotVaultConfig) throws IOException {
    // NO PRIMARY PASSWORD VERIFICATION BEFORE LINE 83!
    multiKeyslotFile.addKeyslot(masterkeyPath, hiddenMasterkey, hiddenPassword, primaryPassword, 9);  // Line 83
```

**Impact**: Unauthorized keyslot addition

**Recommendation**: Before line 70, add:
```java
// Verify primary password by attempting to load existing masterkey
try (Masterkey verifyKey = multiKeyslotFile.load(masterkeyPath, primaryPassword)) {
    // Password verified, proceed
} catch (InvalidPassphraseException e) {
    throw new IOException("Primary password verification failed", e);
}
```

---

### ❌ 1.3 Hidden-Identity Deletion Is Incomplete

**Files**: `VaultIdentityManager.java`, `IdentityInitializer.java`

**Issue**: Identity removal only removes the masterkey keyslot but does NOT remove the matching config slot from `vault.cryptomator`. This leaves orphaned config data that could reveal the hidden vault's existence.

**Evidence**:
```java:src/main/java/org/cryptomator/common/vaults/VaultIdentityManager.java
public boolean removeKeyslot(CharSequence password) {
    try {
        Path masterkeyPath = vaultPath.resolve(MASTERKEY_FILENAME);
        Path hiddenVaultConfig = vaultPath.resolve("vault.bak");  // Line 113 - WRONG! Should be vault.cryptomator
        
        boolean keyslotRemoved = multiKeyslotFile.removeKeyslot(masterkeyPath, password);
        boolean configDeleted = Files.deleteIfExists(hiddenVaultConfig);  // Line 116 - Deletes wrong file!
        // MISSING: Remove config slot from vault.cryptomator using MultiKeyslotVaultConfig
```

**Impact**: Orphaned config slots reveal hidden vault existence after "deletion"

**Recommendation**: 
```java
// 1. Derive hidden masterkey from password
Masterkey hiddenMasterkey = multiKeyslotFile.load(masterkeyPath, password);
byte[] masterkeyBytes = hiddenMasterkey.getEncoded();

// 2. Remove config slot that matches this masterkey
Path vaultConfigPath = vaultPath.resolve("vault.cryptomator");
MultiKeyslotVaultConfig configHandler = new MultiKeyslotVaultConfig();
configHandler.removeConfigSlot(vaultConfigPath, masterkeyBytes);

// 3. Remove keyslot
boolean keyslotRemoved = multiKeyslotFile.removeKeyslot(masterkeyPath, password);

// 4. Destroy masterkey
hiddenMasterkey.destroy();
Arrays.fill(masterkeyBytes, (byte) 0);
```

---

### ⚠️ 1.4 Logging Reveals Multi-Keyslot Status

**Files**: `MultiKeyslotVaultConfig.java`, `VaultIdentityManager.java`, `Vault.java`

**Issue**: Multiple log statements reveal:
- Number of config slots
- Which slot was matched
- Multi-keyslot vault detection

**Evidence**:
```java:src/main/java/org/cryptomator/common/vaults/MultiKeyslotVaultConfig.java
LOG.debug("Found {} config slot(s) in {}", configTokens.size(), path.getFileName());  // Line 123
LOG.info("Masterkey matched config slot {} of {}", i + 1, configTokens.size());  // Line 135
LOG.trace("Masterkey didn't match config slot {}", i + 1);  // Line 140
LOG.info("Added config slot to {} (now {} slots)", path.getFileName(), allConfigs.size());  // Line 196
LOG.info("Removed config slot {} (now {} slots remaining)", configToRemove + 1, newConfigs.size());  // Line 253
```

**Impact**: Log files can reveal hidden vault existence to forensic analysis

**Recommendation**: Remove slot counts and indices from all logs. Keep only generic messages like "Vault unlocked successfully" without revealing which slot.

---

## 2. I/O Robustness, Atomicity, and Parsing Bounds (CRITICAL/MAJOR)

### ❌ 2.1 Unsafe Parsing Without Bounds Checks

**Files**: `VaultConfigCache.java`, `Vault.java`, `MultiKeyslotVaultConfig.java`

**Issue**: Multiple parsing functions lack proper bounds checking before array access:

**Evidence**:
```java:src/main/java/org/cryptomator/common/vaults/VaultConfigCache.java
// Line 108-111 - No check if offset+configSize exceeds fileData.length
int configSize = ((fileData[offset] & 0xFF) << 24) |
                 ((fileData[offset + 1] & 0xFF) << 16) |
                 ((fileData[offset + 2] & 0xFF) << 8) |
                 (fileData[offset + 3] & 0xFF);
offset += 4;

// Line 115 - Can cause ArrayIndexOutOfBoundsException if configSize is malicious
String token = new String(fileData, offset, configSize, StandardCharsets.US_ASCII);
```

```java:src/main/java/org/cryptomator/common/vaults/Vault.java
// Line 242-257 - No bounds checking on count or configSize
int count = ((fileData[8] & 0xFF) << 24) | ...;  // No max limit!

for (int i = 0; i < count; i++) {  // Could be Integer.MAX_VALUE!
    int configSize = ((fileData[offset] & 0xFF) << 24) | ...;  // Could be huge!
    offset += 4;
    String token = new String(fileData, offset, configSize, ...);  // OOM/overflow risk
```

**Impact**: 
- ArrayIndexOutOfBoundsException on malformed files
- Potential OOM if malicious count/size values
- BufferUnderflow errors

**Recommendation**: Add bounds checks:
```java
// Before reading count
if (fileData.length < 16) {
    throw new IOException("File too small");
}

int count = readInt(fileData, 8);
if (count < 0 || count > 256) {  // Reasonable upper bound
    throw new IOException("Invalid config count: " + count);
}

// Before reading each config
if (offset + 4 > fileData.length) {
    throw new IOException("Truncated config size field");
}
int configSize = readInt(fileData, offset);
if (configSize < 0 || configSize > 1_000_000) {  // 1 MB max per config
    throw new IOException("Invalid config size: " + configSize);
}
if (offset + 4 + configSize > fileData.length) {
    throw new IOException("Truncated config data");
}
```

---

### ✅ 2.2 Atomic Move Fallback (ADDRESSED)

**Files**: `MultiKeyslotFile.java`, `MultiKeyslotVaultConfig.java`

**Status**: ✅ Both files correctly implement `AtomicMoveNotSupportedException` fallback.

**Evidence**:
```java:src/main/java/org/cryptomator/common/keychain/MultiKeyslotFile.java
try {
    Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
} catch (java.nio.file.AtomicMoveNotSupportedException e) {
    Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
}
```

---

### ⚠️ 2.3 Duplicate Parsing Logic

**Files**: `VaultConfigCache.java`, `Vault.java`

**Issue**: Both files duplicate the multi-keyslot parsing logic instead of using `MultiKeyslotVaultConfig` helper.

**Impact**: Code duplication increases bug surface and maintenance burden

**Recommendation**: Refactor to use centralized parsing:
```java
// Add to MultiKeyslotVaultConfig.java
public VaultConfig.UnverifiedVaultConfig loadFirstSlotUnverified(Path path) throws IOException {
    List<String> tokens = readConfigSlots(path);
    if (tokens.isEmpty()) {
        throw new IOException("No config slots found");
    }
    return VaultConfig.decode(tokens.get(0));
}

// Use in VaultConfigCache.java (replace lines 92-119)
return multiKeyslotVaultConfig.loadFirstSlotUnverified(configPath);

// Use in Vault.java (replace readConfigSlotsFromMultiKeyslot method)
return multiKeyslotVaultConfig.readConfigSlots(configPath);
```

---

## 3. Masterkey and Crypto Material Lifecycle (CRITICAL)

### ❌ 3.1 Masterkey Bytes Not Zeroed in prepareVaultConfigForUnlock

**File**: `Vault.java`

**Issue**: `masterkeyBytes` array obtained from `keyLoader.loadKey()` is never zeroed after use.

**Evidence**:
```java:src/main/java/org/cryptomator/common/vaults/Vault.java
// Line 194
byte[] masterkeyBytes = keyLoader.loadKey(URI.create("masterkeyfile:masterkey.cryptomator")).getEncoded();

// Lines 194-226: masterkeyBytes used for verification
// NO CLEANUP! masterkeyBytes remains in memory
```

**Impact**: Sensitive masterkey material remains in heap memory, vulnerable to memory dumps

**Recommendation**:
```java
private String prepareVaultConfigForUnlock(MasterkeyLoader keyLoader, String configFilename) throws IOException {
    // ... existing code ...
    
    byte[] masterkeyBytes = null;
    try {
        masterkeyBytes = keyLoader.loadKey(URI.create("masterkeyfile:masterkey.cryptomator")).getEncoded();
        
        // ... use masterkeyBytes ...
        
        return ".vault.cryptomator.unlock";
    } catch (Exception e) {
        // Cleanup temp file on failure
        try {
            Files.deleteIfExists(getPath().resolve(".vault.cryptomator.unlock"));
        } catch (IOException ignored) {}
        throw new IOException("Failed to extract config from multi-keyslot file", e);
    } finally {
        // Always zero sensitive data
        if (masterkeyBytes != null) {
            Arrays.fill(masterkeyBytes, (byte) 0);
        }
    }
}
```

---

### ⚠️ 3.2 Temp Config File Not Cleaned Up On Unlock Failure

**File**: `Vault.java`

**Issue**: If `prepareVaultConfigForUnlock` throws an exception, the temporary `.vault.cryptomator.unlock` file is not cleaned up.

**Evidence**: No try-catch-finally around line 160 that calls `prepareVaultConfigForUnlock`, and no cleanup in failure path before line 318.

**Recommendation**: Wrap unlock in try-catch that calls `cleanupTempVaultConfig()` on failure:
```java
public synchronized void unlock(MasterkeyLoader keyLoader, VaultIdentity identity) throws ... {
    if (cryptoFileSystem.get() != null) {
        throw new IllegalStateException("Already unlocked.");
    }
    
    CryptoFileSystem fs = null;
    try {
        fs = createCryptoFileSystem(keyLoader, identity);
        // ... rest of unlock logic ...
    } catch (Exception e) {
        cleanupTempVaultConfig();  // Clean up on failure
        throw e;
    } finally {
        if (!success) {
            currentIdentity.set(null);
            destroyCryptoFileSystem();
        }
    }
}
```

---

## 4. UI, UX and i18n (MAJOR/MINOR)

### ❌ 4.1 Cleartext Password Prompts (TextInputDialog)

**File**: `IdentityManagementController.java`

**Issue**: All password prompts use `TextInputDialog` which displays passwords in cleartext instead of masked `PasswordField`.

**Evidence**:
```java:src/main/java/org/cryptomator/ui/vaultoptions/IdentityManagementController.java
TextInputDialog primaryPasswordDialog = new TextInputDialog();  // Line 128
primaryPasswordDialog.setContentText("Primary password:");

TextInputDialog newPasswordDialog = new TextInputDialog();  // Line 140
newPasswordDialog.setContentText("New identity password:");

TextInputDialog passwordDialog = new TextInputDialog();  // Line 178
passwordDialog.setContentText("Password:");
```

**Impact**: Passwords visible in screenshots, screen recordings, assistive technology

**Recommendation**: Replace all with custom dialogs using `PasswordField`:
```java
private Optional<String> promptPassword(String title, String header, String prompt) {
    Dialog<String> dialog = new Dialog<>();
    dialog.setTitle(title);
    dialog.setHeaderText(header);
    
    GridPane grid = new GridPane();
    PasswordField passwordField = new PasswordField();
    passwordField.setPromptText(prompt);
    grid.add(new Label(prompt), 0, 0);
    grid.add(passwordField, 1, 0);
    
    dialog.getDialogPane().setContent(grid);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    
    dialog.setResultConverter(button -> 
        button == ButtonType.OK ? passwordField.getText() : null
    );
    
    return dialog.showAndWait();
}
```

**Note**: `HiddenVaultCreationDialog.java` correctly uses `PasswordField` (line 68-72) ✅

---

### ⚠️ 4.2 Missing i18n for Identity UI

**Files**: `IdentityManagementController.java`, `HiddenVaultCreationDialog.java`

**Issue**: All user-visible strings are hardcoded English instead of using ResourceBundle.

**Evidence**:
```java
nameDialog.setTitle("Add Identity");  // Should be resourceBundle.getString("identity.add.title")
confirm.setHeaderText("Are you sure you want to remove this identity?");
```

**Recommendation**: Externalize all strings:
```properties
# In resources bundle
identity.add.title=Add Identity
identity.add.header=Create a new vault identity
identity.add.namePrompt=Identity name:
identity.remove.title=Remove Identity
identity.remove.confirm=Are you sure you want to remove this identity?
# etc.
```

---

## 5. API / Behavior Correctness (MAJOR/MINOR)

### ⚠️ 5.1 Unused Dependency Injection

**File**: `GeneralVaultOptionsController.java`

**Issue**: `MasterkeyFileAccess` is injected (line 33, 49) but never used.

**Evidence**:
```java:src/main/java/org/cryptomator/ui/vaultoptions/GeneralVaultOptionsController.java
private final MasterkeyFileAccess masterkeyFileAccess;  // Line 33
// ... injected at line 49
// NEVER USED in the entire class
```

**Recommendation**: Remove the unused field and constructor parameter.

---

### ⚠️ 5.2 Legacy vault.bak Reference

**File**: `VaultIdentityManager.java`

**Issue**: Code still references obsolete `vault.bak` file which shouldn't exist in the new design.

**Evidence**:
```java:src/main/java/org/cryptomator/common/vaults/VaultIdentityManager.java
Path hiddenVaultConfig = vaultPath.resolve("vault.bak");  // Line 113 - OBSOLETE!
```

**Recommendation**: Remove this line entirely. The new design doesn't use `vault.bak`.

---

### ⚠️ 5.3 Null Safety for Vault Path

**Files**: Multiple vault-related classes

**Issue**: No null-check on `vaultSettings.path.get()` before use.

**Evidence**: In `VaultConfigCache.java` line 41, 76 - no null check before `settings.path.get()`.

**Recommendation**:
```java
Path vaultPath = settings.path.get();
if (vaultPath == null) {
    throw new IllegalStateException("Vault path is not set");
}
```

---

## 6. Implementation Hygiene (MINOR)

### ⚠️ 6.1 Revealing Temp Directory Name

**File**: `IdentityInitializer.java`

**Issue**: Temp directory uses revealing name "hidden-vault-init".

**Evidence**:
```java:src/main/java/org/cryptomator/common/recovery/IdentityInitializer.java
Path tempVaultDir = Files.createTempDirectory("hidden-vault-init");  // Line 86
```

**Impact**: Forensic analysis of temp directories could reveal hidden vault operations

**Recommendation**:
```java
Path tempVaultDir = Files.createTempDirectory("vlt-");  // Neutral prefix
```

---

## 7. Summary of Critical Issues

| Priority | Issue | File(s) | Status |
|----------|-------|---------|--------|
| 🔴 CRITICAL | Magic bytes & count expose hidden vaults | MultiKeyslotVaultConfig.java | ❌ Not Fixed |
| 🔴 CRITICAL | No primary password verification | IdentityInitializer.java | ❌ Not Fixed |
| 🔴 CRITICAL | Incomplete identity deletion | VaultIdentityManager.java | ❌ Not Fixed |
| 🔴 CRITICAL | Masterkey bytes not zeroed | Vault.java | ❌ Not Fixed |
| 🔴 CRITICAL | Unsafe parsing without bounds | VaultConfigCache.java, Vault.java | ❌ Not Fixed |
| 🟡 MAJOR | Logging reveals keyslot info | Multiple | ⚠️ Partially Fixed |
| 🟡 MAJOR | Cleartext password prompts | IdentityManagementController.java | ❌ Not Fixed |
| 🟢 MINOR | Revealing temp directory name | IdentityInitializer.java | ❌ Not Fixed |

---

## 8. Recommendations Priority

### Immediate (Before Merge)
1. Fix primary password verification (1.2)
2. Fix identity deletion to remove config slot (1.3)
3. Zero masterkey bytes after use (3.1)
4. Add bounds checks to parsing (2.1)

### High Priority (Before Release)
1. Redesign vault config format to remove magic/count (1.1)
2. Replace TextInputDialog with PasswordField (4.1)
3. Remove revealing log statements (1.4)
4. Clean up temp files on failure (3.2)

### Medium Priority
1. Centralize parsing logic (2.3)
2. Add i18n for all UI strings (4.2)
3. Remove unused dependencies (5.1)
4. Fix vault.bak reference (5.2)

### Low Priority
1. Use neutral temp directory names (6.1)
2. Add null-safety checks (5.3)

---

## 9. Conclusion

The plausible-deniability feature has **significant security vulnerabilities** that must be addressed before release. The most critical issues are:

1. **Format leaks information** - defeats the entire purpose
2. **Authentication bypasses** - allows unauthorized operations
3. **Incomplete cleanup** - leaves forensic traces
4. **Memory safety** - sensitive data exposure

**Recommendation**: **DO NOT MERGE** until critical issues (marked 🔴) are resolved.

---

*Report generated: 2025-10-19*
*Reviewer: Security Audit*

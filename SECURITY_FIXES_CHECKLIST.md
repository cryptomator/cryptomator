# Security Fixes Checklist - Plausible Deniability

## Critical Issues (Must Fix Before Merge)

### 🔴 Issue 1: Vault Config Format Leaks Information
- [ ] Remove magic bytes `"VCFG"` from MultiKeyslotVaultConfig.java
- [ ] Remove explicit count field from format
- [ ] Redesign to use fixed-size format with padding
- [ ] Make all slots indistinguishable from random data (like MultiKeyslotFile)
- [ ] Update `isMultiKeyslotFile()` to detect format without magic bytes
- [ ] Add migration path from current format to new format

**Files to modify:**
- `src/main/java/org/cryptomator/common/vaults/MultiKeyslotVaultConfig.java`

---

### 🔴 Issue 2: Hidden-Identity Creation Missing Password Verification
- [ ] Add primary password verification in `IdentityInitializer.addSecondaryIdentity()`
- [ ] Add primary password verification in `HiddenVaultCreationDialog.createHiddenVault()`
- [ ] Add primary password verification in `IdentityManagementController.onAddIdentity()`
- [ ] Test that creation fails with wrong primary password
- [ ] Test that creation succeeds with correct primary password

**Code to add before line 70 in IdentityInitializer.java:**
```java
// Verify primary password by attempting to load existing masterkey
try (Masterkey verifyKey = multiKeyslotFile.load(masterkeyPath, primaryPassword)) {
    // Password verified, proceed
} catch (InvalidPassphraseException e) {
    throw new IOException("Primary password verification failed - access denied", e);
}
```

**Files to modify:**
- `src/main/java/org/cryptomator/common/recovery/IdentityInitializer.java`
- `src/main/java/org/cryptomator/ui/vaultoptions/HiddenVaultCreationDialog.java`
- `src/main/java/org/cryptomator/ui/vaultoptions/IdentityManagementController.java`

---

### 🔴 Issue 3: Incomplete Hidden-Identity Deletion
- [ ] Modify `VaultIdentityManager.removeKeyslot()` to derive masterkey
- [ ] Call `MultiKeyslotVaultConfig.removeConfigSlot()` with derived masterkey
- [ ] Remove obsolete `vault.bak` reference (line 113)
- [ ] Ensure masterkey is destroyed after use
- [ ] Test that both keyslot AND config slot are removed

**Replacement code for VaultIdentityManager.removeKeyslot():**
```java
public boolean removeKeyslot(CharSequence password) {
    byte[] masterkeyBytes = null;
    Masterkey hiddenMasterkey = null;
    
    try {
        Path masterkeyPath = vaultPath.resolve(MASTERKEY_FILENAME);
        Path vaultConfigPath = vaultPath.resolve("vault.cryptomator");
        
        // 1. Load masterkey using password
        hiddenMasterkey = multiKeyslotFile.load(masterkeyPath, password);
        masterkeyBytes = hiddenMasterkey.getEncoded();
        
        // 2. Remove config slot from vault.cryptomator
        MultiKeyslotVaultConfig configHandler = new MultiKeyslotVaultConfig();
        configHandler.removeConfigSlot(vaultConfigPath, masterkeyBytes);
        
        // 3. Remove keyslot from masterkey.cryptomator
        boolean keyslotRemoved = multiKeyslotFile.removeKeyslot(masterkeyPath, password);
        
        if (keyslotRemoved) {
            LOG.debug("Removed identity from vault");
        }
        return keyslotRemoved;
    } catch (IOException e) {
        LOG.error("Failed to remove identity", e);
        return false;
    } catch (InvalidPassphraseException e) {
        LOG.warn("Password doesn't match any identity");
        return false;
    } finally {
        // Always clean up sensitive data
        if (hiddenMasterkey != null) {
            hiddenMasterkey.destroy();
        }
        if (masterkeyBytes != null) {
            Arrays.fill(masterkeyBytes, (byte) 0);
        }
    }
}
```

**Files to modify:**
- `src/main/java/org/cryptomator/common/vaults/VaultIdentityManager.java`
- `src/main/java/org/cryptomator/common/recovery/IdentityInitializer.java`

---

### 🔴 Issue 4: Masterkey Bytes Not Zeroed After Use
- [ ] Add try-finally to `Vault.prepareVaultConfigForUnlock()`
- [ ] Zero `masterkeyBytes` array in finally block
- [ ] Add cleanup of temp config file on exception
- [ ] Add unit test to verify zeroing

**Files to modify:**
- `src/main/java/org/cryptomator/common/vaults/Vault.java` (lines 180-227)

---

### 🔴 Issue 5: Unsafe Parsing Without Bounds Checks
- [ ] Add bounds check before reading count field
- [ ] Add max limit on count (e.g., 256 configs)
- [ ] Add bounds check before reading each configSize
- [ ] Add max limit on configSize (e.g., 1 MB)
- [ ] Add check that offset doesn't exceed fileData.length
- [ ] Apply to all three locations:
  - `VaultConfigCache.readConfigFromStorage()` (lines 98-115)
  - `Vault.readConfigSlotsFromMultiKeyslot()` (lines 232-260)
  - `MultiKeyslotVaultConfig.readConfigSlots()` (lines 284-324)

**Utility method to add:**
```java
private static void validateBounds(byte[] data, int offset, int length, String field) throws IOException {
    if (offset < 0 || length < 0 || offset + length > data.length) {
        throw new IOException(String.format(
            "Invalid %s: offset=%d, length=%d, dataLength=%d", 
            field, offset, length, data.length));
    }
}

private static int readIntWithBounds(byte[] data, int offset) throws IOException {
    validateBounds(data, offset, 4, "int field");
    return ((data[offset] & 0xFF) << 24) |
           ((data[offset + 1] & 0xFF) << 16) |
           ((data[offset + 2] & 0xFF) << 8) |
           (data[offset + 3] & 0xFF);
}
```

**Files to modify:**
- `src/main/java/org/cryptomator/common/vaults/VaultConfigCache.java`
- `src/main/java/org/cryptomator/common/vaults/Vault.java`
- `src/main/java/org/cryptomator/common/vaults/MultiKeyslotVaultConfig.java`

---

## High Priority Issues (Fix Before Release)

### 🟡 Issue 6: Logging Reveals Keyslot Information
- [ ] Remove slot count from log in `MultiKeyslotVaultConfig.load()` (line 123)
- [ ] Remove slot index from log in `MultiKeyslotVaultConfig.load()` (line 135)
- [ ] Remove slot count from log in `MultiKeyslotVaultConfig.addConfigSlot()` (line 196)
- [ ] Remove slot index/count from log in `MultiKeyslotVaultConfig.removeConfigSlot()` (line 253)
- [ ] Review all LOG statements for information leakage
- [ ] Replace with generic messages or use TRACE level behind debug flag

**Files to modify:**
- `src/main/java/org/cryptomator/common/vaults/MultiKeyslotVaultConfig.java`
- `src/main/java/org/cryptomator/common/vaults/Vault.java`
- `src/main/java/org/cryptomator/common/recovery/IdentityInitializer.java`

---

### 🟡 Issue 7: Cleartext Password Prompts
- [ ] Create `PasswordDialogHelper` utility class
- [ ] Replace TextInputDialog with custom Dialog + PasswordField
- [ ] Update `IdentityManagementController.onAddIdentity()` (lines 128, 140)
- [ ] Update `IdentityManagementController.onRemoveIdentity()` (line 178)
- [ ] Test that passwords are masked in UI

**Helper class to create:**
```java
package org.cryptomator.ui.common;

public class PasswordDialogHelper {
    public static Optional<String> promptPassword(Window owner, String title, String header, String prompt) {
        Dialog<String> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText(prompt);
        
        grid.add(new Label(prompt), 0, 0);
        grid.add(passwordField, 1, 0);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Focus on password field
        Platform.runLater(passwordField::requestFocus);
        
        dialog.setResultConverter(button -> 
            button == ButtonType.OK ? passwordField.getText() : null
        );
        
        return dialog.showAndWait();
    }
}
```

**Files to modify:**
- `src/main/java/org/cryptomator/ui/vaultoptions/IdentityManagementController.java`
- Create: `src/main/java/org/cryptomator/ui/common/PasswordDialogHelper.java`

---

### 🟡 Issue 8: Temp Config File Not Cleaned Up On Failure
- [ ] Wrap unlock flow in try-catch
- [ ] Call `cleanupTempVaultConfig()` in catch block
- [ ] Test unlock failure scenarios
- [ ] Verify temp file is deleted

**Files to modify:**
- `src/main/java/org/cryptomator/common/vaults/Vault.java` (unlock methods)

---

## Medium Priority Issues

### 🟠 Issue 9: Duplicate Parsing Logic
- [ ] Add `loadFirstSlotUnverified()` helper to MultiKeyslotVaultConfig
- [ ] Make `readConfigSlots()` public
- [ ] Refactor VaultConfigCache to use helper (lines 92-119)
- [ ] Refactor Vault to use helper (lines 232-260)
- [ ] Remove duplicate code

**Files to modify:**
- `src/main/java/org/cryptomator/common/vaults/MultiKeyslotVaultConfig.java`
- `src/main/java/org/cryptomator/common/vaults/VaultConfigCache.java`
- `src/main/java/org/cryptomator/common/vaults/Vault.java`

---

### 🟠 Issue 10: Missing i18n
- [ ] Create resource bundle keys for all identity UI strings
- [ ] Add keys: `identity.add.title`, `identity.add.header`, etc.
- [ ] Update IdentityManagementController to use ResourceBundle
- [ ] Update HiddenVaultCreationDialog to use ResourceBundle
- [ ] Test with different locales

**Files to modify:**
- `src/main/resources/i18n/strings.properties` (and all locale variants)
- `src/main/java/org/cryptomator/ui/vaultoptions/IdentityManagementController.java`
- `src/main/java/org/cryptomator/ui/vaultoptions/HiddenVaultCreationDialog.java`

---

### 🟠 Issue 11: Unused Dependency
- [ ] Remove `MasterkeyFileAccess` field from GeneralVaultOptionsController (line 33)
- [ ] Remove from constructor (line 49)
- [ ] Verify no functionality breaks

**Files to modify:**
- `src/main/java/org/cryptomator/ui/vaultoptions/GeneralVaultOptionsController.java`

---

### 🟠 Issue 12: Obsolete vault.bak Reference
- [ ] Remove vault.bak reference from VaultIdentityManager (line 113)
- [ ] Remove related deletion code (line 116)
- [ ] Search for any other vault.bak references in non-migration code

**Files to modify:**
- `src/main/java/org/cryptomator/common/vaults/VaultIdentityManager.java`

---

## Low Priority Issues

### 🟢 Issue 13: Revealing Temp Directory Name
- [ ] Change "hidden-vault-init" to neutral prefix like "vlt-"
- [ ] Change ".vault-config-" to neutral prefix
- [ ] Change ".keyslot-" to neutral prefix

**Files to modify:**
- `src/main/java/org/cryptomator/common/recovery/IdentityInitializer.java` (line 86)
- `src/main/java/org/cryptomator/common/vaults/MultiKeyslotVaultConfig.java` (line 190)
- `src/main/java/org/cryptomator/common/keychain/MultiKeyslotFile.java` (lines 172, 308, 325, 393)

---

### 🟢 Issue 14: Null Safety for Vault Path
- [ ] Add null check in VaultConfigCache methods
- [ ] Throw IllegalStateException with clear message
- [ ] Add to other vault path uses

**Files to modify:**
- `src/main/java/org/cryptomator/common/vaults/VaultConfigCache.java`

---

## Testing Requirements

### Unit Tests to Add
- [ ] Test primary password verification in addSecondaryIdentity
- [ ] Test identity deletion removes both keyslot and config slot
- [ ] Test bounds checking on malformed config files
- [ ] Test masterkey zeroing after use
- [ ] Test temp file cleanup on unlock failure

### Integration Tests to Add
- [ ] Test full hidden vault creation flow with wrong primary password (should fail)
- [ ] Test full hidden vault creation flow with correct primary password (should succeed)
- [ ] Test full hidden vault deletion flow
- [ ] Test unlock with multiple keyslots
- [ ] Test vault.bak migration

### Manual Testing
- [ ] Create hidden vault via UI
- [ ] Verify no information leakage in logs
- [ ] Delete hidden vault via UI
- [ ] Verify both files are updated
- [ ] Check temp directory for leaked files

---

## Documentation Updates
- [ ] Update SECURITY.md with plausible deniability guarantees
- [ ] Document file format changes
- [ ] Update user-facing docs about hidden vaults
- [ ] Add security best practices guide

---

## Before Merge Checklist
- [ ] All 🔴 Critical issues resolved
- [ ] All 🟡 High priority issues resolved
- [ ] Code review completed
- [ ] Security review completed
- [ ] All tests passing
- [ ] No information leakage in logs
- [ ] Documentation updated

---

*Last updated: 2025-10-19*

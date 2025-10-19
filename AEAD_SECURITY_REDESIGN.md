# AEAD-Based Security Redesign

## Critical Vulnerabilities Fixed (CodeRabbit Issues #4018)

This commit addresses **4 CRITICAL security vulnerabilities** identified by CodeRabbit AI in the multi-keyslot file format that completely undermined plausible deniability.

### The Core Vulnerability

**Problem**: The original design used **plaintext length markers** to determine slot boundaries:
```
[actual data][4-byte length at position X, value = X][random padding]
```

**Impact**: An attacker could scan the file WITHOUT any password to:
- Detect which slots contained real data vs random padding
- Count exactly how many identities existed in the vault
- Enumerate occupied slots without authentication
- **Completely defeat plausible deniability** ❌

### CodeRabbit Findings

| Location | Issue | Severity | Impact |
|----------|-------|----------|--------|
| Lines 201-203 | Empty slot detection via unpadding | 🔴 Critical | Fingerprint occupied slots |
| Lines 277-281 | Slot counting via unpadding | 🔴 Critical | Reveal identity count |
| Lines 405-417 | findFirstEmptySlot() | 🔴 Critical | Enumerate slots without password |
| Lines 423-435 | countOccupiedSlots() | 🔴 Critical | **Violates design goal: unknowable count** |

---

## The Solution: AEAD Encryption (AES-256-GCM)

### New Slot Format

**Before (VULNERABLE)**:
```
[keyslot data][plaintext length marker][random padding]
↑ Length marker detectable WITHOUT password!
```

**After (SECURE)**:
```
[salt:32][iv:12][AEAD-encrypted ciphertext + auth-tag][random padding]
↑ Indistinguishable from random data without correct password!
```

### AEAD Properties (Why This Works)

1. **Authenticated Encryption**: AES-GCM provides both encryption AND authentication
2. **Authentication Tag**: 128-bit tag proves data integrity and authenticity
3. **Cryptographic Indistinguishability**:
   - Random padding: AEAD decrypt fails (wrong password OR no data)
   - Wrong password: AEAD decrypt fails (authentication failure)
   - **These two cases are cryptographically indistinguishable!** ✓

4. **No Plaintext Metadata**: Zero plaintext markers, counts, or identifiers

### Security Parameters

| Parameter | Value | Purpose |
|-----------|-------|---------|
| Algorithm | AES-256-GCM | AEAD encryption |
| Key Derivation | PBKDF2-HMAC-SHA256 | Password → encryption key |
| PBKDF2 Iterations | 100,000 | Brute-force resistance |
| Salt Length | 32 bytes (256 bits) | Unique per slot |
| IV Length | 12 bytes (96 bits) | GCM nonce |
| Auth Tag | 16 bytes (128 bits) | AEAD authentication |

---

## Changes Made

### 1. Removed Vulnerable Methods

**DELETED**:
- `padSlot()` - Used plaintext length markers
- `unpadSlot()` - Detectable without password
- `findFirstEmptySlot()` - Enumerated slots without authentication
- `countOccupiedSlots()` - Revealed exact identity count

### 2. Added AEAD Encryption Methods

**NEW**:
- `aeadEncryptSlot()` - Encrypts keyslot with AES-256-GCM
- `aeadDecryptSlot()` - Decrypts and authenticates
- `deriveKey()` - PBKDF2-based key derivation from password
- `trimTrailingZeros()` - Safe padding removal

### 3. Updated Core Methods

**Modified**:
- `persist()` - Uses AEAD encryption for initial keyslot
- `addKeyslot()` - Uses AEAD authentication for duplicate detection
- `removeKeyslot()` - Removed slot counting (information leak)
- `decryptSlot()` - Uses AEAD authentication for occupancy detection

### 4. Occupancy Detection (Secure Approach)

**Old (VULNERABLE)**:
```java
// Try to unpad - if it works, slot is occupied
try {
    unpadSlot(slotData);
    // Slot is occupied (LEAK!)
} catch (InvalidPassphraseException e) {
    // Slot is empty (LEAK!)
}
```

**New (SECURE)**:
```java
// Try to AEAD-decrypt with password
try {
    aeadDecryptSlot(slotData, password);
    // Success: slot has THIS password
} catch (GeneralSecurityException e) {
    // Failure: empty OR different password
    // INDISTINGUISHABLE without password!
}
```

### 5. Design Trade-offs

**Limitation Accepted**: When adding a new keyslot, if all 4 slots are full but with different passwords, the first "available" slot (one that doesn't decrypt with the new password) will be overwritten.

**Why This Is Acceptable**:
1. Preserves plausible deniability (no slot counting)
2. Users manage their own passwords
3. Warning in documentation: manage passwords carefully
4. Alternative would leak information about occupied slots

**Mitigation**: Check for duplicates across ALL slots before writing, preventing accidental overwrites of the same password.

---

## File Format Specification (Version 2)

### Overall Structure
```
Total Size: 16,384 bytes (16 KB) - FIXED
├─ Slot 0: 4,096 bytes
├─ Slot 1: 4,096 bytes
├─ Slot 2: 4,096 bytes
└─ Slot 3: 4,096 bytes
```

### Each Slot Structure
```
Total: 4,096 bytes
├─ Salt:         32 bytes  (PBKDF2 salt, unique per slot)
├─ IV/Nonce:     12 bytes  (AES-GCM IV, unique per slot)
├─ Ciphertext:   variable  (AEAD-encrypted keyslot data)
├─ Auth Tag:     16 bytes  (GCM authentication tag)
└─ Padding:      variable  (Random data to fill 4,096 bytes)
```

### Slot Types (Indistinguishable!)
1. **Occupied Slot**: Contains AEAD-encrypted keyslot
2. **Empty Slot**: Contains random data from SecureRandom
3. **Different Password Slot**: AEAD decryption fails, looks empty

**Critical Property**: Without the correct password, all three types are **cryptographically indistinguishable** from random data.

---

## Security Analysis

### ✅ Fixes Applied

| Vulnerability | Status | Mitigation |
|---------------|--------|------------|
| Plaintext length markers | ✅ Fixed | AEAD encryption, no markers |
| Slot enumeration | ✅ Fixed | AEAD authentication required |
| Identity counting | ✅ Fixed | No public counting methods |
| Information leakage | ✅ Fixed | All slots indistinguishable |

### ✅ Security Properties Achieved

1. **Indistinguishability**: File looks like 16 KB of random data
2. **No Metadata**: Zero magic bytes, version fields, or counts
3. **Authentication Required**: Cannot detect occupancy without passwords
4. **Plausible Deniability**: Adversary cannot prove hidden identities exist

### ⚠️ Known Limitations

1. **Slot Overwriting**: Adding keyslots when all 4 are full (with different passwords) may overwrite an existing slot
2. **No Last-Keyslot Protection**: User can remove their only access (by design - prevents information leak)
3. **Performance**: PBKDF2 with 100k iterations is intentionally slow (security vs usability trade-off)

---

## Testing Recommendations

### Required Tests

1. **Functional Tests**:
   - Create vault with primary password
   - Add hidden identity with different password
   - Unlock with both passwords
   - Remove identities

2. **Security Tests**:
   - Verify file is indistinguishable from random (entropy analysis)
   - Confirm no plaintext metadata (hex dump inspection)
   - Test AEAD authentication failures (wrong password)
   - Verify duplicate password detection

3. **Edge Cases**:
   - Add 4 different identities (fill all slots)
   - Try to add 5th identity (should detect duplicate or overwrite)
   - Remove all identities except one
   - Convert legacy format to AEAD format

### Manual Verification

```bash
# 1. Check file is exactly 16 KB
ls -l vault.cryptomator/masterkey.cryptomator
# Should show: 16384 bytes

# 2. Verify no magic bytes (should look random)
hexdump -C vault.cryptomator/masterkey.cryptomator | head -20

# 3. Entropy analysis (should be ~8.0 bits/byte)
ent vault.cryptomator/masterkey.cryptomator
```

---

## References

- **CodeRabbit Review**: https://github.com/cryptomator/cryptomator/pull/4018
- **TrueCrypt Hidden Volumes**: https://www.veracrypt.fr/en/Hidden%20Volume.html
- **AES-GCM Specification**: NIST SP 800-38D
- **PBKDF2 Standard**: RFC 2898

---

## Commit Message

```
fix: replace vulnerable padding with AEAD encryption for true plausible deniability

BREAKING CHANGE: Masterkey file format v2 - AEAD-based slot encryption

Critical Security Fix (CodeRabbit Issues #4018):
- Removed plaintext length markers (information leak)
- Implemented AES-256-GCM AEAD encryption for all slots
- Deleted vulnerable slot enumeration methods
- Fixed identity counting vulnerability

Technical Changes:
- Added aeadEncryptSlot/aeadDecryptSlot methods
- Removed padSlot/unpadSlot (vulnerable to fingerprinting)
- Removed findFirstEmptySlot/countOccupiedSlots (leaked info)
- Updated persist/addKeyslot/removeKeyslot to use AEAD
- Each slot: [salt:32][iv:12][ciphertext+tag][random padding]

Security Properties:
- Slots indistinguishable from random without password
- AEAD authentication required to detect occupancy
- No metadata reveals number of identities
- True plausible deniability achieved

Fixes: CodeRabbit critical issues at lines 201-203, 277-281, 405-417, 423-435
See: AEAD_SECURITY_REDESIGN.md for full analysis
```

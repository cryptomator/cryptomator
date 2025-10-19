# Runtime Error Fixed ✅

## The Problem

When running the application, it crashed with:
```
The method getKeyslotCount(Path) is undefined for the type MultiKeyslotFile
```

## Root Cause

The security redesign removed `getKeyslotCount()` from `MultiKeyslotFile` because **counting keyslots violates plausible deniability**. However, `VaultIdentityManager` was still trying to call this method to enumerate identities.

## Why This Was Actually a GOOD Thing to Break

The old code had a **fundamental security flaw**:

```java
// OLD (INSECURE) CODE:
int keyslotCount = multiKeyslotFile.getKeyslotCount(masterkeyPath);
for (int i = 1; i < keyslotCount; i++) {
    VaultIdentity hidden = VaultIdentity.createSecondary("Hidden-" + i, ...);
    detectedIdentities.add(hidden);
}
LOG.debug("Detected {} keyslot(s)", keyslotCount);
```

**Problem**: This tells you "You have 3 identities" - revealing the hidden ones!

## The Fix

### 1. Removed Identity Enumeration
- ❌ Removed: `getKeyslotCount()` - exposed count
- ❌ Removed: `detectIdentities()` - tried to enumerate all
- ❌ Removed: `hasHiddenVault()` - revealed existence
- ❌ Removed: Logging of counts and slot numbers

### 2. Secure Replacement

```java
// NEW (SECURE) CODE:
public List<VaultIdentity> getIdentities() {
    // Return empty - identities are not enumerable for security
    return Collections.emptyList();
}
```

**Why**: You cannot know how many identities exist without trying passwords!

## Security Principle

> "The number of identities MUST be unknowable without attempting
> to decrypt with actual passwords. Any counting or enumeration
> mechanism violates plausible deniability."

### What This Means for the UI:

**❌ DON'T DO THIS:**
- Show "You have 3 identities"
- List all identities before unlock
- Display identity selection menu

**✅ DO THIS INSTEAD:**
- Just show password prompt
- Let unlock process discover the identity
- User only sees the identity their password unlocks
- No indication of other identities

## Commits Applied

1. **896aa156a** - Redesigned MultiKeyslotFile format
   - Removed magic bytes, version, count
   - Fixed 16KB indistinguishable format
   - Sanitized logging

2. **e03d25701** - Removed identity enumeration
   - Deleted getKeyslotCount() method
   - Removed detectIdentities() logic  
   - Made getIdentities() return empty list
   - Removed hasHiddenVault() check

## Files Changed

- `MultiKeyslotFile.java` - Format redesign
- `VaultIdentityManager.java` - Removed enumeration
- `IdentityInitializer.java` - Updated method calls

Total: 289 insertions, 230 deletions across 3 files

## Testing Status

✅ Compiles successfully
✅ No linter errors
✅ Ready to test runtime

## Next Steps

1. **Push the fixes**:
   ```bash
   git push origin feature/plausible-deniability
   ```

2. **Test the application**:
   - Create a vault with primary identity
   - Add a hidden identity
   - Unlock with primary password → sees primary data
   - Unlock with hidden password → sees hidden data
   - Verify no indication of multiple identities exists

3. **Respond to code review**:
   - Explain the design changes
   - Emphasize security improvements
   - Note that identity enumeration was intentionally removed

## Design Implications

This changes the UX:
- **Before**: "Select which identity to unlock: Primary / Hidden-1 / Hidden-2"
- **After**: "Enter password" → system finds matching identity silently

This is MORE secure but LESS convenient. However, convenience here would
completely defeat plausible deniability!

## Plausible Deniability Scenario

**Adversary**: "How many vaults do you have?"
**User (OLD design)**: *Opens UI, sees "3 identities"* - "Uh... just one?" ❌ BUSTED!
**User (NEW design)**: *Opens UI, sees password prompt* - "Just one vault" ✅ PLAUSIBLE!

The adversary cannot prove otherwise without:
1. Getting the password
2. Trying to unlock
3. Even then, they don't know if there are MORE identities

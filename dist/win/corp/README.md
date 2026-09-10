# Cryptomator corp build (per-user MSI)

`build-corp.bat` produces `Cryptomator-<version>-<arch>-corp.msi`, a per-user installer for managed machines where users have no admin rights.

## What is different from the regular installer

| | Regular installer (`build.bat`) | Corp MSI (`build-corp.bat`) |
|---|---|---|
| Install scope | per machine, `C:\Program Files\Cryptomator`, needs admin | per user, `%LOCALAPPDATA%\Cryptomator`, no admin |
| WinFsp | bundled in the exe installer | not bundled, see below |
| Loopback alias `cryptomator-vault` in hosts file | added by installer | not added, WebDAV uses `localhost` |
| `C:\ProgramData\Cryptomator\config.properties` | created by installer | not created, but still read if IT deploys it |
| Update check | on (can be turned off with `DISABLEUPDATECHECK=true`) | off |
| Upgrade code | `bda45523-42b1-4cae-9354-a45475ed4775` | `d98d0145-341a-4684-bdb6-ee480294541a` |

The different upgrade code means the corp MSI and the regular installer are treated as separate products by Windows Installer. Do not install both on the same machine for the same user.

## Installing

Interactive: double-click the MSI. Silent, e.g. from a software deployment tool or a logon script running as the user:

```
msiexec /i Cryptomator-1.19.3-x64-corp.msi /qn
```

Uninstall silently:

```
msiexec /x Cryptomator-1.19.3-x64-corp.msi /qn
```

## Mounting vaults: WinFsp or WebDAV

Cryptomator mounts vaults as drives via [WinFsp](https://winfsp.dev). WinFsp is a kernel driver and can only be installed with admin rights, so the corp MSI does not bundle it. Two options:

1. **IT deploys WinFsp** (recommended). The WinFsp MSI is signed and installs silently per machine, e.g. `msiexec /i winfsp-2.1.25156.msi /qn`. Cryptomator picks it up automatically. The exact version and SHA-256 the regular installer bundles are listed in `dist/win/build.ps1`.
2. **WebDAV fallback.** Without WinFsp, Cryptomator serves vaults over WebDAV on `localhost` and mounts them with the built-in Windows WebDAV client. This requires the *WebClient* service, which some organizations disable by policy, and Windows limits WebDAV file transfers to 50 MB unless `HKLM\SYSTEM\CurrentControlSet\Services\WebClient\Parameters\FileSizeLimitInBytes` is raised (admin).

## Enforcing settings (admin config)

The app reads `C:\ProgramData\Cryptomator\config.properties` if it exists. The corp MSI does not create it, so IT can deploy the file through GPO preferences, Intune or a script, and standard users cannot change it. Supported keys are documented at <https://docs.cryptomator.org/desktop/admin-config/>; a template is in `dist/common/config.properties`. Example:

```
# only allow the company hub, no trust-on-first-use prompts
cryptomator.hub.allowedHosts=hub.example.com
cryptomator.hub.enableTrustOnFirstUse=false
# keep logs where the helpdesk can collect them
cryptomator.logDir=@{localappdata}/Cryptomator
```

## Application control (AppLocker, WDAC, SRP)

A per-user install lives under `%LOCALAPPDATA%`, which the default AppLocker rules block. Ask IT for one of:

- a **publisher rule** for the code-signing certificate, which works regardless of install path. This needs signed binaries; locally built MSIs are unsigned.
- a **path rule** for `%LOCALAPPDATA%\Cryptomator\*`.

If neither is possible, the regular per-machine installer deployed by IT is the better fit, since `C:\Program Files` is allowed by default.

## Proxies and TLS inspection

The app uses the system proxy settings. On Windows it trusts the certificates in the Windows certificate store, so a corporate root CA for TLS inspection that IT has deployed via GPO is trusted automatically for Cryptomator Hub connections.

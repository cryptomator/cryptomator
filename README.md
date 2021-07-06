[![cryptomator](cryptomator.png)](https://cryptomator.org/)

[![Build](https://github.com/cryptomator/cryptomator/workflows/Build/badge.svg)](https://github.com/cryptomator/cryptomator/actions?query=workflow%3ABuild)
[![Known Vulnerabilities](https://snyk.io/test/github/cryptomator/cryptomator/badge.svg)](https://snyk.io/test/github/cryptomator/cryptomator)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/2a0adf3cec6a4143b91035d3924178f1)](https://www.codacy.com/gh/cryptomator/cryptomator/dashboard)
[![Twitter](https://img.shields.io/badge/twitter-@Cryptomator-blue.svg?style=flat)](http://twitter.com/Cryptomator)
[![Crowdin](https://badges.crowdin.net/cryptomator/localized.svg)](https://translate.cryptomator.org/)
[![Latest Release](https://img.shields.io/github/release/cryptomator/cryptomator.svg)](https://github.com/cryptomator/cryptomator/releases/latest)
[![Community](https://img.shields.io/badge/help-Community-orange.svg)](https://community.cryptomator.org)

## Supporting Cryptomator

Cryptomator is provided free of charge as an open-source project despite the high development effort and is therefore dependent on donations. If you are also interested in further development, we offer you the opportunity to support us:

- [One-time or recurring donation via Cryptomator's website.](https://cryptomator.org/#donate)
- [Become a sponsor via Cryptomator's sponsors website.](https://cryptomator.org/sponsors/)

### Gold Sponsors

<table>
  <tbody>
    <tr>
      <td><a href="https://www.gee-whiz.de/"><img src="https://cryptomator.org/img/sponsors/geewhiz.svg" alt="gee-whiz" height="80"></a></td>
      <td><a href="https://proxy-hub.com/"><img src="https://cryptomator.org/img/sponsors/proxyhub.svg" alt="Proxy-Hub" height="80"></a></td>
    </tr>
  </tbody>
</table>

### Silver Sponsors

<table>
  <tbody>
    <tr>
      <td><a href="https://thebestvpn.com/"><img src="https://cryptomator.org/img/sponsors/thebestvpn@2x.png" alt="TheBestVPN" height="64"></a></td>
    </tr>
  </tbody>
</table>

- [Jameson Lopp](https://www.lopp.net/)

---

## Introduction

Cryptomator offers multi-platform transparent client-side encryption of your files in the cloud.

Download native binaries of Cryptomator on [cryptomator.org](https://cryptomator.org/) or clone and build Cryptomator using Maven (instructions below).

## Features

- Works with Dropbox, Google Drive, OneDrive, MEGA, pCloud, ownCloud, Nextcloud and any other cloud storage service which synchronizes with a local directory
- Open Source means: No backdoors, control is better than trust
- Client-side: No accounts, no data shared with any online service
- Totally transparent: Just work on the virtual drive as if it were a USB flash drive
- AES encryption with 256-bit key length
- File names get encrypted
- Folder structure gets obfuscated
- Use as many vaults in your Dropbox as you want, each having individual passwords
- Three thousand commits for the security of your data!! :tada:

### Privacy

- 256-bit keys (unlimited strength policy bundled with native binaries)
- Scrypt key derivation
- Cryptographically secure random numbers for salts, IVs and the masterkey of course
- Sensitive data is wiped from the heap asap
- Lightweight: [Complexity kills security](https://www.schneier.com/essays/archives/1999/11/a_plea_for_simplicit.html)

### Consistency

- HMAC over file contents to recognize changed ciphertext before decryption
- I/O operations are transactional and atomic, if the filesystems support it
- Each file contains all information needed for decryption (except for the key of course), no common metadata means no [SPOF](http://en.wikipedia.org/wiki/Single_point_of_failure)

### Security Architecture

For more information on the security details visit [cryptomator.org](https://docs.cryptomator.org/en/latest/security/architecture/).

## Building

### Dependencies

* JDK 16 (e.g. adoptopenjdk)
* Maven 3
* Optional: OS-dependent build tools for native packaging (see [Windows](https://github.com/cryptomator/cryptomator-win), [OS X](https://github.com/cryptomator/cryptomator-osx), [Linux](https://github.com/cryptomator/builder-containers))

### Run Maven

```
mvn clean install
# or mvn clean install -Pwindows
# or mvn clean install -Pmac
# or mvn clean install -Plinux
```

This will build all the jars and bundle them together with their OS-specific dependencies under `target`. This can now be used to build native packages.

### Start Cryptomator

If you unzip the buildkit for your OS, you will find a launcher script with some basic settings. You might want to adjust these to your needs. To start Cryptomator, simply execute the launcher script from a terminal, e.g. `launcher-linux.sh`, if you're on a Linux system.

## License

This project is dual-licensed under the GPLv3 for FOSS projects as well as a commercial license for independent software vendors and resellers. If you want to modify this application under different conditions, feel free to contact our support team.

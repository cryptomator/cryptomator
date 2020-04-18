[![cryptomator](cryptomator.png)](https://cryptomator.org/)

[![Build Status](https://travis-ci.org/cryptomator/cryptomator.svg?branch=master)](https://travis-ci.org/cryptomator/cryptomator)
[![Known Vulnerabilities](https://snyk.io/test/github/cryptomator/cryptomator/badge.svg?targetFile=main%2Fpom.xml)](https://snyk.io/test/github/cryptomator/cryptomator?targetFile=main%2Fpom.xml)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/2a0adf3cec6a4143b91035d3924178f1)](https://www.codacy.com/app/cryptomator/cryptomator?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=cryptomator/cryptomator&amp;utm_campaign=Badge_Grade)
[![Twitter](https://img.shields.io/badge/twitter-@Cryptomator-blue.svg?style=flat)](http://twitter.com/Cryptomator)
[![Crowdin](https://badges.crowdin.net/cryptomator/localized.svg)](https://translate.cryptomator.org/)
[![Latest Release](https://img.shields.io/github/release/cryptomator/cryptomator.svg)](https://github.com/cryptomator/cryptomator/releases/latest)
[![Community](https://img.shields.io/badge/help-Community-orange.svg)](https://community.cryptomator.org)

## Supporting Cryptomator

Cryptomator is provided free of charge as an open-source project despite the high development effort and is therefore dependent on donations. If you are also interested in further development, we offer you the opportunity to support us:

- [One-time or recurring donation via Cryptomator's website.](https://cryptomator.org/#donate)
- [Become a sponsor via Cryptomator's sponsors website.](https://cryptomator.org/sponsors/)

### Gold Sponsors

[<img src="https://cryptomator.org/img/sponsors/geewhiz.svg" alt="gee-whiz" height="96">](https://www.gee-whiz.de/)

### Silver Sponsors

[![TheBestVPN](https://cryptomator.org/img/sponsors/thebestvpn.png)](https://thebestvpn.com/)

---

## Introduction

Cryptomator offers multi-platform transparent client-side encryption of your files in the cloud.

Download native binaries of Cryptomator on [cryptomator.org](https://cryptomator.org/) or clone and build Cryptomator using Maven (instructions below).

## Features

- Works with Dropbox, Google Drive, OneDrive, ownCloud, Nextcloud and any other cloud storage service which synchronizes with a local directory
- Open Source means: No backdoors, control is better than trust
- Client-side: No accounts, no data shared with any online service
- Totally transparent: Just work on the virtual drive as if it were a USB flash drive
- AES encryption with 256-bit key length
- File names get encrypted
- Folder structure gets obfuscated
- Use as many vaults in your Dropbox as you want, each having individual passwords
- One thousand commits for the security of your data!! :tada:

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

* JDK 11 (we recommend to use the latest version)
* Maven 3
* Optional: OS-dependent build tools for native packaging (see [Windows](https://github.com/cryptomator/cryptomator-win), [OS X](https://github.com/cryptomator/cryptomator-osx), [Linux](https://github.com/cryptomator/builder-containers))

### Run Maven

```
cd main
mvn clean install -Prelease
```

This will build all the jars and bundle them together with their OS-specific dependencies under `main/buildkit/target`. This can now be used to build native packages.

### Start Cryptomator

If you unzip the buildkit for your OS, you will find a launcher script with some basic settings. You might want to adjust these to your needs. To start Cryptomator, simply execute the launcher script from a terminal, e.g. `launcher-linux.sh`, if you're on a Linux system.

## License

This project is dual-licensed under the GPLv3 for FOSS projects as well as a commercial license for independent software vendors and resellers. If you want to modify this application under different conditions, feel free to contact our support team.

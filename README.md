![cryptomator](cryptomator.png)

[![Build Status](https://travis-ci.org/cryptomator/cryptomator.svg?branch=master)](https://travis-ci.org/cryptomator/cryptomator)
[![Coverity Scan Build Status](https://scan.coverity.com/projects/cryptomator-cryptomator/badge.svg?flat=1)](https://scan.coverity.com/projects/cryptomator-cryptomator)
[![Coverage Status](https://coveralls.io/repos/github/cryptomator/cryptomator/badge.svg?branch=master)](https://coveralls.io/github/cryptomator/cryptomator?branch=master)
[![Join the chat at https://gitter.im/cryptomator/cryptomator](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/cryptomator/cryptomator?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Twitter](https://img.shields.io/badge/twitter-@Cryptomator-blue.svg?style=flat)](http://twitter.com/Cryptomator)
[![POEditor](https://img.shields.io/badge/POEditor-Help%20Translate-blue.svg?style=flat)](https://poeditor.com/join/project/bHwbvJmx0E)

Multi-platform transparent client-side encryption of your files in the cloud.

Download native binaries of Cryptomator on [cryptomator.org](https://cryptomator.org/) or clone and build Cryptomator using Maven (instructions below).

## Features

- Works with Dropbox, Google Drive, OneDrive, and any other cloud storage service that synchronizes with a local directory
- Open Source means: No backdoors, control is better than trust
- Client-side: No accounts, no data shared with any online service
- Totally transparent: Just work on the virtual drive as if it were a USB flash drive
- AES encryption with 256-bit key length
- Filenames get encrypted, too
- Use as many vaults in your Dropbox as you want, each having individual passwords

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

For more information on the security details visit [cryptomator.org](https://cryptomator.org/architecture/).

## Building

### Dependencies

* Java 8 + JCE unlimited strength policy files (needed for 256-bit keys)
* Maven 3
* Optional: OS-dependent build tools for native packaging (see [Windows](https://github.com/cryptomator/cryptomator-win), [OS X](https://github.com/cryptomator/cryptomator-osx), [Debian](https://github.com/cryptomator/cryptomator-deb))

### Run Maven

```
cd main
mvn clean install
```

## Contributing to Cryptomator

Please read our [contribution guide](https://github.com/cryptomator/cryptomator/blob/master/CONTRIBUTING.md), if you would like to report a bug, ask a question or help us with coding.

## Code of Conduct

Help us keep Cryptomator open and inclusive. Please read and follow our [Code of Conduct](https://github.com/cryptomator/cryptomator/blob/master/CODE_OF_CONDUCT.md).

## License

Distributed under the MIT X Consortium license. See the `LICENSES/MIT-X-Consortium-License.txt` file for more info.

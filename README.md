Cryptomator
====================

[![Build Status](https://travis-ci.org/totalvoidness/cryptomator.svg?branch=master)](https://travis-ci.org/totalvoidness/cryptomator)
[![Join the chat at https://gitter.im/totalvoidness/cryptomator](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/totalvoidness/cryptomator?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Multiplatform transparent client-side encryption of your files in the cloud.

If you want to take a look at the current beta version, go ahead and get your copy of cryptomator on  [Cryptomator.org](https://cryptomator.org) or clone and build Cryptomator using Maven (instructions below).

## Features
- Totally transparent: Just work on the encrypted volume, as if it was an USB flash drive
- Works with Dropbox, OneDrive (Skydrive), Google Drive and any other cloud storage, that syncs with a local directory.
- In fact it works with any directory. You can use it to encrypt as many folders as you like
- AES encryption with 256 bit key length
- Client-side. No accounts, no data shared with any online service
- Filenames get encrypted too
- No need to provide credentials for any 3rd party service
- Open Source means: No backdoors. Control is better than trust
- Use as many encrypted folders in your dropbox as you want. Each having individual passwords
- No commerical interest, no government agency, no wasted taxpayers' money ;-)

### Privacy
- 256 bit keys (unlimited strength policy bundled with native binaries - 128 bit elsewhere)
- Scrypt key derivation
- Cryptographically secure random numbers for salts, IVs and the masterkey of course
- Sensitive data is swiped from the heap asap
- Lightweight: [Complexity kills security](https://www.schneier.com/essays/archives/1999/11/a_plea_for_simplicit.html)

### Consistency
- HMAC over file contents to recognize changed ciphertext before decryption
- I/O operations are transactional and atomic, if the file systems supports it
- Each file contains all information needed for decryption (except for the key of course). No common metadata means no [SPOF](http://en.wikipedia.org/wiki/Single_point_of_failure)

## Building

#### Dependencies
* Java 8
* Maven 3
* Optional: OS-dependent build tools for native packaging
* Optional: JCE unlimited strength policy files (needed for 256 bit keys)

#### Building on Debian-based OS
```bash
apt-get install oracle-java8-installer oracle-java8-unlimited-jce-policy fakeroot maven git
git clone https://github.com/totalvoidness/cryptomator.git
cd cryptomator/main
git checkout v0.5.1
mvn clean install
```

## License

Distributed under the MIT X Consortium license. See the LICENSE file for more info.


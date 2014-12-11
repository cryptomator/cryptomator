Cryptomator
====================

Multiplatform transparent client-side encryption of your files in the cloud. You need Java 8 in order to run the application. Get the runtime environment here: http://www.oracle.com/technetwork/java/javase/downloads/index.html

If you want to take a look at the current beta version, go ahead and download [Cryptomator.dmg](https://github.com/totalvoidness/cryptomator/releases/download/v0.2.0/Cryptomator.dmg), [Cryptomator.exe](https://github.com/totalvoidness/cryptomator/releases/download/v0.2.0/Cryptomator.exe) or [Cryptomator.jar](https://github.com/totalvoidness/cryptomator/releases/download/v0.2.0/Cryptomator.jar).

## Features
- Totally transparent: Just work on the encrypted volume, as if it was an USB drive
- Works with Dropbox, OneDrive (Skydrive), Google Drive and any other cloud storage, that syncs with a local directory
- In fact it works with any directory. You can use it to encrypt as many folders as you like
- AES encryption with up to 256 bit key length
- Client-side. No accounts, no data shared with any online service
- Filenames get encrypted too
- No need to provide credentials for any 3rd party service
- Open Source means: No backdoors. Control is better than trust
- Use as many encrypted folders in your dropbox as you want. Each having individual passwords

## Security
- Default key length is 256 bit (falls back to 128 bit, if JCE isn't installed)
- PBKDF2 key generation
- 4096 bit internal masterkey
- Cryptographically secure random numbers for salts, IVs and the masterkey of course
- Sensitive data is swiped from the heap asap
- Lightweight: Complexity kills security

## Consistency
- I/O operations are transactional and atomic, if the file systems supports it
- ~~Metadata is stored per-folder, so it's not a SPOF~~
- *NEW:* No Metadata at all. Encrypted files can be decrypted even on completely shuffled file systems (if their contents are undamaged).

## Dependencies
- Java 8 (for UI only - runs headless on Java 7)
- Maven
- Awesome 3rd party open source libraries (Apache Commons, Apache Jackrabbit, Jetty, Jackson, ...)

## TODO

### Core
- Support for HTTP range requests

### UI
- Automount of WebDAV volumes for Win/Tux
- Drive icons in WebDAV volumes
- Change password functionality
- Better explanations on UI

## License

Distributed under the MIT license. See the LICENSE file for more info.

[![Build Status](https://travis-ci.org/totalvoidness/cryptomator.svg?branch=master)](https://travis-ci.org/totalvoidness/cryptomator)

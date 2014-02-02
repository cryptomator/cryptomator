Open Cloud Encryptor
====================

Multiplatform transparent client-side encryption of your files in the cloud.

## Features
- Totally transparent: Just work on the encrypted volume, as if it was an USB drive
- Works with Dropbox, Skydrive, Google Drive and any other cloud storage, that syncs with a local directory
- AES encryption with up to 256 bit key length
- Client-side. No accounts, no data shared with any online service
- Filenames get encrypted too
- No need to provide credentials for any 3rd party service
- Open Source means: No backdoors. Control is better than trust
- Use as many encrypted folders in your dropbox as you want. Each having individual passwords

## Security
- Default key length is 256 bit (falls back to 128 bit, if JCE isn't installed)
- PBKDF2 key generation
- 4096 internal bit masterkey
- Cryptographically secure random numbers for salts, IVs and the masterkey of course
- Sensitive data is swiped from the heap asap
- Lightweight: Complexity kills security

## Consistency
- I/O operations are transactional and atomic, if the file systems supports it
- Metadata is stored per-folder, so it's not a SPOF

## Dependencies
- Java 7 with Java FX 2 enabled or Java 8 beta
- Maven

## TODO
- Automount of WebDAV volumes for Win/Mac/Tux
- App icon and drive icons in WebDAV volumes
- Change password functionality
- Replace WebDAV implementation by more efficient and robust solution
- CRC32 checksums for decrypted files
- Better explanations on UI

## License

Distributed under the MIT license. See the LICENSE file for more info.

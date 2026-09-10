Cryptomator - Portable Edition
==============================

This is the portable build of Cryptomator. It does not need to be installed:
extract the archive anywhere (e.g. to a USB drive) and start Cryptomator.exe.

All application data is kept in the "data" folder next to Cryptomator.exe:

  data\settings.json               your vault list and preferences
  data\keychain.json               saved vault passwords (protected with the
                                   Windows Data Protection API)
  data\windowsHelloKeychain.json   passwords protected with Windows Hello
  data\key.p12                     device key used for Cryptomator Hub vaults
  data\logs\                       log files
  data\config.properties           optional overrides, same format as the
                                   admin config of the installed version

To move Cryptomator to another computer, copy the whole folder. Note that
passwords saved via the Windows Data Protection API or Windows Hello are
bound to the Windows user account they were created with and cannot be
decrypted on another machine; you will simply be asked for the password again.

Differences to the installed version
------------------------------------

* WinFsp is not bundled. If WinFsp (https://winfsp.dev) is installed on the
  computer, vaults can be mounted as drives via WinFsp. Otherwise vaults are
  mounted via WebDAV, which is built into Windows.
* No entry in the Windows autostart, no Start menu entry and no file
  association for .cryptomator files are created.
* The "Cryptomator (Debug).exe" launcher opens a console window with
  additional log output. Use it if you need to report a problem.

Cryptomator is dual-licensed under the GPLv3 for FOSS projects as well as a
commercial license. See LICENSE.txt and https://cryptomator.org.

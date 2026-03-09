# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

The changelog starts with version 1.19.0.
Changes to prior versions can be found on the [Github release page](https://github.com/cryptomator/cryptomator/releases).


## [Unreleased](https://github.com/cryptomator/cryptomator/compare/1.19.0...HEAD)

No changes yet.


## [1.19.0](https://github.com/cryptomator/cryptomator/releases/tag/1.19.0) - 2026-03-09

### Added
* Self-Update Mechanism ([#3948](https://github.com/cryptomator/cryptomator/pull/3948))
  * Implemented `.dmg` update mechanism
  * Implemented Flatpak update mechanism
* App notifications ([#4069](https://github.com/cryptomator/cryptomator/pull/4069))
* Mark files in-use for Hub vaults ([#4078](https://github.com/cryptomator/cryptomator/pull/4078))
* Accessibility: Adjust app to be used with a screen reader ([#547](https://github.com/cryptomator/cryptomator/issues/547))
* Show Archived Vault Dialog on unlock when Hub returns 410 ([#4081](https://github.com/cryptomator/cryptomator/pull/4081))
* Support automatic app theme selection according to OS theme on Linux ([#4027](https://github.com/cryptomator/cryptomator/issues/4027))
* Admin configuration: Allow overwriting certain app properties by external config file ([#4105](https://github.com/cryptomator/cryptomator/pull/4105))
* New keychain backend using [secret service API](https://specifications.freedesktop.org/secret-service/0.2) for Linux ([#4025](https://github.com/cryptomator/cryptomator/pull/4025))
* Liquid Glass icon for macOS ([#4166](https://github.com/cryptomator/cryptomator/pull/4166))

### Fixed
* Fixed password reset/show recovery possible for vaults without masterkey file ([#4120](https://github.com/cryptomator/cryptomator/pull/4120))
* Fixed restore vault config failed due to selecting a directory instead of file ([#4141](https://github.com/cryptomator/cryptomator/issues/4141))
* Fixed leaking of cleartext paths into application log ([GHSA-j83j-mwhc-rcgw](https://github.com/cryptomator/cryptomator/security/advisories/GHSA-j83j-mwhc-rcgw))

### Changed
* Disable user defined app start config on Windows ([#4132](https://github.com/cryptomator/cryptomator/issues/4132))
* Disable plugin loading by default ([#4136](https://github.com/cryptomator/cryptomator/4136))
* Use JDK 25 ([#4031](https://github.com/cryptomator/cryptomator/pull/4031))
* Update JavaFX to 25.0.2 ([#4145](https://github.com/cryptomator/cryptomator/pull/4145))
* Updated translations
* Updated dependencies
  * `ch.qos.logback:*` from 1.5.19 to 1.5.32
  * `com.fasterxml.jackson.core:jackson-databind` from 2.20.0 to 2.21.1
  * `com.fasterxml.jackson.datatype:jackson-datatype-jsr310` from 2.20.0 to 2.21.1
  * `com.github.ben-manes.caffeine:caffeine` from 3.2.2 to 3.2.3
  * `com.google.dagger:*` from 2.57.2 to 2.59.2
  * `org.apache.commons:commons-lang3` from 3.19.0 to 3.20.0
  * `org.cryptomator:cryptofs` from 2.9.0 to 2.10.0
  * `org.cryptomator:cryptolib` from 2.2.1 to 2.2.2
  * `org.cryptomator:fuse-nio-adapter` from 5.1.0 to 6.0.1
  * `org.cryptomator:integrations-api` from 1.7.0 to 1.8.0-beta1
  * `org.cryptomator:integrations-linux` from 1.6.1 to 1.7.0-beta4
  * `org.cryptomator:integrations-mac` from 1.4.1 to 1.5.0-beta3
  * `org.cryptomator:integrations-win` from 1.5.1 to 1.6.0
  * `org.cryptomator:webdav-nio-adapter` from 3.0.0 to 3.0.1
  * `org.cryptomator:webdav-nio-adapter-servlet` to 1.2.12


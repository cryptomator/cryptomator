# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

The changelog starts with version 1.19.0.
Changes to prior versions can be found on the [Github release page](https://github.com/cryptomator/cryptomator/releases).

## [Unreleased](https://github.com/cryptomator/cryptomator/compare/1.18.0...HEAD)

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

### Changed
* Built using JDK 25 ([#4031](https://github.com/cryptomator/cryptomator/issues/4031))
* Modernized Template for GitHub Releases
* Disable user defined app start config on Windows ([#4132](https://github.com/cryptomator/cryptomator/issues/4132))
* Updated dependencies
  * `org.cryptomator:cryptolib` from 2.2.1 to 2.2.2
  * `org.cryptomator:cryptofs` from 2.9.0 to 2.10.0-beta3
  * `org.cryptomator:integrations-api` from 1.7.0 to 1.8.0-beta1
  * `org.cryptomator:integrations-linux` from 1.6.1 to 1.7.0-beta4
  * `org.cryptomator:integrations-mac` from 1.4.1 to 1.5.0-beta3
  * `org.cryptomator:fuse-nio-adapter` from 5.1.0 to 6.0.0
  * `ch.qos.logback:*` from 1.5.19 to 1.5.31
  * `org.apache.commons:commons-lang3` from 3.19.0 to 3.20.0
  * `com.fasterxml.jackson.core:jackson-databind` from 2.20.0 to 2.21.0
  * `com.fasterxml.jackson.datatype:jackson-datatype-jsr310` from 2.20.0 to 2.21.0
  * `com.google.dagger:*` from 2.57.2 to 2.59.1
  * `com.github.ben-manes.caffeine:caffeine` from 3.2.2 to 3.2.3



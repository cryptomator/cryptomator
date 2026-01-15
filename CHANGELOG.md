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
* Accessibility labels for GUI elements ([#4064](https://github.com/cryptomator/cryptomator/issues/4064), [#4066](https://github.com/cryptomator/cryptomator/pull/4066), [#4055](https://github.com/cryptomator/cryptomator/issues/4055))
* Show Archived Vault Dialog on unlock when Hub returns 410 ([#4081](https://github.com/cryptomator/cryptomator/pull/4081))
* Admin configuration: Allow overwriting certain app properties by external config file ([#4105](https://github.com/cryptomator/cryptomator/pull/4105))

### Changed
* Built using JDK 25 ([#4031](https://github.com/cryptomator/cryptomator/issues/4031))
* Modernized Template for GitHub Releases
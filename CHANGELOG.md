# Changelog

All notable changes to HardWire are documented here.

## [1.1.0] - Unreleased

### Security

- Removed hardcoded release signing credentials from the Gradle build.
- Release signing now reads `keystore.properties` locally or CI environment
  variables.
- The release workflow verifies the signed APK and publishes a SHA-256 checksum.
- Excluded generated ADB keys from Android backup and device transfer.
- Removed the external SoC lookup; all SoC resolution now happens locally.

### Changed

- Updated Gradle wrapper, Android Gradle Plugin, Kotlin, KSP, Compose, Room,
  Navigation Compose, core-ktx and appcompat.
- Added lint and release-build validation to CI.
- Added exported Room schemas and removed destructive migrations.
- Fixed network range calculation for non-/24 subnets and added TCP connect
  timeouts.
- Rewrote the README with accurate requirements, build, signing, privacy and
  troubleshooting documentation.
- Added third-party notices and restored the AdbLib BSD-3-Clause headers.

## [1.0.0] - 2026-07-31

### Added

- Local hardware inspection without ADB.
- USB and Wi-Fi ADB device inspection.
- CPU, GPU, memory, storage, battery, display, camera, sensor, network and
  system information.
- Local SoC name lookup.
- Device comparison, history snapshots and home screen widget.
- Spanish and English UI.

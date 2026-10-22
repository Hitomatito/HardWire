<div align="center">
  <img src="docs/hardwire-banner.svg" alt="HardWire — Android hardware inspector" width="100%" />
  <p>
    <a href="https://github.com/Hitomatito/HardWire/actions/workflows/ci.yml"><img src="https://github.com/Hitomatito/HardWire/actions/workflows/ci.yml/badge.svg" alt="CI status" /></a>
    <a href="https://github.com/Hitomatito/HardWire/releases/latest"><img src="https://img.shields.io/github/v/release/Hitomatito/HardWire?display_name=tag" alt="Latest release" /></a>
    <a href="LICENSE"><img src="https://img.shields.io/github/license/Hitomatito/HardWire" alt="MIT License" /></a>
    <img src="https://img.shields.io/badge/Android-7.0%2B-3ddc84" alt="Android 7.0+" />
    <img src="https://img.shields.io/badge/Kotlin-2.4-7f52ff" alt="Kotlin 2.4" />
  </p>
</div>

HardWire is an Android app for inspecting the hardware of the device it runs on,
or of remote Android devices connected through USB or Wi-Fi ADB.

It is built for developers, hardware engineers and enthusiasts who want a clear
view of the CPU, GPU, memory, storage, battery, display, cameras, sensors,
network and system software without juggling a dozen shell commands.

> HardWire is not affiliated with Google or the Android Open Source Project.

## Features

- **Local inspection** — read the hardware of the device running HardWire
  without ADB.
- **USB ADB** — connect to a remote device through USB OTG.
- **Wi-Fi ADB** — connect to a device exposing ADB on TCP port `5555`.
- **Ten hardware categories** — CPU, GPU, memory, storage, battery, display,
  cameras, sensors, network and system.
- **SoC name database** — local lookup for Qualcomm Snapdragon, MediaTek,
  Samsung Exynos, HiSilicon Kirin and Unisoc codenames.
- **Side-by-side comparison** — compare two saved devices.
- **History snapshots** — keep hardware reports over time.
- **Multi-device management** — connect and switch between several devices.
- **Home screen widget** — quick device status without opening the app.
- **Share and copy reports** — export a readable hardware report.
- **Themes and languages** — system/light/dark theme plus Spanish and English UI.

## Download

Get the latest APK from the
[releases page](https://github.com/Hitomatito/HardWire/releases/latest).

After downloading, allow installation from your browser or file manager if
Android asks for it. Verify the checksum published with the release if you want
to confirm the file was downloaded intact.

See [CHANGELOG.md](CHANGELOG.md) for release notes.

## Requirements

- Android 7.0 (API 24) or newer.
- For USB ADB: a USB OTG adapter and USB debugging enabled on the target.
- For Wi-Fi ADB: the target must expose ADB over TCP port `5555`, normally
  after running `adb tcpip 5555` while connected over USB.
- For building: JDK 21, Android SDK Platform 37 and an Android Studio version
  compatible with Android Gradle Plugin 9.4+.

> Android 11+ wireless debugging pairing is not implemented. Use USB to enable
> TCP mode, or connect to a device that already exposes port `5555`.

## Build

```bash
git clone https://github.com/Hitomatito/HardWire.git
cd HardWire
./gradlew assembleDebug
```

Run the checks before opening a pull request:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleRelease
```

The debug APK is written to `app/build/outputs/apk/debug/`.

## Release signing

Release builds read signing material from `keystore.properties` at the project
root or from environment variables. Never commit the keystore or its passwords.

```bash
cp keystore.properties.example keystore.properties
# edit keystore.properties with your local paths and passwords
./gradlew assembleRelease
```

The CI release workflow expects these repository secrets:

| Secret | Purpose |
|---|---|
| `KEYSTORE_BASE64` | Base64-encoded release keystore |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias inside the keystore |
| `KEY_PASSWORD` | Key password |

The workflow verifies the APK signature before publishing it, generates a
SHA-256 checksum and uploads both files to the GitHub release.

## Architecture

HardWire is a single-module Android app using:

- **MVVM** with `ViewModel` and `StateFlow`.
- **Jetpack Compose** and **Material 3** for the UI.
- **Navigation Compose** for screen navigation.
- **Room** for device and history persistence.
- **Kotlin Serialization** for report and device JSON.
- **Bundled AdbLib** for the ADB protocol implementation.
- **Manual service locator** (`di/ServiceLocator.kt`) instead of a DI framework.

```text
HardWire/
├── app/
│   ├── src/main/java/com/hitomatito/hardwire/
│   │   ├── adblib/          # Bundled ADB protocol implementation (BSD-3-Clause)
│   │   ├── data/            # Device collection, ADB, Room, history, export
│   │   ├── di/              # Service locator
│   │   └── ui/              # Compose screens, components, theme, widget
│   ├── src/main/res/        # Resources, strings, themes, widget layout
│   ├── src/test/            # Unit tests
│   ├── schemas/             # Exported Room schemas
│   └── build.gradle.kts
├── gradle/libs.versions.toml
├── docs/
├── THIRD_PARTY_NOTICES.md
└── LICENSE
```

## Tech stack

| Component | Version |
|---|---|
| Android Gradle Plugin | 9.4.1 |
| Gradle | 9.8.1 |
| Kotlin | 2.4.21 |
| Jetpack Compose BOM | 2026.09.00 |
| Room | 2.8.5 |
| Navigation Compose | 2.10.2 |
| compileSdk / targetSdk | 37 |
| minSdk | 24 |
| JDK | 21 |

## Permissions and privacy

HardWire asks only for permissions needed by its local and ADB features:

- `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` and
  `CHANGE_WIFI_STATE` for Wi-Fi ADB and local network discovery.
- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` for Wi-Fi discovery on
  Android versions that require location permission for Wi-Fi information.
- `NEARBY_WIFI_DEVICES` on Android 13+.
- USB host access for USB ADB.

HardWire does not include analytics, advertising or crash-reporting SDKs. Device
reports and history stay on the device. The SoC lookup is local; no device
codename is sent to an external service.

ADB keys generated by HardWire live in the app's private files directory and are
excluded from Android backup and device transfer.

> Wi-Fi ADB is not encrypted by the ADB protocol. Use it only on a trusted
> network, and disable wireless debugging when you are done.

## Testing

The unit test suite covers command parsing, device report parsing and hardware
detection logic. CI builds debug and release variants, runs unit tests and lint,
and uploads test and lint reports.

## Troubleshooting

### The device does not appear over USB

- Enable **Developer options → USB debugging** on the target.
- Accept the RSA authorization prompt on the target.
- Use a data-capable USB cable and an OTG adapter if needed.
- Revoke stale USB debugging authorizations and reconnect.

### Wi-Fi ADB cannot connect

- Confirm the phone and computer are on the same network.
- Run `adb tcpip 5555` over USB first, then connect to `<ip>:5555`.
- Check that no firewall blocks port `5555`.
- Android 11+ wireless debugging pairing is not supported by HardWire yet.

### Release build fails with a signing error

- Check that `keystore.properties` exists and points to a readable keystore.
- Or set `HARDWIRE_KEYSTORE_FILE`, `HARDWIRE_KEYSTORE_PASSWORD`,
  `HARDWIRE_KEY_ALIAS` and `HARDWIRE_KEY_PASSWORD`.
- Do not commit the keystore or the properties file.

## Third-party notices

The bundled ADB implementation is derived from
[cgutman/AdbLib](https://github.com/cgutman/AdbLib) and is distributed under the
BSD-3-Clause license. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for
the full notice and the other open-source components used by HardWire.

## License

HardWire is released under the [MIT License](LICENSE).

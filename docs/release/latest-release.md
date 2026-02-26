# Version

**3.2.0**

## Status:

**RELEASED**

## Release Notes:

### Added

- Integrate send transcript feature into the SDK & UI
- Implement Transaction Token Exchange for OAuth authentication flows

### Fixed

- Fix loading overlay not appearing and chat not reconnecting after network is restored
- Fix threading issue during captured media retrieval
- Fix audio permission request flow on Android 15
- Fix `RemoteLogger` not reporting upload failures to secondary logger instance
- Prevent loading of metadata for pending thread
- Align confirm button string to use "Submit" consistently across pre-chat survey, send transcript and list picker

### Changed

- Migrate secure storage from deprecated `EncryptedSharedPreferences` to Google Tink
- Update Coil 3.1.0 -> 3.3.0
- Add com.google.crypto.tink:tink-android 1.20.0 dependency for secure storage
- Bump androidx-lifecycle from 2.9.4 to 2.10.0
- Bump androidx-navigation and androidx.navigation.safeargs from 2.9.5 to 2.9.7
- Bump androidx-window from 1.5.0 to 1.5.1
- Bump androidx.activity:activity-compose from 1.11.0 to 1.12.3
- Bump androidx.compose:compose-bom from 2025.10.00 to 2026.01.01
- Bump androidx.datastore:datastore-preferences from 1.1.7 to 1.2.0
- Bump androidx.security:security-crypto from 1.0.0 to 1.1.0
- Bump com.airbnb.android:lottie-compose from 6.6.10 to 6.7.1
- Bump com.google.firebase:firebase-bom from 34.4.0 to 34.8.0
- Bump koin-annotations from 2.1.0 to 2.3.1
- Bump the kotlin-and-ksp group 2.2.10 -> 2.2.21
  - Bump com.google.devtools.ksp from 2.3.1 to 2.3.3 in the kotlin-and-ksp group
- Bump okhttp from 5.1.0 to 5.3.2
- Bump net.engawapg.lib:zoomable from 2.7.0 to 2.9.0

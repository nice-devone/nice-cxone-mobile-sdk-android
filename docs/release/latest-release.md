# Version

**3.3.0**

## Status:

**RELEASED**

## Release Notes:

### ✨ Added

- Prefer agent nickname in chat thread titles
- Add `TimePicker` message type support with UI, enabling time slot selection in chat interactions
- Add contextual loading indicators for attachments throughout the chat UI
- Add option to reset chat theme to default colors in UI settings
- Add End Conversation confirmation dialog requiring explicit user confirmation before closing the chat session

### 🔄 Changed

- Update navigation bar menu icons and action labels
- Bump `androidx.activity:activity-compose` from 1.12.3 to 1.12.4
- Bump `androidx.compose:compose-bom` from 2026.01.01 to 2026.02.01
- Bump `com.google.firebase:firebase-bom` from 34.8.0 to 34.10.0
- Bump `org.jetbrains:annotations` from 26.0.2-1 to 26.1.0

### 🐛 Fixed

- Fix crash in `RemoteLogger` when logging exceptions with fewer than 3 stack trace frames
- Fix thread safety issue in `DownloadUtil` causing race conditions during concurrent media downloads
- Fix `Send Transcript` action always visible in End Conversation dialog regardless of channel configuration
- Fix video preview loading spinner being skipped when Compose composition slot is reused
- Fix menu action box padding in navigation bar
- Fix loading overlay not shown and chat not reconnecting after network restore mid-session (regression introduced after 3.2.0 by the WebSocket exponential reconnect feature)
- Fix message read status dropped when `ReadChanged` event arrives before `MessageCreated` has populated the thread; read confirmation is now applied regardless of event ordering and cannot be downgraded by a subsequent `MessageCreated` event
- Fix double-tick read indicator disappearing when an incoming agent message shifts the customer message's position in the chat list

### 🔒 Security

- Enforce minimum `jose4j` 0.9.6 in buildscript resolutionStrategy to address CVE-2024-29371 (build-time only, no SDK artifact impact)
- Enforce minimum `io.netty` 4.1.129.Final in buildscript resolutionStrategy to address CVE-2025-67735 (build-time only, no SDK artifact impact)
- Enforce minimum `org.jdom:jdom2` 2.0.6.1 and `org.apache.commons:commons-lang3` 3.18.0 in buildscript resolutionStrategy to address build-time CVEs (build-time only, no SDK artifact impact)
- Upgrade `org.jetbrains.dokka` from 2.0.0 to 2.2.0 to resolve CVE-2025-52999, CVE-2025-49128, and CVE-2022-40152 (build-time only, no SDK artifact impact)

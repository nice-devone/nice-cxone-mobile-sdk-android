<a name="unreleased"></a>

## [Unreleased]

<a name="4.0.0"></a>

## [4.0.0] - 2026-08-04

### Added

- Enable deobfuscated Compose function names in stack traces for minified release builds
- AI authorship indicator: when AI-generated messages are present in a conversation, an "AI Assistant" badge appears in the top bar and AI authorship is announced to screen readers
- Enrich HTTP requests with a custom `User-Agent` header identifying the SDK (`cxone-chat-sdk/version`), app metadata (name, version, package, build), and device context (OS, model)
- First-class coroutine support — the SDK now exposes `Flow`-based observation APIs alongside the existing listener callbacks, and Java callers can opt into the new async surface via the provided interop wrappers:
    - `ChatThreadHandler.threadFlow: Flow<ChatThread>` — observe thread state updates reactively without registering a listener
    - `ChatThreadsHandler.threadsFlow: Flow<List<ChatThread>>` — observe thread list updates reactively without registering a listener
    - `ChatThreadActionHandler.popupFlow` and `ChatActionHandler.popupFlow` — observe popup action events as a `Flow`
    - Java interop helpers `ChatEventHandlerCoroutineWrapper`, `ChatThreadEventHandlerCoroutineWrapper`, and `ChatThreadMessageHandlerCoroutineWrapper` provide `*Async()` callback-based overloads for Java callers
- New `chat-sdk-core-java` artifact (Maven coordinate `com.nice.cxone:chat-sdk-core-java`) bundles all Java-facing async helpers — Java consumers should add it as a dependency to use callback-style APIs:
    - `ChatBuilderJavaInterop.getDefaultBlocking(...)` replaces the removed `ChatBuilder.getDefaultBlocking(...)` from `chat-sdk-core`
    - `ChatBuilderJavaInterop.getDefaultAsync(...)` and `buildAsync(...)` provide non-blocking factories with `Runnable`/`Consumer<Chat>`/`ErrorCallback` callbacks
    - `ChatJavaInterop.connectAsync(...)`, `getChannelAvailabilityAsync(...)`, `observeState(...)` wrap the suspend/Flow surface of `Chat`
    - `ChatThreadHandlerJavaInterop.archiveAsync(...)`, `observeThread(...)` wrap the suspend/Flow surface of `ChatThreadHandler`
    - `ChatInstanceProviderJavaInterop.closeSuspendingAsync(...)`, `signOutAsync(...)`/`signOutBlocking(...)`, and `configureAsync(...)`/`configureBlocking(...)` wrap the suspend surface of `ChatInstanceProvider` (`closeSuspending()`, `signOut()`, `configure(...)`) for Java callers
    - `ChatThreadsHandlerJavaInterop.observeThreads(...)` adapts `ChatThreadsHandler.threadsFlow` to a callback
    - `ChatActionHandlerJavaInterop.onPopup(...)` and `ChatThreadActionHandlerJavaInterop.onPopup(...)` adapt popup Flows to callbacks
    - All helpers return `Cancellable` for parity with the existing Java interop and never expose Kotlin coroutine types in their public signatures
- `chat-sdk-core-java` helpers and `*CoroutineWrapper` classes now accept an optional `java.util.concurrent.Executor callbackExecutor` parameter — when supplied, success and error callbacks are dispatched via the executor (e.g. `ContextCompat.getMainExecutor(context)` for Android UI updates). When omitted, callbacks fire on the SDK's internal IO thread.
- `chat-sdk-core-java` observer helpers (`observeState`, `observeThread`, `observeThreads`, `ChatActionHandlerJavaInterop.onPopup`, `ChatThreadActionHandlerJavaInterop.onPopup`) now accept an optional `ErrorCallback onError` parameter. When the underlying Flow terminates with an exception, the error is delivered to `onError` (CXoneException pass-through, other exceptions wrapped in InternalError) instead of escaping to the thread's uncaught-exception handler. When `onError` is null, upstream errors still escape so they remain visible to `Thread.UncaughtExceptionHandler` / logging infrastructure.
- `*CoroutineWrapper` async methods called after `close()` now synthesize an `InternalError("Wrapper has been closed; cannot perform async operation")` through `onError` instead of silently dropping the call. The `onError` is invoked synchronously on the calling thread for this case (no scope to dispatch on).

### Changed

- `ChatInstanceProvider` now rebuilds the `Chat` instance for each connected session: after `close()` (e.g. when the app goes to background), the next `connect()` transparently creates a fresh `Chat` and notifies `Listener.onChatChanged`. Do not cache the `Chat` instance across `close()` — always read `ChatInstanceProvider.chat` or observe `onChatChanged`. The channel configuration fetched during `prepare()` is reused for these rebuilds, so no additional network requests are made on reconnect.
- **BREAKING**: Project now uses Java 17 (up from 11 due to the Java 11 EOL) for source and target compatibility.
- **BREAKING**: Increase minSdk version 24 -> 26
- Increase targetSdk version 35 -> 36
- Increase compileSdk 36 -> 37
    - Required by the latest stable versions of androidx libraries (e.g., androidx.core:core-ktx:1.19.0)
- **BREAKING**:
    - **`Agent.fullName`**: return type changed from `String` to `String?`; it now returns `null` when neither name component was provided by the backend, replacing the previous empty-string result.
    - **`Agent.firstName`**, **`Agent.lastName`**, and **`Agent.imageUrl`**: return type changed from `String` to `String?`; `null` now indicates the field was not provided by the backend, as opposed to `""` which is an explicitly empty value.
    - **`Agent.isBotUser`** and **`Agent.isSurveyUser`**: return type changed from `Boolean` to `Boolean?`; `null` now indicates the agent's bot/survey status is unknown (not provided by the backend), which is distinct from `false` (definitively not a bot/survey agent).
- **BREAKING**:
    - **Time API**: All public time fields and parameters now use `kotlin.time.Instant` instead of `java.util.Date`. Affected API surface: `Message.createdAt`, `TimeSlot.startTime`, `MessageMetadata.seenAt`/`readAt`/`seenByCustomerAt`, `CustomField.updatedAt`, `Popup.InactivityPopup.Countdown.startedAt`, and the `instant` parameter on all `ChatEventHandlerActions` extension functions (renamed from `date`). Replace `Date()` with `kotlin.time.Clock.System.now()` and `Date(epochMs)` with `kotlin.time.Instant.fromEpochMilliseconds(epochMs)`.
- **BREAKING** Coroutine API migration — several public APIs have been migrated to `suspend` functions; callers must invoke them from a coroutine context. See the [Coroutine API migration guide](docs/migration/MIGRATION_COROUTINE_API.md) for the full callback → suspend/Flow mapping and Java interop. Java callers should add the new `com.nice.cxone:chat-sdk-core-java` dependency and use the helpers documented under **Added**:
    - `ChatBuilder.build()` is now a `suspend` function; call it from a coroutine or use `ChatBuilderJavaInterop.getDefaultBlocking()` for Java/blocking contexts
    - `Chat.connect()` is now a `suspend` function; call it from a coroutine
    - `ChatThreadMessageHandler.send()` is now a `suspend` function returning a `String` message ID; the previous callback-based overload and the `MessageSendResult` sealed interface have been removed
    - `ChatEventHandler.trigger()`, `ChatEventHandler.chatWindowOpen()`, and related event extensions are now `suspend` functions
    - `Chat.stateFlow` now returns `SharedFlow<ChatStateEvent>`, enforcing the replay-1 guarantee by the type system
    - `Chat.signOut()` is now a `suspend` function; call it from a coroutine
    - `ChatInstanceProvider.signOut()` and `ChatInstanceProvider.configure()` are now `suspend` functions; call them from a coroutine
    - `Chat.closeSuspending()` and `ChatInstanceProvider.closeSuspending()` added as the coroutine-native equivalent of `close()` — suspends until the same cleanup (storage/cookie persistence, socket teardown) completes instead of blocking the calling thread; prefer these over `close()` when a coroutine context is available
- **BREAKING** Java interop helpers extracted from `chat-sdk-core` to the new `com.nice.cxone:chat-sdk-core-java` artifact. Java consumers that use the `*CoroutineWrapper` classes or `ErrorCallback` must add `chat-sdk-core-java` as a dependency. Package names are unchanged (`com.nice.cxonechat.api`), so source imports do not change. Kotlin-only consumers can use the new suspend/Flow APIs directly and do not need the new dependency. As part of this extraction, the wrapper classes' constructors no longer accept a custom `CoroutineScope` parameter — they now accept an optional `java.util.concurrent.Executor callbackExecutor` instead, and otherwise manage their own internal coroutine scope.
- **BREAKING** `ChatBuilder.getDefaultBlocking(...)` removed from `chat-sdk-core`. Java consumers should replace calls with `ChatBuilderJavaInterop.getDefaultBlocking(...)` from `chat-sdk-core-java`. Kotlin consumers should call the suspend `ChatBuilder.getDefault(...)` directly.
- `chat-sdk-core` contains no internal `runBlocking` calls outside the `AutoCloseable` `close()` bridges — every `close()` implementation (`Chat`, its internal decorators, and `ChatInstanceProvider`) delegates to a suspend `closeSuspending()` via `runBlocking` so it can still support non-coroutine callers. Coroutine callers should call `closeSuspending()` directly instead. All other suspending work happens on caller-controlled coroutine contexts.
- Widen `com.nice.cxonechat.exceptions.InternalError` constructor visibility from `internal` to `public` so the moved `chat-sdk-core-java` interop helpers can wrap unexpected exceptions when forwarding to `ErrorCallback`. SDK consumers should not construct this exception type directly — its contract remains "report to CXone support".
- **BREAKING**  **`Environment` interface**: `authUrl` has been removed and replaced by `tokenUrl`. Both implementers of `Environment` (who must rename their `authUrl` override to `tokenUrl`) and callers that read `authUrl` on an `Environment` instance must migrate to `tokenUrl`.
- **BREAKING**  **`ChatThread.contactId`**: Exposed as a public abstract property; existing `ChatThread` subclasses must add `override val contactId: String?`.
- **BREAKING** Add implicit OAuth token delegation flow: integrators implement `TokenDelegateListener` and register it via `ChatBuilder.setTokenDelegateListener()`; the SDK calls `onNewTokenRequested()` with `UNSPECIFIED` on initial connect, `TOKEN_INVALID` when the backend rejects the stored token (HTTP 401), and `TOKEN_EXPIRED` when the token expires proactively during a session. The integrator's `onNewTokenRequested` implementation is expected to perform a **silent background refresh** (e.g. AppAuth `AuthState.performActionWithFreshTokens` using a cached refresh token) for `TOKEN_EXPIRED` / `TOKEN_INVALID` reasons — no browser UI should be shown to the user.
- **New exception types** — three new `RuntimeChatException` subclasses are now thrown and must be handled in your `ChatInstanceProvider.Listener.onChatRuntimeException` (or `ChatStateListener`) implementation:
    - `FeatureUnavailableException` — thrown when the explicit OAuth flow (`ChatBuilder.setAuthorization(...)`) is used but the backend returns an error during the transaction token exchange. Indicates the explicit flow is not functioning on the current backend version. Switch to the implicit flow via `setTokenDelegateListener(...)`.
    - `InvalidAccessTokenException` — thrown when the backend rejects the JWT used in the implicit flow and the SDK's automatic recovery also fails. Treat as a hard re-authentication signal.
    - `TokenDelegationFailedException` — thrown when `TokenDelegateListener.onNewTokenRequested` throws. The original cause is available via `Throwable.cause`.
- **Known issue**: Explicit OAuth flow (`ChatBuilder.setAuthorization(...)`) is currently not supported by the backend in the latest SDK version. Use implicit flow (`setTokenDelegateListener(...)`) for upgrades, or stay on your previous SDK version until explicit flow is supported.
- Bump `org.jetbrains.kotlin` from 2.2.21 to 2.4.0
- Bump `org.jetbrains.kotlinx:kotlinx-serialization-json` from 1.9.0 to 1.11.0
- Bump `org.jetbrains.kotlinx:kotlinx-coroutines-android` from 1.10.2 to 1.11.0
- Bump `io.insert-koin:koin-bom` from 4.1.1 to 4.2.2
- Bump `com.google.crypto.tink:tink-android` from 1.20.0 to 1.22.0
- Bump `androidx.core:core-ktx` from 1.17.0 to 1.19.0 (also bumps `androidx.core:core` and `androidx.annotation:annotation(-jvm)` transitively)
- Bump `androidx.compose:compose-bom` from 2026.02.01 to 2026.06.00
- Bump `androidx.activity:activity-compose` from 1.12.4 to 1.13.0
- Bump `androidx.media3` from 1.8.0 to 1.10.1
- Bump `androidx-lifecycle` from 2.10.0 to 2.11.0
- Bump `androidx.navigation` and `androidx.navigation.safeargs` from 2.9.7 to 2.9.8
- Bump `androidx.datastore:datastore-preferences` from 1.2.0 to 1.2.1
- Bump `io.coil-kt.coil3:coil-bom` from 3.3.0 to 3.4.0
- Bump `com.google.firebase:firebase-bom` from 34.10.0 to 34.15.0
- Bump `com.google.firebase.appdistribution` from 5.2.1 to 5.3.0
- Bump `com.google.gms.google-services` from 4.4.4 to 4.5.0
- Bump `com.squareup.okhttp3:okhttp` from 5.3.2 to 5.4.0
- Default SocketFactoryConfiguration implementation is now a data class with value equality,
  which may cause issues if users rely on previous reference only equality
- Published javadoc artifacts now use a single `-javadoc.jar` classifier per module instead of separate `-debug-javadoc.jar`/`-release-javadoc.jar` variants, and are no longer exposed as a Gradle Module Metadata `docstype=javadoc` variant; classifier-based javadoc resolution used by Maven, Gradle, and IDEs is unaffected

### Deprecated

- `Configuration.securedSessions` and `Configuration.Feature.SecuredSessions` — secured sessions are becoming mandatory for all channels. The flag will be removed in a future SDK release; integrators should stop reading it. The SDK will treat secured sessions as always enabled regardless of the channel configuration.

### Removed

- **BREAKING** `ChatThreadHandler.get(OnThreadUpdatedListener): Cancellable` removed — use `threadFlow` to observe thread state updates reactively
- **BREAKING** `ChatThreadHandler.OnThreadUpdatedListener` nested interface removed (deprecated since coroutine migration) — use `threadFlow` instead
- **BREAKING** `ChatThreadsHandler.threads(OnThreadsUpdatedListener): Cancellable` and `OnThreadsUpdatedListener` removed — use `threadsFlow` to observe thread list updates reactively
- **BREAKING** `ChatInstanceProvider.stateFlow` extension property removed — use `Chat.stateFlow` to observe chat state changes

### Fixed

- Chat header, thread list, and end-session screen now show a localizable "Agent" label when an agent is assigned but personal information is hidden by the backend
- Message avatars fall back correctly to initials (when name is visible) or the generic agent icon (when both name and image are hidden)
- Fix position-in-queue overlay remaining visible after Live Chat thread recovery when the assigned agent is only present in the `ownerAssignee` field
- Fix `ChatInstanceProvider` remaining stuck in `Connecting` state when `connect()` fails immediately (race between coroutine start and state transition)
- Fix `threadsFlow` in single-thread and live-chat modes not replaying the correct thread state to late subscribers after a recovery failure
- Fix `chat-sdk-ui` showing no feedback when the backend rejects the SDK version (`SdkNotSupported` state now shows a non-dismissible error dialog instead of silently leaving the user on a blank screen)
- Fix LiveChat getting stuck on the loading screen with no automatic reconnect after the initial connection attempt fails while the device is offline; the SDK now retries automatically with exponential backoff once connectivity is available again

### Security

- Enforce minimum `org.bouncycastle` 1.84 in buildscript resolutionStrategy to address CVE-2026-5598 / GHSA-p93r-85wp-75v3 (build-time only, no SDK artifact impact)
- Enforce minimum `io.netty` 4.1.135.Final in buildscript resolutionStrategy to address GHSA-3qp7-7mw8-wx86 (build-time only, no SDK artifact impact)
- Harden exported `ChatActivity` with `android:intentMatchingFlags="enforceIntentFilter"` for Android 16 (API 36) intent-matching compliance
- Enforce minimum `com.fasterxml.jackson` 2.18.9 (including `jackson-bom` and `jackson-databind`) in resolutionStrategy to address CVE-2026-54515 / GHSA-5jmj-h7xm-6q6v (build-time only, no SDK artifact impact)

<a name="3.3.1"></a>

## [3.3.1] - 2026-05-06

### Fixed

- Fix queue position indicator not updating during agent handover; visibility now relies solely on the reported position value

<a name="3.3.0"></a>

## [3.3.0] - 2026-04-30

### Added

- Prefer agent nickname in chat thread titles
- Add `TimePicker` message type support with UI, enabling time slot selection in chat interactions
- Add contextual loading indicators for attachments throughout the chat UI
- Add option to reset chat theme to default colors in UI settings
- Add End Conversation confirmation dialog requiring explicit user confirmation before closing the chat session

### Changed

- Update navigation bar menu icons and action labels
- Bump `androidx.activity:activity-compose` from 1.12.3 to 1.12.4
- Bump `androidx.compose:compose-bom` from 2026.01.01 to 2026.02.01
- Bump `com.google.firebase:firebase-bom` from 34.8.0 to 34.10.0
- Bump `org.jetbrains:annotations` from 26.0.2-1 to 26.1.0

### Fixed

- Fix crash in `RemoteLogger` when logging exceptions with fewer than 3 stack trace frames
- Fix thread safety issue in `DownloadUtil` causing race conditions during concurrent media downloads
- Fix `Send Transcript` action always visible in End Conversation dialog regardless of channel configuration
- Fix video preview loading spinner being skipped when Compose composition slot is reused
- Fix menu action box padding in navigation bar
- Fix loading overlay not shown and chat not reconnecting after network restore mid-session (regression introduced after 3.2.0 by the WebSocket exponential reconnect feature)
- Fix message read status dropped when `ReadChanged` event arrives before `MessageCreated` has populated the thread; read confirmation is now applied regardless of event ordering and cannot be downgraded by a subsequent `MessageCreated` event
- Fix double-tick read indicator disappearing when an incoming agent message shifts the customer message's position in the chat list

### Security

- Enforce minimum `jose4j` 0.9.6 in buildscript resolutionStrategy to address CVE-2024-29371 (build-time only, no SDK artifact impact)
- Enforce minimum `io.netty` 4.1.129.Final in buildscript resolutionStrategy to address CVE-2025-67735 (build-time only, no SDK artifact impact)
- Enforce minimum `org.jdom:jdom2` 2.0.6.1 and `org.apache.commons:commons-lang3` 3.18.0 in buildscript resolutionStrategy to address build-time CVEs (build-time only, no SDK artifact impact)
- Upgrade `org.jetbrains.dokka` from 2.0.0 to 2.2.0 to resolve CVE-2025-52999, CVE-2025-49128, and CVE-2022-40152 (build-time only, no SDK artifact impact)

<a name="3.2.3"></a>

## [3.2.3] - 2026-05-06

### Fixed

- Fix queue position indicator not updating during agent handover; visibility now relies solely on the reported position value

<a name="3.2.2"></a>

## [3.2.2] - 2026-04-23

### Fixed

- Respect the `liveChatAllowTranscript` channel configuration flag when showing the "Send Transcript" option in the End Conversation sheet

<a name="3.2.1"></a>

## [3.2.1] - 2026-03-31

### Fixed

- Prevent EndContact dialog from reappearing after dismissal in LiveChat mode

<a name="3.2.0"></a>

## [3.2.0] - 2026-02-26

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

<a name="3.1.2"></a>

## [3.1.2] - 2026-02-18

### Fixed

- Fix FileProvider authority conflict when multiple apps using the SDK are published on Google Play by making the authority dynamic based on
  application package name instead of hardcoded value
- Fix PdfRender thread-safety issue causing crashes during concurrent access
- Fix PdfRenderer resource leak during rapid scrolling and constructor failures by properly closing resources in all error paths and
  lifecycle events
- Fix opening of non-video/image attachments by adding URI read permission flag
- Fix issue introduced in version 3.0 where user entered text which is auto-linked is invisible on background
- Remove remnants of unused Foreground Service implementation to prevent clash with other integrations

<a name="3.1.1"></a>

## [3.1.1] - 2026-01-14

### Fixed

- Fix `OnThreadsUpdatedListener` listener is notified when a `ChatThreadState.Pending` thread is created locally
- Add missing subtitle to the Pre-chat survey screen & Edit Custom Values Screen
- Fix chat transcript layout when the position in queue counter is displayed
- Documentation error regarding UI module Theme customization
- Add missing Java helper methods for UI module `ThemeColorTokens` and subclasses
- Improve handling of slow SDK start when attachments are added
- Improve handling of slow SDK start when UI is resumed
- Hide "Add attachment" button based on channel configuration
- Fix missing permission request for camera attachment iff CAMERA permission is declared in the application manifest

<a name="3.1.0"></a>

## [3.1.0] - 2025-11-06

### Added

- `RemoteLogger` to report errors to the server and exposed in the public API ([US-SDK-001](docs/user-stories.md))
- Inactivity Popup Support ([US-SDK-002](docs/user-stories.md))
- `ChatThreadActionHandler` which can be obtained from `ChatThreadHandler`
- `Popup` which is provided to `OnPopup` listeners registered in the `ChatThreadActionHandler`
- `InactivityPopup` ([US-SDK-002](docs/user-stories.md))
- Utility extension method `ChatThreadEventHandler.triggerAction` to allow easy triggering of `Action` events (for Quick Replies, List
  Picker, Popups)
- Daily Perfecto build workflow for automated testing ([US-INFRA-001](docs/user-stories.md))
- Concurrency controls to PR workflows to cancel previous runs and save CI resources ([US-INFRA-002](docs/user-stories.md))
- UI tests support
- ChatInstanceProvider unit test coverage ([US-INFRA-004](docs/user-stories.md))
- Presurvey field validation check dynamically ([US-UI-011](docs/user-stories.md))
- Video and Image caching to improve loading performance ([US-PERF-003](docs/user-stories.md))
- Indicator for attachment preparing ([US-UI-012](docs/user-stories.md))
- WebSocket exponential reconnect with backoff strategy ([US-SDK-005](docs/user-stories.md))
- androidx.material3.adaptive dependency for adaptive UI layouts
- androidx.window and androidx.window-testing dependencies for window size class support

### Changed

- **BREAKING CHANGE**: Bump com.squareup.okhttp3:okhttp from 4.12.0 to 5.1.0
- Updated the thread list cell design for unread state ([US-UI-001](docs/user-stories.md))
- Updated UI for List Picker ([US-UI-001](docs/user-stories.md))
- Updated UI for Quick Replies ([US-UI-002](docs/user-stories.md))
- Updated UI for voice messages ([US-UI-003](docs/user-stories.md))
- Updated UI for attachments ([US-UI-004](docs/user-stories.md))
- Updated UI for typing indicator ([US-UI-005](docs/user-stories.md))
- Updated UI for Position in Queue ([US-UI-006](docs/user-stories.md))
- Updated UI for offline mode ([US-UI-007](docs/user-stories.md))
- Updated UI for Rich-Link ([US-UI-008](docs/user-stories.md))
- Updated UI for Basic Conversation ([US-UI-009](docs/user-stories.md))
- Updated UI for Accessibility ([US-UI-010](docs/user-stories.md))
- QuickReply message interaction behavior - options only visible for last message ([US-UI-013](docs/user-stories.md))
- UI module error handling with grouped error types ([US-UI-014](docs/user-stories.md))
- Increased compileSdkVersion to 36
- Update Android Gradle Plugin 8.11.1 -> 8.13.0
- Bump androidx.compose:compose-bom from 2025.06.01 to 2025.10.00
- Bump androidx-navigation from 2.9.3 to 2.9.5
- Bump androidx.navigation.safeargs from 2.9.3 to 2.9.5
- Bump androidx.activity:activity-compose from 1.10.1 to 1.11.0
- Bump androidx.emoji2:emoji2 from 1.5.0 to 1.6.0
- Bump androidx-lifecycle from 2.9.3 to 2.9.4
- Bump com.google.firebase:firebase-bom from 34.3.0 to 34.4.0
- Bump com.github.gmazzo.buildconfig from 5.6.8 to 5.7.0
- Bump com.google.gms.google-services from 4.4.3 to 4.4.4

### Fixed

- Handle failure & added auto retry mechanism during visitor creation/updation ([US-SDK-003](docs/user-stories.md))
- Filter unsupported message answers ([US-SDK-004](docs/user-stories.md))
- Presurvey invalid email and field validation ([US-UI-011](docs/user-stories.md))
- Server error reporting in single thread mode - ThreadRecoveryFailure errors now suppressed ([US-SDK-006](docs/user-stories.md))
- Single attachment preview - fixed size and design according to new specifications ([US-UI-015](docs/user-stories.md))
- Deeplink handling - delay Chat access until it is ready or in terminal state ([US-UI-016](docs/user-stories.md))
- Custom Fields dropdown - menu now opens properly and expands on clear ([US-UI-017](docs/user-stories.md))
- Memory leak in TemporaryFileStorage - using Application context instead of Activity ([US-UI-018](docs/user-stories.md))
- ExplicitGcViolation for Android 16 compatibility ([US-SDK-007](docs/user-stories.md))

### Security

- Audio message recording - validate audio file Uri returned by the ContentResolver before deleting it

<a name="3.0.0"></a>

## [3.0.0] - 2025-07-28

### Added

- Chat SDK UI module release
- androidx.compose:compose-bom
- androidx-navigation and androidx.navigation.safeargs
- androidx.activity:activity-compose
- androidx.constraintlayout:constraintlayout-compose dependency for constraint-based Compose layouts
- androidx.emoji2:emoji2
- androidx.media3 dependencies (datasource-okhttp, exoplayer, exoplayer-hls, ui) for media playback
- androidx-lifecycle
- com.google.firebase:firebase-bom
- com.google.gms.google-services
- net.engawapg.lib:zoomable dependency for image zooming functionality
- io.coil-kt.coil3 dependencies (coil-compose, coil-network-okhttp, coil-video) for image loading
- io.insert-koin dependencies (koin-bom, koin-android, koin-androidx-compose, koin-annotations, koin-ksp-compiler) for dependency injection
- org.jetbrains.kotlinx.binary-compatibility-validator plugin for API compatibility checking

### Changed

- **BREAKING CHANGE**: Renamed all classes with name like `CXOne` to `CXone`
- **BREAKING CHANGE**: Bump retrofit from 2.11.0 to 3.0.0
- Redesigned pre-contact survey
- Allow to name pending thread
- Bump androidx.datastore:datastore-preferences from 1.1.1 to 1.1.7
- Bump com.github.gmazzo.buildconfig from 5.5.4 to 5.6.6
- Bump androidx.core:core-ktx from 1.15.0 to 1.16.0
- Bump org.jetbrains:annotations from 26.0.1 to 26.0.2
- Bump org.jetbrains.kotlinx:kotlinx-serialization-json from 1.7.3 to 1.8.1
- Update Kotlin 2.0.21 -> 2.1.21

### Removed

- All deprecated methods and classes

### Fixed

- ChatInstanceProvider - fix concurrency for state updates
- Block multiple archival requests

<a name="2.3.0"></a>

## [2.3.0] - 2025-03-03

### Added

- Progress dialog for initialization in single thread mode
- com.github.gmazzo.buildconfig plugin for build configuration generation

### Changed

- Raise targetSdk 34 -> 35
- Update query parameters for web socket request
- Redesign message group header
- Redesign pre-contact survey
- Bump androidx.core:core-ktx from 1.13.1 to 1.15.0
- Bump kotlin from 2.0.10 to 2.0.21
- Bump org.jetbrains:annotations from 24.1.0 to 25.0.0
- Bump org.jetbrains.kotlinx:kotlinx-serialization-json from 1.7.2 to 1.7.3

### Fixed

- Welcome message improvements
    - In Messaging mode Welcome message is initially provided as placeholder
    - Welcome Message in LiveChat mode is using different logic
    - Improved support for late handling of a WelcomeMessage ProActive event
- Custom fields handling
    - Remove check which has allowed only the pre-chat survey fields to be supplied during thread creation to align with iOS platform
    - Don't send event about contact custom field change if the thread is in the pending or closed state
- Reconnect issues
    - Events are properly delayed on reconnect
- Allow SDK to reconnect in Offline state
- The `ChatWindowOpen` event is delayed pending authorization
- ProGuard/R8 fixes
    - Split out create method from all invoke operators
    - Added rules to the internal SDK minification

<a name="2.2.2"></a>

## [2.2.2]

### Fixed

- Livechat mode won't recover messages from closed case
- In livechat mode the queue position will remain `null` as long as the agent is assigned to the case
- SDK will supply Message object `createdAt` date with millisecond precision if possible
- Fix missing `ChatThreadState.Closed` enum entry in api.txt

<a name="2.2.1"></a>

## [2.2.1]

### Fixed

- Default `invoke()` operators in public API don't use `@JvmName` annotation to avoid minification issues in ProGuard/R8
    - The API was extended with methods which have the same signature as was previously covered by `invoke()` operators.
      This should prevent binary compatibility issues.
- Fix missing contactId for live chat thread
    - This fixes issue where it wasn't possible to end the live chat session in certain scenarios

<a name="2.2.0"></a>

## [2.2.0]

### Added

- Enable application LargeHeap for attachment upload
    - This allows upload of larger attachments on devices with sufficient RAM
- Enhance AuthorizeCustomer event
- Mobile SDK sends new headers for internal analytics
- Update agent model
    - Added nickName field
    - imageUrl field now provides the public image url

### Changed

- **BREAKING CHANGE**: Update kotlin from 1.9.24 to 2.0.10
- Replace GSON with kotlinx.serialization

### Deprecated

- inContactId and emailAddress from Agent
    - Values for these fields are now always empty

### Removed

- Unused dependencies from the utilities module

### Fixed

- Allow reconnect in offline state
- ProGuard/R8 issues
    - Updated consumer-rules.pro to prevent minification of several problematic methods
    - Added rules to the internal SDK minification
- SDK awaits for authorization
    - ℹ️ Event sending may be delayed until server confirms user authorization to use Chat service, sending of events prior to this could
      lead to loss of such events.

<a name="2.1.1"></a>

## [2.1.1]

### Fixed

- CaseStatusChanged with status closed doesn't archive thread in SingleThread mode

<a name="2.1.0"></a>

## [2.1.0] - 2024-07-18

### Added

- Support for autolinking in text messages
- Support ISO 8601 time zone in datetime values
- Allow to set customerId
- Prevent duplicate welcome message
- coordinate ArchiveThread and ThreadArchived events

### Changed

- **BREAKING CHANGE**: Increased minSDK to 24
- Live chat documentation update
- Bump Kotlin 1.9.22 -> 1.9.24
- Bump retrofit from 2.10.0 to 2.11.0
- Bump com.google.code.gson:gson from 2.10.1 to 2.11.0

### Removed

- Custom field validation
- sarif conversion as it isn't supported without special permission

<a name="2.0.0"></a>

## [2.0.0] - 2024-06-18

### Added

- Missing support for wildcard filetype restrictions
- LiveChat support
- LiveChat creates thread on connect if needed
- Support for autolinking in text messages
- Parsing and publication of file upload restrictions
- SDK enforces file restrictions
- Handle EventInS3 meta-event

### Changed

- **BREAKING CHANGE**: Enum case name consistency
- Align Single-threaded Channel Behavior
- Update DeviceFingerprint
- Sdk doesn't allow messaging to archived thread
- Bump androidx.core:core-ktx from 1.13.0 to 1.13.1
- Update AGP 8.2.2 to 8.3.1
- Update com.squareup.retrofit2 2.9.0 -> 2.10.0

### Deprecated

- Legacy plugins

### Removed

- [@Deprecated](https://github.com/Deprecated) Plugin support

### Fixed

- Fix looping now that caseContactFields is removed
- Deliver thread updates to all listeners
- Fix LiveChat restart
- Validate allowed file mimetype specification

<a name="1.3.1"></a>

## [1.3.1] - 2024-05-16

### Fixed

- Fixes for 1.3.1

<a name="1.3.0"></a>

## [1.3.0] - 2024-02-28

### Added

- ProxyLogger constructor with vararg param
- seenAt and inferred state to message metadata
- logger-android module
- Extract logging library module
- Option to specify Logger for the SDK
- Implement CaseStatusChanged event

### Changed

- **BREAKING CHANGE**: Replace dagger/hilt with Koin in UI and sample application components
- **BREAKING CHANGE**: If uploaded filename has no extension, get one using MimeTypeMap
- Set CustomerId type to String
- Update Chat to separate prepare and connect actions
- Display sender name and read/received status
- Update Agent.isTyping only when agent is typing
- Sockets created by SDK are tagged for TrafficStats
- Delay SharedPreferences initialization
- Improve Java compatibility
- Raise project compileSdk 33 -> 34
- Improve logging of outgoing events
- Pass server reported errors to integration
- Process events on background thread
- Allow to change users name
- Message is updated when read by agent
- Correct welcome message handling
- Update Kotlin 1.9.20 -> 1.9.21
- Bump com.squareup.okhttp3:okhttp from 4.11.0 to 4.12.0
- Bump androidx.core:core-ktx from 1.10.1 to 1.12.0
- Update Kotlin 1.8.21 -> 1.9.10

### Fixed

- Consolidate differing kotlin tool versions on 1.9.21
- Make date formatting thread safe
- Use referential equality for enum comparison
- Cancel start job in case of re-configuration
- TreeField/CVHierarchicalField to not skip every other level
- Remove messages with duplicate id from thread
- Crash when restore suspended login dialog
- Unreliable unit tests depending on makeMessageModel being sequential
- Display of new agent messages
- Disable sending empty messages
- Return error when receiving "invalid" server response on image upload
- Crashes on some Qualcomm/Samsung devices

<a name="1.2.1"></a>

## [1.2.1] - 2024-01-10

### Added

- Improve Java compatibility for 1.2.1

### Fixed

- Fix minification issues with ChatInstanceProvider

<a name="1.2.0"></a>

## [1.2.0] - 2023-09-26

### Added

- New chat-sdk-ui library from the prior sample application
- Support for timeSpentOnPage
- Support updated RecoveredThread event
- ChatInstanceProvider
- Support for customizable brand logo
- Conversion events in sample application

### Changed

- **BREAKING CHANGE**: Convert unnecessary abstract classes to interfaces
- Use events endpoint for analytics events
- Merge SampleApplication project to the SDK project
- Internally mark archived threads as archived pending success/failure from server
- Convert ListPicker message to Compose
- Convert thread list screen to JetPack Compose
- Compose Chat UI
- Integrate new Chat UI module with Store application
- Allow colorization of chat sdk screens
- Bump androidx.core:core-ktx from 1.10.0 to 1.10.1
- Bump com.squareup.okhttp3:okhttp from 4.10.0 to 4.11.0

### Fixed

- Reenable tests depending on android.util.Patterns
- Successful thread-archived event will trigger thread list refresh
- Persist deviceToken until the value is updated
- StoreVisitor upload failure message - server message part
- Create/update Visitor using new endpoint
- Issue with color settings fields not reflecting day/night
- Improve handling of notifications
- Allow empty/non-existent TEXT in QuickReply plugins
- Prepopulate configuration menu with last known custom values
- Thread update flow and remove extraneous refresh requests
- Misc errors around state changes
- Remove messages with duplicate id from thread

<a name="1.1.0"></a>

## [1.1.0] - 2023-06-26

### Bug Fixes

- Update thread agent correctly
- Allow application to respond to the websocket session state & reconnect
- Use OkHttp for WebSocket communication
- Add missing consumer ProGuard rule for GSON classes
- Modify Proguard/R8 rules to be compatible with R8 fullMode
- Successful thread-archived event will trigger thread list refresh
- Fix threading issue introduced with postback support
- Allow lenient parsing of UUID type

### Dependency Change

- Bump androidx.core:core-ktx from 1.9.0 to 1.10.0
- Bump org.jetbrains.dokka from 1.6.10 to 1.8.10
- Bump org.jetbrains.kotlin.android from 1.6.10 to 1.8.10

### Features

- implement/fix LoadThreadMetadata and add loadMetadata entry to ChatThreadEventHandlerActions
- Dynamic pre contact survey
- Documentation update
- create ContentDescriptor.DataSource to facilitate minimizing memory usage
- Modify Custom Fields storing behavior
- Increase targetSdk 31 -> 33
- add support for QUICK_REPLIES message types
- implement RichLink message types
- implement ListPicker message type
- Add an option to send postback value in a thread message
- Parse and utilize custom field definitions from the server.
- Fresh Naming in Websocket events
- Fresh Naming in Websocket events

### Reverts

- [chore] Bump androidx.core:core-ktx from 1.9.0 to 1.10.0

<a name="1.0.1"></a>

## [1.0.1] - 2023-03-07

### Bug Fixes

- Use OkHttp for WebSocket communication
- Add missing consumer ProGuard rule for GSON classes

<a name="1.0.0"></a>

## 1.0.0 - 2023-02-14

### Features

- Logging PoC
- Error handling
- Connection
    - Ping to ensure connection state
    - Execute trigger manually
    - Handle unexpected disconnect
- Customer
    - Save customer credentials
    - Customer authorisation
    - Customer reconnect
    - OAuth
- Customer Custom Fields
    - Save customer custom fields
- Threads
    - Update thread name
    - „Read“ flag
    - „Delivered“ flag
    - Threads load
    - Contact inbox assignee change
    - Recover thread
    - Typing indicator
    - Archive thread
    - Load thread metadata
    - Handle proactive action
        - Welcome message
        - Custom popup box
- Contact Custom Fields
    - Save contact custom fields
- Messages
    - Send/Receive attachments
        - Image
        - Video
        - Documents
    - Handle a message
        - Text
        - Plugin
            - Gallery
            - Menu
            - Text and Buttons
            - Quick Replies
            - Satisfaction Survey
            - Custom
            - Sub Elements
                - Text
                - Title
                - File
                - Button/iFrame Button
    - Previous message load
- Analytics
    - Page view
    - Chat window open
    - App visit
    - Conversion
    - Custom visitor event
    - Proactive action
        - display
        - success
        - failure
    - typing start/end

[Unreleased]: https://github.com/BrandEmbassy/cxone-mobile-sdk-android/compare/4.0.0...HEAD

[4.0.0]: https://github.com/BrandEmbassy/cxone-mobile-sdk-android/compare/3.3.1...4.0.0

[3.3.1]: https://github.com/BrandEmbassy/cxone-mobile-sdk-android/compare/3.3.0...3.3.1

[3.3.0]: https://github.com/BrandEmbassy/cxone-mobile-sdk-android/compare/3.2.2...3.3.0

[3.2.3]: https://github.com/BrandEmbassy/cxone-mobile-sdk-android/compare/3.2.2...3.2.3

[3.2.2]: https://github.com/BrandEmbassy/cxone-mobile-sdk-android/compare/3.2.1...3.2.2

[3.2.1]: https://github.com/BrandEmbassy/cxone-mobile-sdk-android/compare/3.2.0...3.2.1

[3.2.0]: https://github.com/BrandEmbassy/cxone-mobile-sdk-android/compare/3.1.2...3.2.0

[3.1.2]: https://github.com/BrandEmbassy/cxone-mobile-sdk-android/compare/3.1.1...3.1.2

[3.1.1]: https://github.com/BrandEmbassy/cxone-mobile-sdk-android/compare/3.1.0...3.1.1

[3.1.0]: https://github.com/BrandEmbassy/cxone-mobile-sdk-android/compare/3.0.0...3.1.0

[3.0.0]: https://github.com/BrandEmbassy/cxone-mobile-sdk-android/compare/2.2.2...3.0.0

[2.2.2]: https://github.com/BrandEmbassy/cxone-mobile-sdk-android/compare/2.2.1...2.2.2

[2.2.1]: https://github.com/BrandEmbassy/cxone-mobile-sdk-android/compare/2.2.0...2.2.1

[2.2.0]: https://github.com/BrandEmbassy/cxone-mobile-sdk-android/compare/2.1.1...2.2.0

[2.1.1]: https://github.com/BrandEmbassy/cxone-mobile-sdk-android/compare/2.1.0...2.1.1

[2.1.0]: https://github.com/BrandEmbassy/cxone-mobile-sdk-android/compare/2.0.0...2.1.0

[2.0.0]: https://github.com/BrandEmbassy/cxone-mobile-sdk-android/compare/1.3.1...2.0.0

[1.3.1]: https://github.com/BrandEmbassy/cxone-mobile-sdk-android/compare/1.3.0...1.3.1

[1.3.0]: https://github.com/BrandEmbassy/cxone-mobile-sdk-android/compare/1.2.1...1.3.0

[1.2.1]: https://github.com/BrandEmbassy/cxone-mobile-sdk-android/compare/1.2.0...1.2.1

[1.2.0]: https://github.com/BrandEmbassy/cxone-mobile-sdk-android/compare/1.1.0...1.2.0

[1.1.0]: https://github.com/BrandEmbassy/cxone-mobile-sdk-android/compare/1.0.1...1.1.0

[1.0.1]: https://github.com/BrandEmbassy/cxone-mobile-sdk-android/compare/1.0.0...1.0.1

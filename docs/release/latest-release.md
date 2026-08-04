# Version

**4.0.0**

## Status:

**RELEASED**

## Release Notes:

### 💥 Breaking Changes

- **Java 17 required**: source/target compatibility raised from Java 11 (EOL) to Java 17.
- **minSdk raised 24 → 26**: apps targeting API 24/25 can no longer integrate this SDK version.
- **Coroutine API migration**: `ChatBuilder.build()`, `Chat.connect()`, `ChatThreadMessageHandler.send()`, `ChatEventHandler.trigger()`/`chatWindowOpen()` and related extensions are now `suspend` functions; `Chat.stateFlow` now returns `SharedFlow<ChatStateEvent>`. See the [Coroutine API migration guide](../migration/MIGRATION_COROUTINE_API.md).
- **Java interop extracted**: `*CoroutineWrapper` classes and `ErrorCallback` moved from `chat-sdk-core` into the new `com.nice.cxone:chat-sdk-core-java` artifact — Java consumers must add this dependency. Wrapper constructors no longer accept a custom `CoroutineScope`; they take an optional `Executor callbackExecutor` instead.
- **`ChatBuilder.getDefaultBlocking(...)` removed** from `chat-sdk-core` — Java consumers use `ChatBuilderJavaInterop.getDefaultBlocking(...)` from `chat-sdk-core-java`; Kotlin consumers call the suspend `ChatBuilder.getDefault(...)` directly.
- **Listener APIs removed** in favor of `Flow`-based observation: `ChatThreadHandler.get(OnThreadUpdatedListener)`/`OnThreadUpdatedListener` (use `threadFlow`), `ChatThreadsHandler.threads(OnThreadsUpdatedListener)`/`OnThreadsUpdatedListener` (use `threadsFlow`), `ChatInstanceProvider.stateFlow` extension (use `Chat.stateFlow`).
- **`Agent` nullability changes**: `fullName`, `firstName`, `lastName`, `imageUrl` now return `String?` (`null` replaces the previous empty-string result); `isBotUser`/`isSurveyUser` now return `Boolean?` (`null` means "unknown", distinct from `false`).
- **`Environment` interface**: `authUrl` has been removed and replaced by `tokenUrl`. Both implementers and callers must migrate to `tokenUrl`.
- **Time API**: All public time fields and parameters now use `kotlin.time.Instant` instead of `java.util.Date`. Affected: `Message.createdAt`, `TimeSlot.startTime`, `MessageMetadata.seenAt`/`readAt`/`seenByCustomerAt`, `CustomField.updatedAt`, `Popup.InactivityPopup.Countdown.startedAt`, and `instant` parameters on `ChatEventHandlerActions` extension functions (renamed from `date`). Replace `Date()` with `kotlin.time.Clock.System.now()` and `Date(epochMs)` with `kotlin.time.Instant.fromEpochMilliseconds(epochMs)`.
- **`ChatThread.contactId`**: Exposed as a public abstract property; existing `ChatThread` subclasses must add `override val contactId: String?`.
- **Implicit OAuth token delegation flow added**: integrators implement `TokenDelegateListener` and register it via `ChatBuilder.setTokenDelegateListener()`; the SDK calls `onNewTokenRequested()` with `UNSPECIFIED` on initial connect, `TOKEN_INVALID` when the backend rejects the stored token (HTTP 401), and `TOKEN_EXPIRED` when the token expires proactively during a session. The integrator's `onNewTokenRequested` implementation is expected to perform a **silent background refresh** (e.g. AppAuth `AuthState.performActionWithFreshTokens` using a cached refresh token) for `TOKEN_EXPIRED` / `TOKEN_INVALID` reasons — no browser UI should be shown to the user.

### ✨ Added

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

### 🔄 Changed

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
- **BREAKING** Coroutine API migration — several public APIs have been migrated to `suspend` functions; callers must invoke them from a coroutine context. See the [Coroutine API migration guide](../migration/MIGRATION_COROUTINE_API.md) for the full callback → suspend/Flow mapping and Java interop. Java callers should add the new `com.nice.cxone:chat-sdk-core-java` dependency and use the helpers documented under **Added**:
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

### ⚠️ Deprecated

- `Configuration.securedSessions` and `Configuration.Feature.SecuredSessions` — secured sessions are becoming mandatory for all channels. The flag will be removed in a future SDK release; integrators should stop reading it. The SDK will treat secured sessions as always enabled regardless of the channel configuration.

### 🗑️ Removed

- **BREAKING** `ChatThreadHandler.get(OnThreadUpdatedListener): Cancellable` removed — use `threadFlow` to observe thread state updates reactively
- **BREAKING** `ChatThreadHandler.OnThreadUpdatedListener` nested interface removed (deprecated since coroutine migration) — use `threadFlow` instead
- **BREAKING** `ChatThreadsHandler.threads(OnThreadsUpdatedListener): Cancellable` and `OnThreadsUpdatedListener` removed — use `threadsFlow` to observe thread list updates reactively
- **BREAKING** `ChatInstanceProvider.stateFlow` extension property removed — use `Chat.stateFlow` to observe chat state changes

### 🐛 Fixed

- Chat header, thread list, and end-session screen now show a localizable "Agent" label when an agent is assigned but personal information is hidden by the backend
- Message avatars fall back correctly to initials (when name is visible) or the generic agent icon (when both name and image are hidden)
- Fix position-in-queue overlay remaining visible after Live Chat thread recovery when the assigned agent is only present in the `ownerAssignee` field
- Fix `ChatInstanceProvider` remaining stuck in `Connecting` state when `connect()` fails immediately (race between coroutine start and state transition)
- Fix `threadsFlow` in single-thread and live-chat modes not replaying the correct thread state to late subscribers after a recovery failure
- Fix `chat-sdk-ui` showing no feedback when the backend rejects the SDK version (`SdkNotSupported` state now shows a non-dismissible error dialog instead of silently leaving the user on a blank screen)
- Fix LiveChat getting stuck on the loading screen with no automatic reconnect after the initial connection attempt fails while the device is offline; the SDK now retries automatically with exponential backoff once connectivity is available again

### 🔒 Security

- Enforce minimum `org.bouncycastle` 1.84 in buildscript resolutionStrategy to address CVE-2026-5598 / GHSA-p93r-85wp-75v3 (build-time only, no SDK artifact impact)
- Enforce minimum `io.netty` 4.1.135.Final in buildscript resolutionStrategy to address GHSA-3qp7-7mw8-wx86 (build-time only, no SDK artifact impact)
- Harden exported `ChatActivity` with `android:intentMatchingFlags="enforceIntentFilter"` for Android 16 (API 36) intent-matching compliance
- Enforce minimum `com.fasterxml.jackson` 2.18.9 (including `jackson-bom` and `jackson-databind`) in resolutionStrategy to address CVE-2026-54515 / GHSA-5jmj-h7xm-6q6v (build-time only, no SDK artifact impact)

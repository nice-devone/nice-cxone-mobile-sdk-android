# Migration Guide: OAuth Flow Changes

## Overview

This release introduces a significant redesign of third-party OAuth authentication in the CXone Chat SDK.
The previous single-path flow (authorization code + code verifier set on `ChatBuilder`) has been expanded
into two distinct, first-class flows:

| Flow | Old name | New name | API entry point |
|------|----------|----------|-----------------|
| Authorization-code + PKCE | (only flow) | **Explicit OAuth** | `ChatBuilder.setAuthorization()` (unchanged signature) |
| Integrator-managed JWT | (not supported) | **Implicit OAuth** | `ChatBuilder.setTokenDelegateListener()` (**new**) |

The explicit flow retains the same `ChatBuilder.setAuthorization()` API, so **existing integrations
continue to compile without changes**. However, several internal types and events have been removed,
new exception types have been added, and token storage has changed. Read the sections below for the
complete list of breaking changes.

---

## Table of Contents

1. [Breaking Changes](#breaking-changes)
2. [New APIs](#new-apis)
3. [Removed APIs](#removed-apis)
4. [Migration: Explicit Flow (existing integrations)](#migration-explicit-flow-existing-integrations)
5. [Migration: Adding the Implicit Flow](#migration-adding-the-implicit-flow)
6. [Dependency Changes](#dependency-changes)
7. [Sample App Changes](#sample-app-changes)
8. [Verification Checklist](#verification-checklist)

---

## Breaking Changes

### 1. `RefreshToken` event removed

`chat.events().refresh(RefreshToken())` no longer exists. Token refresh is now managed entirely by the
SDK via the `TokenDelegateListener` (implicit flow) or by re-supplying credentials
(explicit flow). Remove any calls to `RefreshToken` from your code.

```kotlin
// BEFORE — remove this:
chat.events().refresh(RefreshToken())

// AFTER — no replacement needed for the explicit flow.
// For the implicit flow, the SDK calls TokenDelegateListener automatically.
```

### 2. New exception types — update your `onChatRuntimeException` handler

New `RuntimeChatException` subclasses have been added. Your `ChatInstanceProvider.Listener` (or
`ChatStateListener`) implementation must handle them:

| New exception | When thrown |
|---------------|-------------|
| `RuntimeChatException.InvalidAccessTokenException` | Backend rejected the JWT used in the implicit flow; SDK could not automatically recover |
| `RuntimeChatException.InvalidRefreshTokenException` | Backend rejected the `refresh_token` used in the explicit flow while the SDK tried to exchange it for a new access token; no fallback remains |
| `RuntimeChatException.TokenDelegationFailedException` | `TokenDelegateListener.onNewTokenRequested` threw an exception |

**Update your exception handler:**

```kotlin
// BEFORE:
override fun onChatRuntimeException(exception: RuntimeChatException) {
    if (exception is RuntimeChatException.ConnectionTokenFailed) {
        // re-authenticate
    } else {
        logError(exception)
    }
}

// AFTER:
override fun onChatRuntimeException(exception: RuntimeChatException) {
    when (exception) {
        is RuntimeChatException.ConnectionTokenFailed -> {
            // Explicit flow: SDK could not recover the session in the background (e.g. no
            // refresh_token available) — re-run OAuth
            triggerExplicitReAuthentication()
        }
        is RuntimeChatException.InvalidAccessTokenException -> {
            // Implicit flow: JWT rejected by backend and silent refresh also failed
            triggerImplicitReAuthentication()
        }
        is RuntimeChatException.InvalidRefreshTokenException -> {
            // Explicit flow: backend rejected the refresh_token — no fallback remains, re-run OAuth
            triggerExplicitReAuthentication()
        }
        is RuntimeChatException.TokenDelegationFailedException -> {
            // Implicit flow: TokenDelegateListener threw — show error or re-login
            triggerImplicitReAuthentication()
        }
        else -> logError(exception)
    }
}
```

### 3. `ErrorType` — OAuth-related error codes removed

Several internal `ErrorType` enum entries have been removed. If you were matching on raw error type
strings, update to use the structured `RuntimeChatException` subclasses above.

### 4. `EventType` — OAuth-related event types removed

The `EventType` entries for `CustomerAuthorized`, `TokenRefreshed`, and related socket events have been
removed from the public `EventType` enum. These were never intended for direct use by integrators.

### 5. `ChatInstanceProvider.AuthenticationScope` — new `tokenDelegateListener` property

`ChatInstanceProvider.AuthenticationScope` (used inside `configure { }` blocks) has a new property:

```kotlin
var tokenDelegateListener: TokenDelegateListener?
```

If you subclass or implement `AuthenticationScope` (uncommon), add this property.

### 6. `Configuration.securedSessions` deprecated

`Configuration.securedSessions` and `Configuration.Feature.SecuredSessions` are now `@Deprecated`.
Secured sessions are becoming mandatory. Remove any checks against this flag:

```kotlin
// BEFORE — remove this check:
if (chat.configuration.securedSessions) { ... }

// AFTER — secured sessions are always enabled; no check needed.
```

---

## New APIs

### `OAuthToken`

```kotlin
@Public
interface OAuthToken {
    val accessToken: String
    val expiryDateMillis: Long?   // null = treat as non-expiring by time

    companion object {
        operator fun invoke(accessToken: String, expiryDateMillis: Long? = null): OAuthToken
    }
}
```

Returned from `TokenDelegateListener.onNewTokenRequested`. The `expiryDateMillis` field enables
the SDK to schedule proactive token refresh before the backend rejects an expired JWT.

### `TokenDelegateListener`

```kotlin
@Public
fun interface TokenDelegateListener {
    @Throws(RuntimeChatException.TokenDelegationFailedException::class)
    fun onNewTokenRequested(reason: TokenRequestReason): OAuthToken
}
```

A SAM functional interface. Register it via `ChatBuilder.setTokenDelegateListener()` to opt in to the
implicit flow. Called on a background thread; the implementation **must block** until the token is ready.

### `TokenRequestReason`

```kotlin
@Public
enum class TokenRequestReason {
    UNSPECIFIED,     // Initial connect — return cached token if available
    TOKEN_INVALID,   // Backend returned 400 for the stored JWT
    TOKEN_EXPIRED,   // Proactive refresh before expiryDateMillis is reached
}
```

Passed to `TokenDelegateListener.onNewTokenRequested` to allow the implementation to differentiate
between an initial connect and a mid-session refresh.

### `ChatBuilder.setTokenDelegateListener()`

```kotlin
fun setTokenDelegateListener(listener: TokenDelegateListener): ChatBuilder
```

Registers the implicit flow token delegate. Has no effect in the explicit flow.

### `ChatInstanceProvider.setTokenDelegateListener()`

```kotlin
fun setTokenDelegateListener(listener: TokenDelegateListener?)
```

Sets or clears the delegate at runtime. Takes effect on the next `prepare()` call. Use
`configure { tokenDelegateListener = ... }` to apply immediately to a running session.

### `RuntimeChatException.InvalidAccessTokenException`

Reported when the backend rejects the JWT and the SDK's automatic recovery (calling the delegate again)
also fails. Treat as a hard re-authentication signal.

### `RuntimeChatException.InvalidRefreshTokenException`

Reported when the explicit flow's `refresh_token` is rejected by the backend while the SDK tries to
exchange it for a new access token (e.g. expired or revoked). Like `InvalidAccessTokenException`, this
is terminal — treat as a hard re-authentication signal requiring a new authorization code + verifier.

### `RuntimeChatException.TokenDelegationFailedException`

Reported when `onNewTokenRequested` throws. The original throwable that caused the failure is
available as `Throwable.cause`.

---

## Removed APIs

| Removed element | Replacement |
|-----------------|-------------|
| `ChatAuthorization` (internal) | `Authorization` interface (unchanged) |
| `DelayUnauthorizedEventHandler` (internal) | Removed — SDK handles internally |
| `ActionAuthorizeCustomer` (internal socket message) | Removed |
| `ActionReconnectCustomer` (internal socket message) | Removed |
| `ActionRefreshToken` (internal socket message) | Removed |
| `EventCustomerAuthorized` (internal) | Removed |
| `EventTokenRefreshed` (internal) | Removed |
| `event.RefreshToken` (public event) | Removed — SDK manages refresh automatically |
| `ErrorType` OAuth entries | Use `RuntimeChatException` subclasses |
| `EventType` OAuth entries | Removed |

---

## Migration: Explicit Flow (existing integrations)

If you were using the previous OAuth flow (authorization code + code verifier), your integration maps
directly to the new **explicit flow**. The `ChatBuilder.setAuthorization()` API signature is unchanged.

### What you must change

#### 1. Remove `RefreshToken` event usage

```kotlin
// Remove any occurrence of:
chat.events().refresh(RefreshToken())
```

#### 2. Update `onChatRuntimeException` to handle new exceptions

See [Breaking Changes §2](#2-new-exception-types--update-your-onchatruntimeexception-handler) above.

#### 3. Handle explicit-flow refresh fallback

For explicit flow, the SDK first attempts recovery in the background (no user interaction): if the
underlying OAuth access token is still valid it is reused directly; otherwise the SDK spends the stored
`refresh_token` to mint a new access token and uses that. Your app must re-run the full PKCE
authorization-code flow when neither recovery path can proceed — either `ConnectionTokenFailed` (no
refresh token available, or the refresh exchange did not return a usable access token) or
`InvalidRefreshTokenException` (the backend rejected the refresh token itself):

```kotlin
// ChatInstanceProvider.Listener or ChatStateListener:
override fun onChatRuntimeException(exception: RuntimeChatException) {
    if (exception is RuntimeChatException.ConnectionTokenFailed ||
        exception is RuntimeChatException.InvalidRefreshTokenException
    ) {
        // SDK could not recover the session in the background — re-run PKCE OAuth,
        // then call setAuthorization() and reconnect.
        reAuthenticationPending = true
        triggerReAuthentication()
    }
}

// On ChatInstanceProvider.Listener only — ChatStateListener has no state callback. configure()
// re-prepares and returns while still Preparing, so the reconnect belongs here; connect() accepts
// only Prepared, ConnectionLost or Offline. It is not suspend, unlike Chat.connect(), so call it
// directly. reAuthenticationPending is your own flag, raised above and cleared here: without it the
// prepare() your app does at startup would connect early, and left set it would reconnect out of
// every later closeSuspending() or cancel(), both of which also land on Prepared.
override fun onChatStateChanged(chatState: ChatState) {
    if (chatState === ChatState.Prepared && reAuthenticationPending) {
        reAuthenticationPending = false
        chatProvider.connect()
    }
}

// After obtaining a new code + verifier from your OAuth provider.
// ChatInstanceProvider.configure(...) is a suspend function, so run it from a coroutine. It signs
// out and re-prepares; the listener above connects once that finishes:
scope.launch {
    chatProvider.configure(context) {
        authorization = Authorization(code = newCode, verifier = newVerifier)
    }
}
// or via ChatBuilder for a fresh build — setAuthorization() is not suspend, build() and connect() are:
scope.launch {
    chatBuilder.setAuthorization(Authorization(code = newCode, verifier = newVerifier))
        .build()
        .connect()
}
```

### What stays the same

- `ChatBuilder.setAuthorization(Authorization)` — unchanged
- `Authorization(code, verifier)` — unchanged
- `ChatInstanceProvider.configure(context) { authorization = ... }` — the `authorization` scope property is unchanged
  (`configure` itself became `suspend`, see the [Coroutine API migration guide](MIGRATION_COROUTINE_API.md))
- `Configuration.isAuthorizationEnabled` — unchanged

---

## Migration: Adding the Implicit Flow

If you want to adopt the new implicit flow in addition to (or instead of) the explicit flow:

### Step 1: Implement `TokenDelegateListener`

`TokenDelegateListener` is a SAM (functional interface). Its single method is called on a background
thread and must block until a token is ready or the attempt fails.

```kotlin
val tokenDelegate = TokenDelegateListener { reason ->
    // Runs on a background thread — block until ready.
    when (reason) {
        TokenRequestReason.UNSPECIFIED -> {
            // Initial connect — return a cached token if available; otherwise fetch one.
            getCachedToken() ?: fetchFreshToken()
        }
        TokenRequestReason.TOKEN_EXPIRED,
        TokenRequestReason.TOKEN_INVALID -> {
            // Mid-session — try a silent refresh using your stored refresh token first.
            trySilentRefresh() ?: fetchFreshToken()
        }
    }
}
```

#### Threading: bridging interactive sign-in to a blocking callback

When no cached token is available and user interaction is required (e.g. a browser sign-in), you need
a mechanism to block the SDK background thread until the UI delivers the token. A `CompletableFuture`
is a common approach:

```kotlin
// ViewModel — hold a reference so the UI can complete it:
private val pendingTokenFuture = AtomicReference<CompletableFuture<OAuthToken>?>()

// Inside TokenDelegateListener when interactive sign-in is needed:
val future = CompletableFuture<OAuthToken>()
pendingTokenFuture.set(future)
triggerSignInUi()    // Post a signal to launch your OAuth browser flow
return future.get()  // Blocks the SDK background thread

// Called by your UI layer on success:
fun deliverToken(accessToken: String, expiryMillis: Long?) {
    val token = OAuthToken(accessToken, expiryMillis)
    pendingTokenFuture.getAndSet(null)?.complete(token)
}

// Called by your UI layer on failure / cancellation:
fun deliverTokenFailure(cause: Throwable) {
    val failure = TokenDelegationFailedException(cause.message ?: "Token request failed", cause)
    pendingTokenFuture.getAndSet(null)?.completeExceptionally(failure)
}

// Clean up to avoid dangling futures when the ViewModel is destroyed:
override fun onCleared() {
    super.onCleared()
    deliverTokenFailure(TokenDelegationFailedException("ViewModel cleared"))
}
```

### Step 2: Register the listener

```kotlin
// Via ChatBuilder (before connect) — the ChatBuilder(context, config) factory, build() and connect()
// are all suspend functions:
scope.launch {
    ChatBuilder(context, config)
        .setTokenDelegateListener(tokenDelegate)
        .build()
        .connect()
}

// Or via ChatInstanceProvider at runtime:
chatProvider.setTokenDelegateListener(tokenDelegate)

// Or inside configure() to apply to a running or new session — configure() is also suspend:
scope.launch {
    chatProvider.configure(context) {
        configuration = socketFactoryConfiguration
        tokenDelegateListener = tokenDelegate
    }
}
```

### Step 3: Return `OAuthToken` with expiry

Always provide `expiryDateMillis` when you know the token lifetime — it enables the SDK to schedule
proactive refresh before the backend rejects the token:

```kotlin
// Preferred — with expiry for proactive refresh:
OAuthToken(accessToken = jwt, expiryDateMillis = System.currentTimeMillis() + lifetimeMs)

// Without expiry — SDK only refreshes when backend returns 400:
OAuthToken(accessToken = jwt)
```

### Step 4: Clear `Authorization` when switching to implicit flow

The explicit and implicit flows are **mutually exclusive per session**. The SDK enables implicit
flow only when a `TokenDelegateListener` is set **and** `Authorization` is not provided.

If both are set, the SDK uses the explicit flow (`setAuthorization()`).

---

## Dependency Changes

| Dependency | Before | After | Scope |
|------------|--------|-------|-------|
| `libs/login-with-amazon-sdk.jar` | Included | **Removed** | Sample app |

No new SDK-level dependencies have been added. The `chat-sdk-core` library has no dependency on any
OAuth library — you choose what to use in your application.

---

## Sample App Changes

The sample app (`store` module) has been updated to demonstrate both flows. These changes are
informational — they show one possible implementation using AppAuth and are not prescriptive for
your integration.

| Component | Change |
|-----------|--------|
| `StoreActivity` | Replaced Amazon Login SDK with AppAuth for PKCE and browser-based flows |
| `StoreViewModel` | Added `TokenDelegateListener` registration with caching, silent refresh, and `CompletableFuture` bridge |
| `UiState` | Added states to trigger implicit and explicit sign-in flows from the UI |
| `GoogleSignInBottomSheet` | **New** — Composable UI for choosing between implicit and explicit sign-in methods |
| `ChatSettingsHandler` | Added helpers for caching access tokens, expiry, and OAuth provider state |
| `AuthConfiguration` | **New** — reads OAuth provider endpoints and client IDs from a config file |
| `store/proguard-rules.pro` | Updated ProGuard rules; removed Amazon Login SDK rules |
| `store/AndroidManifest.xml` | Updated intent filters for AppAuth redirect URI; removed Amazon Login SDK entries |

---

## Verification Checklist

After migration, verify the following end-to-end scenarios in your application:

**Explicit flow:**
- [ ] Fresh install: sign-in flow launches → user authenticates → chat connects
- [ ] Transaction token expiry with a still-valid access token: SDK reuses it in the background and session continues without prompting the user (no `refresh_token` spent)
- [ ] Transaction token expiry with an expired access token but valid `refresh_token`: SDK refreshes in background and session continues without prompting the user
- [ ] `ConnectionTokenFailed` fallback path (e.g., missing refresh token, or a successful refresh response lacks a usable access token): sign-in flow re-launches → new code + verifier obtained → chat reconnects
- [ ] `InvalidRefreshTokenException` fallback path (backend rejects the `refresh_token`, e.g. revoked): sign-in flow re-launches → new code + verifier obtained → chat reconnects
- [ ] User cancels sign-in: app handles cancellation gracefully, no crash

**Implicit flow:**
- [ ] Fresh install: sign-in flow launches → user authenticates → token delivered → chat connects
- [ ] Token proactive refresh: session stays active across token expiry without prompting the user (requires `expiryDateMillis` to be set)
- [ ] Silent refresh failure: interactive sign-in flow re-triggers correctly
- [ ] User cancels sign-in: `TokenDelegationFailedException` reported → app shows appropriate re-login UI
- [ ] ViewModel destroyed mid-flow: no dangling `CompletableFuture`; failure delivered in `onCleared()`

**Both flows:**
- [ ] `Configuration.isAuthorizationEnabled == false`: login dialog shown without OAuth flow
- [ ] Sign out: all tokens and auth state cleared correctly
- [ ] No remaining calls to `chat.events().refresh(RefreshToken())`

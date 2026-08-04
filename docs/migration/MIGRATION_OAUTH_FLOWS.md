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

> [!WARNING]
> The **Explicit OAuth flow is currently not supported by the backend in the latest SDK version**.
> Migrate to the **Implicit flow** for latest releases.
> If your integration depends on explicit flow, stay on your current SDK version until explicit flow is supported.

---

## Table of Contents

1. [Breaking Changes](#breaking-changes)
2. [New APIs](#new-apis)
3. [Removed APIs](#removed-apis)
4. [Migration: Explicit Flow (existing integrations)](#migration-explicit-flow-existing-integrations)
5. [Migration: Adding the Implicit Flow](#migration-adding-the-implicit-flow)
6. [Dependency Changes](#dependency-changes)
7. [Sample App Changes](#sample-app-changes)

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

Three new `RuntimeChatException` subclasses have been added. Your `ChatInstanceProvider.Listener` (or
`ChatStateListener`) implementation must handle them:

| New exception | When thrown |
|---------------|-------------|
| `RuntimeChatException.FeatureUnavailableException` | Backend returned an error when exchanging the authorization code for a transaction token; explicit OAuth flow is not functioning on the current backend version. **If using `ChatActivity` (chat-sdk-ui), it exits silently — your own `ChatInstanceProvider.Listener` must show recovery UI.** |
| `RuntimeChatException.InvalidAccessTokenException` | Backend rejected the JWT used in the implicit flow; SDK could not automatically recover |
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
        is RuntimeChatException.FeatureUnavailableException -> {
            // Explicit flow: backend does not support this flow on the current version.
            // Switch to implicit flow or downgrade to a previous SDK version.
            showExplicitFlowUnavailableError()
        }
        is RuntimeChatException.ConnectionTokenFailed -> {
            // Explicit flow: SDK could not refresh with refresh_token (e.g. token missing) — re-run OAuth
            triggerExplicitReAuthentication()
        }
        is RuntimeChatException.InvalidAccessTokenException -> {
            // Implicit flow: JWT rejected by backend and silent refresh also failed
            triggerImplicitReAuthentication()
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
    TOKEN_INVALID,   // Backend returned 401 for the stored JWT
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

### `RuntimeChatException.FeatureUnavailableException`

Reported in the **explicit OAuth flow** when the backend returns an error during the transaction token
exchange. This indicates that the explicit flow (authorization-code grant) is not functioning on the
current backend version.

Switch to the implicit flow via `setTokenDelegateListener(...)`, or stay on a previous SDK version
until the explicit flow is restored.

### `RuntimeChatException.InvalidAccessTokenException`

Reported when the backend rejects the JWT and the SDK's automatic recovery (calling the delegate again)
also fails. Treat as a hard re-authentication signal.

### `RuntimeChatException.TokenDelegationFailedException`

Reported when `onNewTokenRequested` throws. The original throwable that caused the failure is
available as `Throwable.cause`.

---

## Removed APIs

| Removed element | Replacement |
|-----------------|-------------|
| `ChatAuthorization` (internal) | `Authorization` sealed class (unchanged) |
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

> [!WARNING]
> Explicit flow is currently not supported by the backend in the latest SDK version.
> This section remains for reference only.
> For upgrades, migrate to implicit flow instead.

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

For explicit flow, the SDK first attempts to refresh in the background using the stored `refresh_token`
(no user interaction). Your app must re-run the full PKCE authorization-code flow only when that recovery
cannot proceed (for example, `ConnectionTokenFailed` because no refresh token is available):

```kotlin
// ChatInstanceProvider.Listener or ChatStateListener:
override fun onChatRuntimeException(exception: RuntimeChatException) {
    if (exception is RuntimeChatException.ConnectionTokenFailed) {
        // SDK could not refresh via refresh_token (e.g. missing token) — re-run PKCE OAuth,
        // then call setAuthorization() and reconnect.
        triggerReAuthentication()
    }
}

// After obtaining a new code + verifier from your OAuth provider:
chatProvider.configure(context) {
    authorization = Authorization(code = newCode, verifier = newVerifier)
}
// or via ChatBuilder for a fresh build:
chatBuilder.setAuthorization(Authorization(code = newCode, verifier = newVerifier))
chat.connect()
```

### What stays the same

- `ChatBuilder.setAuthorization(Authorization)` — unchanged
- `Authorization(code, verifier)` — unchanged
- `ChatInstanceProvider.configure { authorization = ... }` — unchanged
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
// Via ChatBuilder (before connect):
ChatBuilder(context, config)
    .setTokenDelegateListener(tokenDelegate)
    .build { result -> result.onSuccess { chat -> chat.connect() } }

// Or via ChatInstanceProvider at runtime:
chatProvider.setTokenDelegateListener(tokenDelegate)

// Or inside configure() to apply to a running or new session:
chatProvider.configure(context) {
    configuration = socketFactoryConfiguration
    tokenDelegateListener = tokenDelegate
}
```

### Step 3: Return `OAuthToken` with expiry

Always provide `expiryDateMillis` when you know the token lifetime — it enables the SDK to schedule
proactive refresh before the backend rejects the token:

```kotlin
// Preferred — with expiry for proactive refresh:
OAuthToken(accessToken = jwt, expiryDateMillis = System.currentTimeMillis() + lifetimeMs)

// Without expiry — SDK only refreshes when backend returns 401:
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
- [ ] Transaction token expiry with valid `refresh_token`: SDK refreshes in background and session continues without prompting the user
- [ ] `ConnectionTokenFailed` fallback path (e.g., missing refresh token): sign-in flow re-launches → new code + verifier obtained → chat reconnects
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

# OAuth Third-Party Authentication

This case study explains how to integrate third-party OAuth authentication with the CXone Chat SDK.
The SDK supports two distinct flows — **Explicit** and **Implicit** — and this document covers both in
detail, including token lifecycle, re-authentication, and error handling.

## Table of Contents

1. [Overview](#overview)
2. [Choosing a Flow](#choosing-a-flow)
3. [Explicit OAuth Flow](#explicit-oauth-flow)
   - [How It Works (Explicit)](#how-it-works-explicit)
   - [Setup (Explicit)](#setup-explicit)
   - [Token Expiration and Re-Authentication](#token-expiration-and-re-authentication)
   - [Error Handling (Explicit)](#error-handling-explicit)
4. [Implicit OAuth Flow](#implicit-oauth-flow)
   - [How It Works (Implicit)](#how-it-works-implicit)
   - [Setup (Implicit)](#setup-implicit)
   - [Token Lifecycle and Refresh](#token-lifecycle-and-refresh)
   - [Error Handling (Implicit)](#error-handling-implicit)
5. [Exception Reference](#exception-reference)
6. [Summary](#summary)

---

## Overview

The CXone Chat SDK supports third-party OAuth authentication, configured on the channel. In this mode, user identity is
managed by an external OAuth provider of your choice. Whether the channel requires it is reported by the public
`Chat.configuration.isAuthorizationEnabled` (`com.nice.cxonechat.state.Configuration`); the authentication type itself is
an SDK-internal detail and is not part of the public API.

Two distinct flows are supported:

| Flow | Who exchanges the auth code for tokens | SDK API |
|------|----------------------------------------|---------|
| **Explicit** | CXone backend (app passes code + PKCE verifier to SDK) | `ChatBuilder.setAuthorization()` |
| **Implicit** | Integrating application (app passes finished JWT directly to SDK) | `ChatBuilder.setTokenDelegateListener()` |

Both flows require the channel to have `isAuthorizationEnabled = true` in its configuration.

Flow selection is resolved per session as follows:
- **Implicit flow** is active when a `TokenDelegateListener` is registered **and** no `Authorization` (code + verifier) is set.
- **Explicit flow** is active when an `Authorization` is set — even if a `TokenDelegateListener` is also registered. Setting an authorization overrides the delegate for that session.

> **OAuth library choice:** The SDK has no dependency on any specific OAuth library. You may use
> AppAuth, a vendor SDK, a plain HTTP client, or any other library to perform the OAuth exchange.
> The snippets in this document show only the SDK API calls; how you obtain the credentials from
> your provider is entirely up to you.

---

## Choosing a Flow

Use the **Explicit flow** when:
- Your CXone backend is configured to exchange authorization codes directly with your OAuth provider.
- You prefer the backend to own the token exchange step (PKCE authorization-code flow).
- You supply the SDK with a one-time authorization code and the corresponding PKCE code verifier.

Use the **Implicit flow** when:
- Your application already performs the full OAuth exchange and holds the JWT access token.
- You need fine-grained control over token refresh (silent refresh, biometric re-authentication, etc.).
- You want to integrate with an OAuth provider that the CXone backend does not directly support.

---

## Explicit OAuth Flow

### How It Works (Explicit)

The integrating app performs the PKCE authorization-code step with the OAuth provider, then passes the
resulting **authorization code** and **code verifier** to the SDK. The SDK forwards them to the CXone
backend, which exchanges them for an access token and issues a transaction token used to establish the
WebSocket session.

```
┌─────────────┐      ┌───────────────┐      ┌─────────────┐      ┌──────────────┐
│  Client App │      │ OAuth Provider│      │CXone Backend│      │   Chat SDK   │
└──────┬──────┘      └──────┬────────┘      └──────┬──────┘      └──────┬───────┘
       │                    │                      │                    │
       │ 1. PKCE auth       │                      │                    │
       │    request         │                      │                    │
       ├───────────────────>│                      │                    │
       │                    │                      │                    │
       │ 2. Authorization   │                      │                    │
       │    code returned   │                      │                    │
       │<───────────────────┤                      │                    │
       │                    │                      │                    │
       │ 3. setAuthorization(code, verifier)       │                    │
       ├───────────────────────────────────────────────────────────────>│
       │                    │                      │                    │
       │ 4. chat.connect()  │                      │                    │
       ├───────────────────────────────────────────────────────────────>│
       │                    │                      │                    │
       │                    │  5. Exchange code    │                    │
       │                    │     for token        │                    │
       │                    │<─────────────────────┤                    │
       │                    │                      │                    │
       │                    │  6. Transaction      │                    │
       │                    │     token issued     │                    │
       │                    ├─────────────────────>│                    │
       │                    │                      │                    │
       │ 7. WebSocket connected                                         │
       │<───────────────────────────────────────────────────────────────┤
```

### Setup (Explicit)

#### Step 1: Obtain an authorization code and code verifier

Use your preferred OAuth library or a plain PKCE implementation to:

1. Generate a PKCE code verifier (a cryptographically random string).
2. Derive the code challenge from the verifier (SHA-256, Base64url-encoded).
3. Launch the OAuth authorization request with the code challenge.
4. Receive the authorization code from the provider's redirect.

The code verifier must be retained — you need to pass it to the SDK alongside the authorization code.

#### Step 2: Pass the credentials to the SDK

Once you have both the authorization code and the code verifier, hand them to the SDK:

```kotlin
// Build with ChatBuilder:
// ChatBuilder(...), build() and connect() are all suspend functions — call them from a coroutine.
scope.launch {
    try {
        val chat = ChatBuilder(context, socketFactoryConfiguration)
            .setDevelopmentMode(BuildConfig.DEBUG)
            .setAuthorization(Authorization(code = authorizationCode, verifier = codeVerifier))
            .setChatStateListener(chatStateListener)
            .build()
        chat.connect()
    } catch (expected: CancellationException) {
        throw expected // Never swallow cancellation — it would break structured concurrency.
    } catch (logged: Exception) {
        // Handle build/connect failure.
    }
}
```

When using `ChatInstanceProvider`:

```kotlin
// configure() is a suspend function — call it from a coroutine.
scope.launch {
    chatProvider.configure(context) {
        configuration = socketFactoryConfiguration
        authorization = Authorization(code = authorizationCode, verifier = codeVerifier)
    }
}
```

### Token Expiration and Re-Authentication

The transaction token issued by the CXone backend expires after a configured period. For explicit flow,
the SDK attempts background recovery with no user interaction, in two steps depending on whether the
underlying OAuth access token is also still valid:

1. **Access token still valid:** the SDK reuses it directly to obtain a new transaction token — the
   stored `refresh_token` is not spent.
2. **Access token also expired:** the SDK spends the stored `refresh_token` to mint a new access token,
   then uses that new access token to obtain a new transaction token.

If neither recovery path can proceed — no `refresh_token` is available, the refresh exchange does not
return a usable access token, or the backend rejects the `refresh_token` itself — your app must re-run
the PKCE authorization-code flow to obtain a new code + verifier.

**What happens when the token expires:**

1. On expiry, the SDK attempts background recovery as described above (access-token reuse, or
   refresh-token exchange followed by access-token reuse).
2. If recovery succeeds, the session continues without prompting the user.
3. If recovery cannot proceed because no usable credential is available, the SDK reports
   `RuntimeChatException.ConnectionTokenFailed`. If the backend explicitly rejects the `refresh_token`
   while attempting the exchange, the SDK reports `RuntimeChatException.InvalidRefreshTokenException` instead.
4. The integrating app re-runs PKCE OAuth, supplies new code + verifier via `setAuthorization()` (or `configure { authorization = ... }`), and reconnects.

```kotlin
// In your ViewModel or ChatInstanceProvider.Listener:
override fun onChatRuntimeException(exception: RuntimeChatException) {
    when (exception) {
        is RuntimeChatException.ConnectionTokenFailed,
        is RuntimeChatException.InvalidRefreshTokenException -> {
            // SDK could not recover the session in the background — re-run OAuth to get a new
            // code + verifier, then call setAuthorization() and reconnect.
            triggerOAuthReAuthentication()
        }
        else -> { /* handle other exceptions */ }
    }
}
```

### Error Handling (Explicit)

| Exception | Cause | Required action |
|-----------|-------|-----------------|
| `RuntimeChatException.ConnectionTokenFailed` | SDK could not recover an expired explicit-flow session in the background — either no `refresh_token` was available, or the refresh-token exchange did not return a usable access token | Re-run PKCE flow → call `setAuthorization()` with new code + verifier → reconnect |
| `RuntimeChatException.InvalidRefreshTokenException` | Backend rejected the `refresh_token` itself while the SDK tried to exchange it for a new access token — no fallback remains | Re-run PKCE flow → call `setAuthorization()` with new code + verifier → reconnect |
| `RuntimeChatException.AuthorizationError` | Backend rejected the authorization code, or returned an error while exchanging it for a transaction token (e.g. explicit flow not enabled/misconfigured for this channel) | Verify OAuth provider setup and backend explicit-flow configuration (`isAuthorizationEnabled`) |

> [!NOTE]
> **If you use `chat-sdk-ui`'s built-in `ChatActivity`/`ChatErrorScreen`:** it already shows its own
> non-dismissible dialog for `AuthorizationError` and `InvalidRefreshTokenException` (confirming ends the
> chat session), since the SDK itself cannot recover from either. Don't also trigger your own
> re-authentication UI for these two from a separate `ChatInstanceProvider.Listener` — that would show two
> competing prompts. `ConnectionTokenFailed` is different: `chat-sdk-ui` exits the chat screen silently and
> expects your app to drive re-authentication, which is exactly the pattern shown above.
> This distinction only matters if you're using `chat-sdk-ui`; headless integrations against `chat-sdk-core`
> alone should follow the table and code sample above for all three exceptions.

---

## Implicit OAuth Flow

### How It Works (Implicit)

The integrating app performs the **full OAuth exchange** with the provider and hands a finished JWT
access token directly to the SDK via `TokenDelegateListener`. The SDK uses the token's expiry timestamp
to proactively request a refresh before sending events.

```
┌─────────────┐      ┌───────────────┐      ┌──────────────┐      ┌──────────────┐
│  Client App │      │ OAuth Provider│      │   Chat SDK   │      │CXone Backend │
└──────┬──────┘      └──────┬────────┘      └──────┬───────┘      └──────┬───────┘
       │                    │                      │                     │
       │ 1. Full OAuth      │                      │                     │
       │    exchange        │                      │                     │
       ├───────────────────>│                      │                     │
       │                    │                      │                     │
       │ 2. JWT + expiry    │                      │                     │
       │<───────────────────┤                      │                     │
       │                    │                      │                     │
       │ 3. setTokenDelegateListener(listener)     │                     │
       ├──────────────────────────────────────────>│                     │
       │                    │                      │                     │
       │ 4. chat.connect()  │                      │                     │
       ├──────────────────────────────────────────>│                     │
       │                    │                      │                     │
       │         5. onNewTokenRequested(UNSPECIFIED)                     │
       │<──────────────────────────────────────────┤                     │
       │                    │                      │                     │
       │ 6. return OAuthToken(jwt, expiryMillis)   │                     │
       ├──────────────────────────────────────────>│                     │
       │                    │                      │                     │
       │                    │                      │ 7. Connect with JWT │
       │                    │                      ├────────────────────>│
       │                    │                      │                     │
       │ 8. WebSocket connected                    │                     │
       │<──────────────────────────────────────────┤                     │
       │                    │                      │                     │
       │          ... session active ...           │                     │
       │                    │                      │                     │
       │         9. onNewTokenRequested(TOKEN_EXPIRED)  (proactive)      │
       │<──────────────────────────────────────────┤                     │
       │                    │                      │                     │
       │ 10. Silent refresh │                      │                     │
       │     (no UI needed) │                      │                     │
       ├───────────────────>│                      │                     │
       │                    │                      │                     │
       │ 11. return OAuthToken(newJwt, newExpiry)  │                     │
       ├──────────────────────────────────────────>│                     │
```

### Setup (Implicit)

#### Step 1: Implement TokenDelegateListener

`TokenDelegateListener` is a SAM (functional interface). Its single method `onNewTokenRequested` is
called on a **background thread** and **must block** until a token is returned or the attempt fails.
The `reason` parameter tells you why the SDK needs a token so you can decide between returning a cached
value, performing a silent refresh, or prompting the user.

```kotlin
val tokenDelegate = TokenDelegateListener { reason ->
    // Runs on a background thread — block until ready.
    when (reason) {
        TokenRequestReason.UNSPECIFIED -> {
            // Initial connect. Return a cached token immediately if one exists.
            val cached = getCachedToken()
            if (cached != null) return@TokenDelegateListener cached

            // No cache — obtain a fresh token from your OAuth provider.
            // This may block on user interaction (browser sign-in, biometric, etc.).
            fetchFreshToken()
        }
        TokenRequestReason.TOKEN_EXPIRED,
        TokenRequestReason.TOKEN_INVALID -> {
            // Mid-session refresh. Try a silent refresh using the stored refresh token first.
            // If that fails, fall back to an interactive flow.
            trySilentRefresh() ?: fetchFreshToken()
        }
    }
}
```

If the token cannot be obtained, throw from the callback. Any throwable is accepted — the SDK wraps
non-`TokenDelegationFailedException` causes and reports them via `onChatRuntimeException`.

#### Step 2: Register the listener

```kotlin
// Via ChatBuilder:
// ChatBuilder(...), build() and connect() are all suspend functions — call them from a coroutine.
scope.launch {
    try {
        val chat = ChatBuilder(context, socketFactoryConfiguration)
            .setDevelopmentMode(BuildConfig.DEBUG)
            .setTokenDelegateListener(tokenDelegate)
            .setChatStateListener(chatStateListener)
            .build()
        chat.connect()
    } catch (expected: CancellationException) {
        throw expected // Never swallow cancellation — it would break structured concurrency.
    } catch (logged: Exception) {
        // Handle build/connect failure.
    }
}

// Via ChatInstanceProvider at runtime:
chatProvider.setTokenDelegateListener(tokenDelegate)

// Or inside configure() to apply immediately — configure() is a suspend function:
scope.launch {
    chatProvider.configure(context) {
        configuration = socketFactoryConfiguration
        tokenDelegateListener = tokenDelegate
        authorization = null // Required when switching from explicit to implicit.
    }
}
```

#### Step 3: Bridge the background thread to your UI

`onNewTokenRequested` blocks the SDK background thread. When a fresh token requires user interaction
(browser sign-in, etc.), you need a mechanism to block the background thread until the UI delivers the
token. `CompletableFuture` is a straightforward choice:

```kotlin
// In your ViewModel:
private val pendingTokenFuture = AtomicReference<CompletableFuture<OAuthToken>?>()

// Inside the TokenDelegateListener — when no cached/silent token is available:
val future = CompletableFuture<OAuthToken>()
pendingTokenFuture.set(future)
triggerSignInUi()       // Post a signal to your UI layer to launch the OAuth flow
future.get()            // Blocks the SDK background thread until the UI delivers a token

// Called by your UI layer after the OAuth provider returns a token:
fun deliverToken(token: String, expiryMillis: Long?) {
    val oauthToken = OAuthToken(token, expiryMillis)
    val future = pendingTokenFuture.getAndSet(null)
    if (future != null) {
        future.complete(oauthToken)     // Unblocks the SDK background thread
    } else {
        cacheToken(token, expiryMillis) // No pending connect — cache for next call
    }
}

// Called by your UI layer if the OAuth flow fails or is cancelled:
fun deliverTokenFailure(cause: Throwable) {
    val exception = cause as? TokenDelegationFailedException
        ?: TokenDelegationFailedException(cause.message ?: "Token request failed", cause)
    pendingTokenFuture.getAndSet(null)?.completeExceptionally(exception)
}

// Clean up when your ViewModel is destroyed to avoid dangling futures:
override fun onCleared() {
    super.onCleared()
    deliverTokenFailure(TokenDelegationFailedException("ViewModel cleared"))
}
```

### Token Lifecycle and Refresh

The SDK triggers `onNewTokenRequested` based on three reasons:

| `TokenRequestReason` | When triggered | Recommended action |
|----------------------|----------------|--------------------|
| `UNSPECIFIED` | First `connect()` call | Return cached token if available; otherwise fetch a fresh one |
| `TOKEN_EXPIRED` | Proactive refresh before `expiryDateMillis` is reached | Attempt a silent refresh using your stored refresh token; fall back to interactive if needed |
| `TOKEN_INVALID` | Backend returned HTTP 400 for the current JWT | Attempt a silent refresh; fall back to interactive if needed |

#### OAuthToken

`OAuthToken` is the value returned from `onNewTokenRequested`. It wraps the JWT and its expiry:

```kotlin
// With known expiry (recommended — enables proactive refresh):
val token = OAuthToken(
    accessToken = "eyJhbGci...",
    expiryDateMillis = System.currentTimeMillis() + 3_600_000L   // e.g. 1 hour
)

// Without expiry (SDK only refreshes on backend rejection):
val token = OAuthToken(accessToken = "eyJhbGci...")
```

When `expiryDateMillis` is `null` the SDK treats the token as non-expiring by time and only calls
`onNewTokenRequested` with `TOKEN_INVALID` when the backend rejects it. Providing the expiry is
strongly recommended to enable proactive refresh and avoid session interruptions.

#### Silent Refresh

For `TOKEN_EXPIRED` and `TOKEN_INVALID`, a silent refresh should be attempted before falling back to
an interactive browser flow. The implementation depends on your OAuth library:

- If your library stores a refresh token (e.g., AppAuth's `AuthState`), use its refresh-token exchange
  to obtain a new access token without user interaction.
- If your library does not manage refresh tokens, implement a background HTTP call to your provider's
  token endpoint using the stored refresh token.
- If no refresh token is available, fall through to the interactive browser flow.

A silent refresh that fails should throw `TokenDelegationFailedException` so the SDK can catch it and
either fall back (if you implement fallback logic before throwing) or report the failure.

### Error Handling (Implicit)

**Throwing from `onNewTokenRequested`:**

```kotlin
val tokenDelegate = TokenDelegateListener { reason ->
    val token = oauthProvider.getToken()
        ?: throw TokenDelegationFailedException("User cancelled sign-in")
    OAuthToken(token.jwt, token.expiryMillis)
}
```

**Handling exceptions in your `ChatInstanceProvider.Listener`:**

```kotlin
override fun onChatRuntimeException(exception: RuntimeChatException) {
    when (exception) {
        is RuntimeChatException.TokenDelegationFailedException -> {
            // Delegate could not provide a token — show re-login UI
        }
        is RuntimeChatException.InvalidAccessTokenException -> {
            // Backend rejected JWT and automatic recovery also failed — show re-login UI
        }
        else -> { /* handle other exceptions */ }
    }
}
```

| Exception | Cause | Required action |
|-----------|-------|-----------------|
| `RuntimeChatException.TokenDelegationFailedException` | `onNewTokenRequested` threw an exception | Show error / re-login UI |
| `RuntimeChatException.InvalidAccessTokenException` | Backend rejected JWT; delegate recovery also failed | Show re-login UI |

---

## Exception Reference

| Exception class | Flow | Description |
|-----------------|------|-------------|
| `RuntimeChatException.ConnectionTokenFailed` | Explicit | SDK could not recover an expired session in the background (no `refresh_token` available, or the refresh exchange did not return a usable access token); re-authentication with a new code + verifier required |
| `RuntimeChatException.AuthorizationError` | Explicit | Backend rejected the authorization code, or returned an error while exchanging it for a transaction token (e.g. explicit flow not enabled/misconfigured for this channel) |
| `RuntimeChatException.InvalidRefreshTokenException` | Explicit | Backend rejected the `refresh_token` while the SDK tried to exchange it for a new access token (e.g. expired/revoked); full re-authentication with a new code + verifier is required — no fallback remains |
| `RuntimeChatException.InvalidAccessTokenException` | Implicit | Backend rejected JWT; automatic recovery also failed |
| `RuntimeChatException.TokenDelegationFailedException` | Implicit | `onNewTokenRequested` threw; SDK cannot proceed |

All exceptions are reported via `ChatStateListener.onChatRuntimeException()` on a background thread.

---

## Summary

### Responsibilities

| Component | Explicit Flow | Implicit Flow |
|-----------|--------------|---------------|
| **Integrating App** | Run PKCE OAuth flow; pass code + verifier to SDK; handle `ConnectionTokenFailed`/`InvalidRefreshTokenException` as fallback re-auth path | Perform full OAuth exchange; implement `TokenDelegateListener` with caching and refresh logic |
| **Chat SDK** | Forward code + verifier to backend; establish WebSocket with transaction token | Call `onNewTokenRequested` at the right time; schedule proactive refresh using `expiryDateMillis` |
| **CXone Backend** | Exchange auth code for tokens; issue transaction token | Validate JWT; return 400 on rejection |

### Quick Setup Checklist

**Explicit flow:**
- [ ] Implement PKCE authorization-code flow with your OAuth provider
- [ ] Call `ChatBuilder.setAuthorization(Authorization(code, verifier))` after obtaining credentials
- [ ] Verify expiry path: session recovers in the background when the access token is still valid (no `refresh_token` spent) or when a valid `refresh_token` is available
- [ ] Handle `RuntimeChatException.ConnectionTokenFailed` fallback by re-running OAuth when refresh cannot proceed
- [ ] Handle `RuntimeChatException.InvalidRefreshTokenException` by re-running OAuth when the backend rejects the `refresh_token`

**Implicit flow:**
- [ ] Implement `TokenDelegateListener` with caching, silent refresh, and interactive fallback
- [ ] Register via `ChatBuilder.setTokenDelegateListener()` or `ChatInstanceProvider.setTokenDelegateListener()`
- [ ] Use `OAuthToken(accessToken, expiryDateMillis)` to enable proactive refresh
- [ ] Handle `RuntimeChatException.TokenDelegationFailedException` and `InvalidAccessTokenException`
- [ ] Clean up any pending futures in your ViewModel's `onCleared()`

### Further Reading

- [Migration Guide: OAuth Flows](../migration/MIGRATION_OAUTH_FLOWS.md)
- [Sample implementation](../../store/src/main/kotlin/com/nice/cxonechat/sample/)

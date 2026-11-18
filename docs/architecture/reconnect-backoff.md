# Reconnect Backoff Behavior

## TL;DR

The SDK never retries a dropped or failed connection immediately. Every automatic reconnect —
whether the WebSocket drops mid-session or the initial connect attempt fails before a socket is even
opened — waits `1s + random(1-5s)` before the first retry, then multiplies the delay by `1.3` on each
subsequent attempt, capped at `500s`, giving up after `20` attempts. This is a **backend requirement**,
not a tuning choice: retrying too fast gets the client rate-limited by the server. There are two
separate mechanisms that both use this same formula — pick the right one below when tracing reconnect
behavior in the code.

## Why backoff, and why shared

If a client retries a failed connection too frequently within a short window, the backend blocks that
client until a block timeout expires. A fast or naive retry loop makes outages *worse*, not better —
the client spends its retry budget getting blocked instead of reconnecting once the outage clears. Every
reconnect mechanism in the SDK must therefore compute its delay from the same formula, defined once in
[`ReconnectBackoff`][reconnect-backoff] (`chat-sdk-core/src/main/kotlin/com/nice/cxonechat/internal/ReconnectBackoff.kt`):

| Constant | Value | Meaning |
|---|---|---|
| `INITIAL_DELAY` | `1000ms` | Base delay before the first retry |
| `MIN_RANDOM_DELAY` / `MAX_RANDOM_DELAY` | `1000ms` / `5000ms` | Jitter added to the first retry only, to avoid every client retrying in lockstep |
| Growth factor | `×1.3` | Applied to the previous delay for every attempt after the first |
| `MAX_BACKOFF` | `500_000ms` (~8.3 min) | Ceiling — the delay never grows past this |
| `MAX_RECONNECT_ATTEMPTS` | `20` | The mechanism gives up after this many attempts and reports an unexpected disconnect |

## The two reconnect mechanisms

Both share the formula above, but they exist because they cover **disjoint failure phases** of a
connection attempt — one never sees the failures the other handles.

### 1. Socket-level: `ReconnectingListener`

`chat-sdk-core/src/main/kotlin/com/nice/cxonechat/internal/ReconnectingListener.kt` is an
`okhttp3.WebSocketListener` that reacts to `onFailure`/`onClosing` callbacks — i.e. it only engages
**after a WebSocket has successfully opened at least once**. It covers:

- The socket dropping mid-session (network blip while chatting).
- A reconnect attempt's socket failing to open again (`TransientConnectFailureException`).

It does **not** engage for the very first connect attempt if that attempt never gets far enough to open
a socket at all — see the second mechanism below.

### 2. Pre-socket REST phase: `PreSocketReconnectScheduler` (Live Chat only)

Before a Live Chat session opens a WebSocket, `ChatLiveChat.connect()`
(`chat-sdk-core/src/main/kotlin/com/nice/cxonechat/internal/ChatLiveChat.kt`) first calls
`getChannelAvailability()` — a plain REST call. If the device is offline at that moment, this throws
before any socket is ever opened, so `ReconnectingListener` above never sees the failure at all. Left
unhandled, the SDK would stay stuck in `ConnectionLost` until the app was restarted or the UI happened to
retry at exactly the right moment.

`PreSocketReconnectScheduler` (`chat-sdk-core/src/main/kotlin/com/nice/cxonechat/internal/PreSocketReconnectScheduler.kt`)
closes this gap: `ChatInstanceProvider.attemptConnect()` catches the wrapped failure
(`ChannelAvailabilityFailedException`) and hands it to the scheduler, which retries the connection
automatically using the same `ReconnectBackoff` formula, without waiting on the UI to call `connect()`
again. It resets its attempt count on any successful (re)connect, and on explicit `cancel()`/`close()`/
`signOut()`, so a stale scheduled retry never fires into a session the caller has already moved on from.

## Related docs

- [CS: Live Chat][cs-live-chat] — the integrator-facing case study; see its note on reconnection handling.

[reconnect-backoff]: ../../chat-sdk-core/src/main/kotlin/com/nice/cxonechat/internal/ReconnectBackoff.kt
[cs-live-chat]: ../chat-sdk-core/cs-live-chat.md

# Migration Guide: Coroutine-First API (4.0)

## TL;DR

Version 4.0 makes the CXone Chat SDK **coroutine-first**. The per-handler callback/listener APIs for
connecting, observing threads, sending messages, and triggering events have been **removed** in this
breaking major release (see the mapping table below). One listener survives: `ChatStateListener`
remains in the public API but `ChatBuilder.setChatStateListener(...)` is deprecated in favour of
`Chat.stateFlow`. The provider-level `ChatInstanceProvider.Listener` is unchanged.

- **Kotlin consumers** call `suspend` functions from a coroutine and collect `Flow` properties
  (`stateFlow`, `threadsFlow`, `threadFlow`, `popupFlow`) instead of registering listeners.
- **Java consumers** add the new `com.nice.cxone:chat-sdk-core-java` dependency and use its
  `*JavaInterop` helpers and `*CoroutineWrapper` classes, which expose `Cancellable`-returning
  callback methods that bridge to the suspend/Flow surface.

If you use the `chat-sdk-ui` module, this migration is handled for you — no consumer changes needed.

---

## Removed / changed API mapping

| Removed / changed (3.x) | Replacement (4.0) — Kotlin | Replacement (4.0) — Java (`chat-sdk-core-java`) |
|---|---|---|
| `ChatBuilder.build { chat -> }` | `suspend fun build(): Chat` | `ChatBuilderJavaInterop.buildAsync(builder, onResult, onError, executor)` |
| `ChatBuilder.getDefault(...)` *(blocking, in `chat-sdk-core`)* | `suspend fun ChatBuilder.getDefault(...)` | `ChatBuilderJavaInterop.getDefaultBlocking(...)` / `getDefaultAsync(...)` |
| `Chat.connect()` *(fire-and-forget + listener)* | `suspend fun connect()` + collect `stateFlow` | `ChatJavaInterop.connectAsync(chat, onConnected, onError, executor)` |
| `ChatStateListener` *(for a `Chat`)* | `Chat.stateFlow: SharedFlow<ChatStateEvent>` | `ChatJavaInterop.observeState(chat, onEvent, onError, executor)` |
| `ChatThreadsHandler.threads(OnThreadsUpdatedListener)` | `ChatThreadsHandler.threadsFlow: Flow<List<ChatThread>>` | `ChatThreadsHandlerJavaInterop.observeThreads(handler, onThreads, onError, executor)` |
| `ChatThreadHandler.get(OnThreadUpdatedListener)` / `OnThreadUpdatedListener` | `ChatThreadHandler.threadFlow: Flow<ChatThread>` | `ChatThreadHandlerJavaInterop.observeThread(handler, onThread, onError, executor)` |
| `ChatThreadMessageHandler.send(msg, OnMessageTransferListener)` | `suspend fun send(message): String` *(returns message id)* | `ChatThreadMessageHandlerCoroutineWrapper.sendAsync(message, onResult, onError)` |
| `ChatEventHandler.trigger(event, listener)` *(+ action extensions)* | `suspend fun trigger(event)` *(+ suspend extensions)* | `ChatEventHandlerCoroutineWrapper.triggerAsync(event, onDone, onError)` |
| `ChatThreadEventHandler.trigger(event, listener)` | `suspend fun trigger(event): EventResponse?` | `ChatThreadEventHandlerCoroutineWrapper.triggerAsync(event, onDone, onError)` |
| `ChatActionHandler.onPopup(listener)` / `ChatThreadActionHandler.onPopup(listener)` | `popupFlow: Flow<…>` | `ChatActionHandlerJavaInterop.onPopup(...)` / `ChatThreadActionHandlerJavaInterop.onPopup(...)` |
| `Chat.signOut()` *(fire-and-forget persistence)* | `suspend fun signOut()` | *(no wrapper yet — call from a coroutine)* |
| — *(new)* | `suspend fun closeSuspending()` — coroutine-native equivalent of `close()`; suspends until storage/cookie persistence and socket teardown complete instead of blocking the calling thread | — |

> Note: `ChatInstanceProvider.Listener` (the 8-state `ChatState` provider callback) is **unchanged**
> and remains the recommended way to observe chat lifecycle across process transitions. Only the
> per-`Chat` `ChatStateListener` callbacks were superseded by `Chat.stateFlow`. See
> [Instance Holder](../chat-sdk-core/cs-instance-holder.md).
>
> **`ChatInstanceProvider` itself did change**: `signOut()` and `configure(...)` are now `suspend`
> functions (matching `Chat.signOut()` above), and `ChatInstanceProvider.closeSuspending()` was added
> alongside the existing blocking `close()`. Call them from a coroutine; `close()` remains available
> for non-coroutine callers. Java callers can use `ChatInstanceProviderJavaInterop`'s
> `closeSuspendingAsync(...)`, `signOutAsync(...)`/`signOutBlocking(...)`, and
> `configureAsync(...)`/`configureBlocking(...)`.
>
> **Reliability fix:** In 4.0 `ChatInstanceProvider.connect()` reliably transitions to
> `ConnectionLost` when the connection fails immediately (e.g. expired token checked before any
> network I/O). In 3.x this was guaranteed by a synchronous lock; the coroutine-based
> implementation had a race that could leave the provider permanently stuck in `Connecting` instead.
> The `ConnectionLost`-based retry pattern shown in the Instance Holder case study works correctly
> in 4.0.

---

## Kotlin: before / after

### Build + connect + observe state

```kotlin
// 3.x — callback build + ChatStateListener
ChatBuilder(context, config)
    .setChatStateListener(object : ChatStateListener {
        override fun onReady() { /* ready */ }
        override fun onConnected() {}
        override fun onUnexpectedDisconnect() {}
        override fun onChatRuntimeException(exception: RuntimeChatException) {}
    })
    .build { chat -> /* save chat */ }
chat.connect()
```

```kotlin
// 4.0 — ChatBuilder(...) is a suspend factory and build()/connect() are suspend, so run from a coroutine
scope.launch {
    val chat = ChatBuilder(context, config).build()   // both ChatBuilder(...) and build() suspend

    launch {
        chat.stateFlow.collect { event ->
            when (event) {
                ChatStateEvent.Connecting -> Unit
                ChatStateEvent.Connected -> Unit
                ChatStateEvent.Ready -> { /* ready to use */ }
                ChatStateEvent.UnexpectedDisconnect -> { /* offer reconnect */ }
                is ChatStateEvent.RuntimeException -> handle(event.exception)
            }
        }
    }
    chat.connect()                                     // suspend
}
```

### Observe threads / a thread

```kotlin
// 3.x
val cancellable = chat.threads().threads { threads -> render(threads) }
```

```kotlin
// 4.0 — collect threadsFlow (the SDK triggers refresh() automatically on first collection)
val handler = chat.threads()
handler.threadsFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
```

For the full ViewModel examples (single-thread, multi-thread, live-chat) see
[Coroutines](../chat-sdk-core/cs-coroutines.md),
[Single Thread](../chat-sdk-core/cs-single-thread.md),
[Multi Thread](../chat-sdk-core/cs-multi-thread.md).

### Send a message

```kotlin
// 3.x
messageHandler.send(OutboundMessage(text), OnMessageTransferListener(onProcessed, onSent))
```

```kotlin
// 4.0 — suspend send() returns the message id
viewModelScope.launch {
    val messageId: String = messageHandler.send(OutboundMessage(text))
}
```

> Send/transfer outcomes are reported through `Chat.stateFlow`; failures surface as
> `ChatStateEvent.RuntimeException`.

---

## Java consumers

Add the interop artifact:

```groovy
implementation("com.nice.cxone:chat-sdk-core-java:${chatSdkVersion}")
```

Each helper returns a `Cancellable` (cancel it when your screen is destroyed) and accepts an
optional `java.util.concurrent.Executor` to dispatch callbacks (e.g.
`ContextCompat.getMainExecutor(context)` for the UI thread). `*CoroutineWrapper` instances are
`AutoCloseable` — call `close()` when done (do **not** use try-with-resources, as the wrapper
outlives a single statement).

| Task | Java helper |
|---|---|
| Create the default builder (blocking, worker thread) | `ChatBuilderJavaInterop.getDefaultBlocking(context, config, logger)` → returns a `ChatBuilder` |
| Create the default builder (async) | `ChatBuilderJavaInterop.getDefaultAsync(...)` → delivers a `ChatBuilder` |
| Build a `Chat` from a builder (async) | `ChatBuilderJavaInterop.buildAsync(builder, onResult, onError, executor)` |
| Connect | `ChatJavaInterop.connectAsync(chat, onConnected, onError, executor)` |
| Observe state | `ChatJavaInterop.observeState(chat, onEvent, onError, executor)` |
| Channel availability | `ChatJavaInterop.getChannelAvailabilityAsync(chat, onResult, onError, executor)` |
| Observe thread list | `ChatThreadsHandlerJavaInterop.observeThreads(handler, onThreads, onError, executor)` |
| Observe single thread | `ChatThreadHandlerJavaInterop.observeThread(handler, onThread, onError, executor)` |
| Archive a thread | `ChatThreadHandlerJavaInterop.archiveAsync(handler, onResult, onError, executor)` |
| Send a message | `new ChatThreadMessageHandlerCoroutineWrapper(handler, executor).sendAsync(message, onResult, onError)` |
| Trigger an event | `new ChatEventHandlerCoroutineWrapper(handler, executor).triggerAsync(event, onDone, onError)` |
| Thread events (typing, read, transcript) | `new ChatThreadEventHandlerCoroutineWrapper(handler, executor)` |
| Observe popups | `ChatActionHandlerJavaInterop.onPopup(...)` / `ChatThreadActionHandlerJavaInterop.onPopup(...)` |

---

## You no longer need callbackFlow bridges

In 3.x it was common to wrap the listener API in `callbackFlow` to get a `Flow`:

```kotlin
// 3.x — no longer needed
val threads = callbackFlow {
    val cancellable = chat.threads().threads(::trySend)
    awaitClose { cancellable.cancel() }
}
```

In 4.0 the SDK exposes these flows natively — collect `threadsFlow` / `threadFlow` directly and
delete the bridge.
